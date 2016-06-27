package verse.rates.processor

import java.io.File
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
import verse.rates.app.ConfigHelper._
import verse.rates.calculator.SampleRatesCalculator
import verse.rates.model.{MxUser, MxTag, MxSong}
import verse.rates.model.VerseMetrics._
import verse.rates.processor.VectorsProcessor._
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}
import verse.rates.util.StringUtil._
import collection.JavaConverters._
import VectorsProcessor._


object VectorsProcessorImpl {

  def createVerseProcessor(confTreeton: Config, logger: Logger): Option[VerseProcessor] = {
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

    Logger.getRootLogger.setLevel(Level.INFO)

    val processor = new VerseProcessor(metricGrammarPath, stressRestrictionViolationWeight,
      reaccentuationRestrictionViolationWeight, spacePerMeter, maxStressRestrictionViolations,
      maxReaccentuationRestrictionViolations, maxSyllablesPerVerse, false)
    processor.setProgressListener(new LoggerProgressListener("Musimatix", logger))
    processor.addLogListener(new LoggerLogListener(logger))
    processor.initialize()

    Some(processor)
  }
}

class VectorsProcessorImpl(confRoot: Config) extends VectorsProcessor {
  import VectorsProcessorImpl._

  private[this] val logger = Logger.getLogger(classOf[VerseProcessingExample])

  private[this] var verseProcessor: Option[VerseProcessor] = None
  private[this] var connectionProvider: Option[ConnectionProvider] = None
  private[this] var vectorsTree: Option[KDTree] = None // new KDTree(90)
  private[this] var titleSuggestor: Option[TitleSuggestor] = None
  private[this] var songsBox: Option[SongsBox] = None
  private[this] var usersBox: Option[UsersBox] = None

  private[this] var similarityBound = 100.0
  private[this] val similarBucket = 5000

  private[this] var mixForSongFile: Option[File] = None
  private[this] var mixForTextFolder: Option[File] = None
  private[this] var mixForTextDistance: Double = 0.0

  private[this] var mixForSong = Map.empty[Int, Seq[Int]]
  private[this] var mixForText: Option[KDTree] = None

  locally {
    init()
  }

  def loadMixels(): Unit = {
    verseProcessor.foreach { vp =>
      mixForText = Some(new KDTree(vp.getMetricVectorDimension))

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
        verseProcessor = createVerseProcessor(confTreeton, logger)
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
      st.setInt(1, id)
      val rs = st.executeQuery()
      val (idsVec, baseVec) = if (rs.next()) {
        val vec = Option(rs.getBlob(1))
          .map( blob => deserializeVerseVec(IOUtils.toByteArray(blob.getBinaryStream)) )

        vec.map { v =>
          val filteredIds = if (tags.nonEmpty) {
            val ids = vt.nearest(v.toArray, similarBucket)
              .toVector
              .map(Int.unbox)
            sb.filterByTags(ids, tags)
          } else
            vt.nearest(v.toArray, limit + 1)
              .toVector
              .map(Int.unbox)

          filteredIds.view.filter(_ != id).take(limit).force -> v
        }.getOrElse(Vector.empty[Int] -> Seq.empty[Double])
      } else {
        Vector.empty[Int] -> Seq.empty[Double]
      }
      rs.close()
      st.close()
      val ss = sb.getSongsByIds(idsVec)
      updateSimilarity(ss, baseVec)
    }
    songs.getOrElse(Vector.empty[MxSong])
  }

  def updateSimilarity(songs: Seq[MxSong], baseVec: VerseVec): Seq[MxSong] = {
    if (baseVec.nonEmpty)
      songs.map { song =>
        val siml = song.vec.map {vec =>
          val dist = distance(baseVec, vec)
          if (dist > similarityBound) 0.0
          else (similarityBound - dist) / similarityBound
        }
        song.copy(similarity = siml)
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

      val verseDescriptions = vp.process(javaRows, javaStresses).asScala.toVector

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

      val (songVec, baseVec) = vectors
        .reduceOption(sumVec)
        .map { vec =>
          val scaled = vec.map(_ / vectors.size)

          val filteredIds = if (tags.nonEmpty) {
            val ids = vt.nearest(scaled.toArray, similarBucket)
              .toVector
              .map(Int.unbox)
            sb.filterByTags(ids, tags)
              .take(limit)
          } else
            vt.nearest(scaled.toArray, limit)
              .toVector
              .map(Int.unbox)

          filteredIds -> scaled
        }.getOrElse(Seq.empty[Int] -> Seq.empty[Double])
        val ss = sb.getSongsByIds(songVec)
        updateSimilarity(ss, baseVec)
    }
    songs.getOrElse(Vector.empty[MxSong])
  }

  def calcSyllables(rows: Seq[String]): Seq[(String, Syllables)] = {
    val syllables = (for {
      vp <- verseProcessor
    } yield {
        val withIndex = rows.zipWithIndex.toVector
        val filtered = withIndex.filter { case (r, _) => r.exists(isCyrillic) }

        val vds = vp.process(filtered.map(_._1).asJava).asScala.toVector

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
      case Some(ts) => ts.suggest(s.toLowerCase, limit)
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

  def admitUserByEmail(email: String): Option[(MxUser, String)] = {
    usersBox.flatMap(_.register(email))
  }

  def recognizeSession(session: String): Option[MxUser] =
    usersBox.flatMap(_.checkSession(session))

  def findVideo(song_id: Int): Option[String] = {
    Some("YbnGiXm02OY")
  }
}
