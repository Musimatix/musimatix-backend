package verse.rates.processor

import java.io.{InputStream, File}
import java.sql.Timestamp
import java.util
import java.util.Calendar
import com.typesafe.config.{ConfigFactory, Config}
import net.sf.javaml.core.kdtree.KDTree
import org.apache.commons.io.IOUtils
import org.apache.log4j.{Level, Logger}
import treeton.core.config.BasicConfiguration
import treeton.core.config.context.resources.LoggerLogListener
import treeton.core.config.context.{ContextConfigurationSyntaxImpl, ContextConfiguration}
import treeton.core.util.LoggerProgressListener
import treeton.prosody.musimatix.{StressDescription, SyllableInfo, VerseProcessingExample, VerseProcessor}
import verse.rates.env.ConfigHelper
import ConfigHelper._
import verse.rates.calculator.SampleRatesCalculator
import verse.rates.model.{MxUser, MxTag, MxSong}
import verse.rates.model.VerseMetrics._
import scala.annotation.tailrec
import scala.io.Source
import scala.util.{Failure, Success, Try}
import verse.rates.util.StringUtil._
import collection.JavaConverters._
import VectorsProcessor._


object VectorsProcessorImpl {

  val vpLogger = org.apache.log4j.Logger.getLogger("VerseProcessor")

  def createVerseProcessor(confTreeton: Config): Option[VerseProcessor] = {
    val treetonDataPath = Try { confTreeton.getString("treeton.data.path") }.toOption
    val stressRestrictionViolationWeight = confTreeton.getDouble("stress.restriction.violation.weight")
    val reaccentuationRestrictionViolationWeight = confTreeton.getDouble("reaccentuation.restriction.violation.weight")
    val spacePerMeter = confTreeton.getInt("space.per.meter")
    val maxStressRestrictionViolations = confTreeton.getInt("max.stress.restriction.violations")
    val maxReaccentuationRestrictionViolations = confTreeton.getInt("max.reaccentuation.restriction.violations")
    val maxSyllablesPerVerse = confTreeton.getInt("max.syllables.per.verse")
    val metricGrammarPath = confTreeton.getString("metric.grammar.path")

    treetonDataPath.foreach { p =>
      BasicConfiguration.setRootURL(new File(p).toURI.toURL)
    }
    BasicConfiguration.createInstance()
    ContextConfiguration.registerConfigurationClass(classOf[ContextConfigurationSyntaxImpl])
    ContextConfiguration.createInstance()

    val processor = new VerseProcessor(metricGrammarPath, stressRestrictionViolationWeight,
      reaccentuationRestrictionViolationWeight, spacePerMeter, maxStressRestrictionViolations,
      maxReaccentuationRestrictionViolations, maxSyllablesPerVerse, false)
    processor.setProgressListener(new LoggerProgressListener("Musimatix", vpLogger))
    processor.addLogListener(new LoggerLogListener(vpLogger))
    processor.initialize()

    Some(processor)
  }
}

class VectorsProcessorImpl(confRoot: Config) extends VectorsProcessor {
  import VectorsProcessorImpl._

  private[this] val logger = Logger.getLogger(classOf[VectorsProcessorImpl])

  private[this] var verseProcessor: Option[VerseProcessor] = None
  private[this] var connectionProvider: Option[ConnectionProvider] = None
  private[this] var vectorsTree: Option[KDTree] = None // new KDTree(90)
  private[this] var titleSuggestor: Option[TitleSuggestor] = None
  private[this] var songsBox: Option[SongsBox] = None
  private[this] var usersBox: Option[UsersBox] = None
  private[this] var youtubeSearch = new YoutubeSearch

  private[this] var similarityBound = 100.0
  private[this] val similarBucket = 5000

  private[this] var mixForSongFile: Option[File] = None
  private[this] var mixForTextFolder: Option[File] = None
  private[this] var mixForTextDistance: Double = 0.1

  private[this] var mixForSong = Map.empty[Int, Seq[Int]]
    .withDefaultValue(Seq.empty)
  private[this] var mixForText: Option[KDTree] = None //vector -> Seq(Int)

  locally {
    init()
  }

  case class MixText(ids: Seq[Int], vec: VerseVec)

  val ReMix = "([^|]+)\\|(.*)".r

  def loadMixels(): Unit = {
    for {
      vp <- verseProcessor
      cp <- connectionProvider
      st <- cp.select(
        """SELECT s.id, s.title_rus, s.title_eng, a.id, a.name_rus, a.name_eng
          |FROM songs s
          |LEFT OUTER JOIN song_author sa ON sa.song_id = s.id
          |LEFT OUTER JOIN authors a ON sa.author_id = a.id
          |WHERE s.vector IS NOT NULL
        """.stripMargin)
    } yield {
      val rs = st.executeQuery()

      var titleAuthor2Id = Map.empty[String, Int]

      @tailrec
      def nextSong(): Unit = {
        if (rs.next()) {
          val song_id = rs.getInt(1)
          val key = for {
            title <- Option(rs.getString(2)).orElse(Option(rs.getString(3)))
            author <- Option(rs.getString(5)).orElse(Option(rs.getString(6)))
          } yield s"${title.trim.toLowerCase}|${author.trim.toLowerCase}"
          key.foreach { k => titleAuthor2Id += k -> song_id }
          nextSong()
        }
      }
      nextSong()

      rs.close()
      st.close()

      println(s"Title|Author entries: ${titleAuthor2Id.size}")

      mixForSongFile.foreach { f =>
        Try {
          val bs = Source.fromFile(f)
          bs.getLines().foreach  { row =>
            row.toLowerCase.split("%%").toList match {
              case head :: tail =>
                val idOpt = idByTitleAuthor(head, titleAuthor2Id)
                  idOpt.foreach { id =>
                  val refs = tail.flatMap(s => idByTitleAuthor(s, titleAuthor2Id))
                  if (refs.nonEmpty) {
                    mixForSong += id -> refs
                  }
                }
              case _ =>
            }
          }
          bs.close()
        }
      }

      var textsCount = 0
      mixForTextFolder.foreach { folder =>
        val mixTree = new KDTree(vp.getMetricVectorDimension)
        mixForText = Some(mixTree)
        folder.listFiles.toVector.filter(_.isFile).foreach { file =>
          val rows = Source.fromFile(file).getLines().toVector
          if (rows.size > 1) {
            val head = rows.head
            val ids = head.toLowerCase.split("%%").toVector.flatMap { item =>
              idByTitleAuthor(item.trim, titleAuthor2Id)
            }
            val songText = rows.tail
            val stressDescriptions = new util.ArrayList[StressDescription]
            val plainLyrics = new util.ArrayList[String]
            val formattedRows = new util.ArrayList[String]
            songText.map(_.trim).filter(_.nonEmpty).foreach { s =>
              formattedRows.add(s)
            }
            vp.parseFormattedVerses(formattedRows, plainLyrics, stressDescriptions)
            val verseDescriptions = vp.process(plainLyrics, stressDescriptions, false)

            val vectors = verseDescriptions.asScala.toVector
              .map { vd =>
                vd.metricVector.asScala.toVector
                  .map {
                    case d: java.lang.Double => Double.unbox(d)
                    case _ => 0.0
                  }
              }

            def sumVec(vec1: VerseVec, vec2: VerseVec): VerseVec = {
              vec1.zip(vec2).map { case (d1, d2) => d1 + d2 }
            }

            val baseVec = vectors
              .reduceOption(sumVec)
              .map { vec => vec.map(_ / vectors.size) }
            baseVec.foreach { v =>
              mixTree.insert(v.toArray, MixText(ids, v))
              textsCount += 1
            }

          }
        }
      }
      println(s"Mix roots. Songs: ${mixForSong.size}   Texts: $textsCount")
    }
  }

  def idByTitleAuthor(at: String, mp: Map[String, Int]): Option[Int] = {
    Try {
      val sp = at.split("\\|")
      val t = sp(0)
      val a = sp(1)
      val key = s"${t.trim.toLowerCase}|${a.trim.toLowerCase}"
      val v = mp.get(key)
      v
    } match {
      case Success(s) => s
      case Failure(f) =>
        println(f.toString)
        None
    }
  }

  def init(): Unit = {
    (for {
      confRoot <- Try { ConfigFactory.load().getConfig(confRootKey) }
      confMsmx <- Try { confRoot.getConfig(confMsmxKey) }
      confTreeton <- Try { confRoot.getConfig(confTreetonKey) }
      confSongs <- Try { confRoot.getConfig(confSongsKey) }
    } yield (confMsmx, confTreeton, confSongs)) match {
      case Success((confMsmx, confTreeton, confSongs)) =>
        verseProcessor = createVerseProcessor(confTreeton)
        connectionProvider = Some(new ConnectionProvider(confMsmx))
        buildVectorsTree()
        connectionProvider.foreach { cp =>
          titleSuggestor = Some(new TitleSuggestor(cp))
          songsBox = Some(new SongsBox(cp))
          usersBox = Some(new UsersBox(cp))
        }
        Try { similarityBound = confSongs.getDouble("similarity.bound") }
        mixForSongFile = Try { new File(confSongs.getString("mix.for.song.file")) }.toOption
        mixForTextFolder = Try { new File(confSongs.getString("mix.for.text.folder")) }.toOption
        Try { mixForTextDistance = confSongs.getDouble("mix.for.text.distance") }

        loadMixels()

      case Failure(f) =>
        println(failureMessage(f))
    }
  }

  def buildVectorsTree(): Unit = {
    for (
      vp <- verseProcessor;
      cp <- connectionProvider;
      st <- cp.select("SELECT id, vector FROM songs WHERE vector IS NOT NULL")
    ) {
        val vt = new KDTree(vp.getMetricVectorDimension)
        print("Building vectors...")

        val rs = st.executeQuery()
        var count = 0
        Try {
          @tailrec
          def nextVec(): Unit = {
            if (rs.next()) {
              val id = rs.getInt(1)
              val vec = Option(rs.getBlob(2))
                .map( blob => IOUtils.toByteArray(blob.getBinaryStream) )
                .map(deserializeVerseVec)
              vec.foreach { v =>
                vt.insert(v.toArray, Int.box(id))
                count += 1
              }
              nextVec()
            }
          }
          nextVec()
        }
        rs.close()
        st.close()

        println(s" [$count] -- done")
        vectorsTree = Some(vt)
    }
  }

  def bye(): Unit = {
    verseProcessor.foreach(_.deinitialize())
    connectionProvider.foreach(_.bye())
    vectorsTree = None
  }

  val calc = new SampleRatesCalculator

  override def invokeCalculator(verse: String): Either[ErrorCode, VerseRates] = {

    val rates = calc.calculate(verse)

    val ratesAsScala = rates
      .map(_.map(_.asInstanceOf[Double]).toVector)
      .toVector

    val rowsRates = ratesAsScala.take(rates.length - 1)
    val totalRates = ratesAsScala.last

    Right(
      VerseRates(
        rowsRates,
        totalRates
      )
    )
  }

  override def findSimilarSimple(id: Int, limit: Int): Seq[FullSong] = {
    val builder = Vector.newBuilder[FullSong]
    for (
      cp <- connectionProvider;
      vt <- vectorsTree;
      st <- cp.select("SELECT id, vector FROM songs WHERE id = ?")
    ) {
      st.setInt(1, id)
      val rs = st.executeQuery()
      if (rs.next()) {
        val vec = Option(rs.getBlob(2))
          .map( blob => IOUtils.toByteArray(blob.getBinaryStream) )
          .map { bb =>
            deserializeVerseVec(bb)
          }
        vec.foreach { v =>
          val ids = vt.nearest(v.toArray, 20)
          println(s"found: ${ids.length}")
          for (idObj <- ids) {
            val id = Int.unbox(idObj.asInstanceOf[Integer])
            builder += FullSong(id, "", Vector.empty[Author], Vector.empty[String],
              Vector.empty[Syllable], Vector.empty[SongTag])
          }
        }
      }
      rs.close()
      st.close()
    }
    builder.result()
  }

  override def findSimilar(id: Int, limit: Int, tags: Seq[Int]): Seq[MxSong] = {
    val songs = for {
      cp <- connectionProvider
      vt <- vectorsTree
      sb <- songsBox
      st <- cp.select("SELECT vector FROM songs WHERE id = ?")
    } yield {

      var mixIds = mixForSong(id).filterNot(id.==)

      var mixSongs = sb.getSongsByIds(mixIds)
          .map(song => song.copy(similarity = Some(1.0)))
      val restToFind = math.max(0, limit - mixSongs.size)

      var excludeIds = mixIds.toSet + id

      st.setInt(1, id)
      val rs = st.executeQuery()
      val baseVecOpt =
        if (rs.next()) {
          Option(rs.getBlob(1))
            .map(blob => deserializeVerseVec(IOUtils.toByteArray(blob.getBinaryStream)))
        } else None
      val baseVec = baseVecOpt.getOrElse(Vector.empty[Double])
      rs.close()
      st.close()

      val restIds = if (restToFind > 0 && baseVec.nonEmpty) {
        if (tags.nonEmpty) {
          val ids = vt.nearest(baseVec.toArray, similarBucket)
            .toVector
            .map(Int.unbox)
          sb.filterByTags(ids, tags)
            .view
            .filterNot(excludeIds.contains)
            .take(restToFind)
            .force
        } else
          vt.nearest(baseVec.toArray, restToFind + excludeIds.size)
            .toVector
            .map(Int.unbox)
            .filterNot(excludeIds.contains)
            .take(restToFind)
      } else {
        Vector.empty[Int]
      }

      val restSongs = updateSimilarity(sb.getSongsByIds(restIds), baseVec)

      mixSongs ++ restSongs
    }
    songs.getOrElse(Vector.empty[MxSong])
  }

  def updateSimilarity(songs: Seq[MxSong], baseVec: VerseVec): Seq[MxSong] = {
    if (baseVec.nonEmpty)
      songs.map { song =>
        if (song.similarity.isEmpty) {
          val siml = song.vec.map { vec =>
            val dist = distance(baseVec, vec)
            if (dist > similarityBound) 0.0
            else (similarityBound - dist) / similarityBound
          }
          song.copy(similarity = siml)
        } else song
      }
    else songs
  }

  override def findSimilar(rows: Seq[(String, Syllables)], limit: Int, tags: Seq[Int]): Seq[MxSong] = {
    val songs = for {
      cp <- connectionProvider
      vt <- vectorsTree
      sb <- songsBox
      vp <- verseProcessor
    } yield {
      val significantRows = rows.filter { case (r, _) =>
        r.nonEmpty && r.exists(isCyrillic)
      }

      val convertedRows = significantRows.map { case (r, syls) =>
        val stresses = syls
          .flatMap { syl =>
            syl.accent match {
              case AccentStressed =>
                Some(new SyllableInfo(syl.pos, syl.len, SyllableInfo.StressStatus.STRESSED))
              case AccentUnstressed =>
                Some(new SyllableInfo(syl.pos, syl.len, SyllableInfo.StressStatus.UNSTRESSED))
              case _ => None
            }
          }
        val jarr = new util.ArrayList[SyllableInfo]()
        stresses.foreach(jarr.add)
        (r, new StressDescription(jarr))
      }
      val (rowsSeq, stressesSeq) = convertedRows.unzip
      val javaRows = rowsSeq.asJava
      val javaStresses = stressesSeq.asJava

      val verseDescriptions = vp.process(javaRows, javaStresses, false).asScala.toVector

      val vectors = verseDescriptions
        .map { vd =>
        vd.metricVector.asScala.toVector
          .map {
            case d: java.lang.Double => Double.unbox(d)
            case _ => 0.0
          }
      }

      def sumVec(vec1: VerseVec, vec2: VerseVec): VerseVec = {
        vec1.zip(vec2).map { case (d1, d2) => d1 + d2 }
      }

      val baseVec = vectors
        .reduceOption(sumVec)
        .map { vec => vec.map(_ / vectors.size) }
        .getOrElse(Vector.empty[Double])

      if (baseVec.nonEmpty) {

        val mixIds = mixForText match {
          case Some(kdt) =>
            val mixText = kdt.nearest(baseVec.toArray).asInstanceOf[MixText]
            val MixText(mixIds, mixVec) = mixText
            val dist = distance(baseVec, mixVec)
            if (dist <= mixForTextDistance) {
              mixIds
            } else Vector.empty[Int]
          case _ =>
            Vector.empty[Int]
        }

        var mixSongs = sb.getSongsByIds(mixIds)
          .map(song => song.copy(similarity = Some(1.0)))
        val restToFind = math.max(0, limit - mixSongs.size)

        var excludeIds = mixIds.toSet

        val restIds = if (restToFind > 0) {
          if (tags.nonEmpty) {
            val ids = vt.nearest(baseVec.toArray, similarBucket)
              .toVector
              .map(Int.unbox)
            sb.filterByTags(ids, tags)
              .view
              .filterNot(excludeIds.contains)
              .take(restToFind)
              .force
          } else
            vt.nearest(baseVec.toArray, restToFind + excludeIds.size)
              .toVector
              .map(Int.unbox)
              .filterNot(excludeIds.contains)
              .take(restToFind)
        } else Vector.empty[Int]

        val restSongs = updateSimilarity(sb.getSongsByIds(restIds), baseVec)

        mixSongs ++ restSongs
      } else Vector.empty[MxSong]
    }
    songs.getOrElse(Vector.empty[MxSong])
  }

  def calcSyllables(rows: Seq[String]): Seq[(String, Syllables)] = {
    val syllables = (for {
      vp <- verseProcessor
    } yield {
        val withIndex = rows.zipWithIndex.toVector
        val filtered = withIndex.filter { case (r, _) => r.exists(isCyrillic) }

        val vds = vp.process(filtered.map(_._1).asJava, true).asScala.toVector

        val sylsRows = vds.map { vd =>
          vd.syllables.asScala.toSeq.map( si =>
            Syllable(si.startOffset, si.length, accentTypeForStress(si.stressStatus)))
        }

        val idx2syls = filtered.zip(sylsRows)
          .map { case ((r, i), syls) => i -> syls }
          .toMap
          .withDefaultValue(Seq.empty[Syllable])

        withIndex.map { case (r, idx) => r -> idx2syls(idx) }
    }).getOrElse( rows.map(_ -> Seq.empty[Syllable]))
    syllables
  }

  override def suggest(s: String, limit: Int): Seq[TitleBox] = {
    titleSuggestor match {
      case Some(ts) => ts.suggest(s, limit)
      case _ => Seq.empty[TitleBox]
    }
  }

  def byid(ids: Seq[Int]): Seq[MxSong] =
    songsBox match {
      case Some(sb) => sb.getSongsByIds(ids)
      case _ => Seq.empty[MxSong]
    }

  def getTags: Seq[MxTag] = {
    val tags = Vector.newBuilder[MxTag]
    for (
      cp <- connectionProvider;
      st <- cp.select("SELECT id, name_rus, name_eng FROM tags")
    ) {
      val rs = st.executeQuery()
      Try {
        @tailrec
        def nextTag(): Unit = {
          if (rs.next()) {
            tags += MxTag(
              rs.getInt(1),
              Option(rs.getString(2)),
              Option(rs.getString(3)))
            nextTag()
          }
        }
        nextTag()
      }
      rs.close()
      st.close()
    }
    tags.result()
  }

  def saveFeedback(rawForm: String): Unit = {
    for (
      cp <- connectionProvider;
      st <- cp.update("INSERT INTO feedback (fb_time, text) VALUES (?, ?)")
    ) {
      val timestamp = new Timestamp(Calendar.getInstance.getTimeInMillis)
      st.setTimestamp(1, timestamp)
      st.setString(2, rawForm)
      st.executeUpdate()
      st.close()
    }
  }

  def admitUserByEmail(email: String, pwd: Option[String]): Option[(MxUser, String)] = {
    usersBox.flatMap(_.register(email, pwd))
  }

  def recognizeSession(session: String): Option[MxUser] =
    usersBox.flatMap(_.checkSession(session))

  def findVideo(song_id: Int): Option[String] = {
    (for {
      cp <- connectionProvider
      st <- cp.select(
        s"""SELECT s.title_rus, s.title_eng, a.name_rus, a.name_eng
            |FROM songs s
            |LEFT OUTER JOIN song_author sa ON sa.song_id = s.id
            |LEFT OUTER JOIN authors a ON sa.author_id = a.id
            |WHERE s.id = $song_id
         """.stripMargin)
    } yield {
      val rs = st.executeQuery()
      val name =
        if (rs.next()) {
          val author = Option(rs.getString(3)).orElse(Option(rs.getString(4)))
          val title = Option(rs.getString(1)).orElse(Option(rs.getString(2)))
          val query = Vector(author, title).flatten.mkString(". ")
          if (query.isEmpty) None
          else Some(query)
        } else None
      rs.close()
      st.close()
      name.flatMap(youtubeSearch.search)
    }).flatten
//    Some("YbnGiXm02OY")
  }
}
