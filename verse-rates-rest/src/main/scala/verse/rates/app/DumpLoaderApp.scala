package verse.rates.app

import java.io.{ByteArrayInputStream, File}
import java.nio.file.Files
import java.sql.PreparedStatement

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.log4j.{Level, Logger}
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import treeton.prosody.musimatix.{SyllableInfo, VerseProcessingExample, VerseProcessor}
import verse.rates.model.{VerseMetrics, MxTag}
import verse.rates.model.VerseMetrics._
import verse.rates.processor.VectorsProcessor._
import verse.rates.processor.{VectorsProcessorImpl, ConnectionProvider, SongsBox}
import verse.rates.util.StringUtil._
import collection.JavaConverters._
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/** **
  * Put dump to DB
  * ***/
object DumpLoaderApp {
  import ConfigHelper._

  Logger.getRootLogger.setLevel(Level.WARN)

  implicit val json4sFormats = Serialization.formats(NoTypeHints)

  case class DumpSong(id: Option[Int], title: String, author: String, text: String, ext: String,
    tags: Seq[Int], vec: Seq[Double])

  def setBlobValue(st: PreparedStatement, field: Int, bOpt: Option[Array[Byte]]): Unit = {
    bOpt match {
      case Some(b) =>
        st.setBlob(field, new ByteArrayInputStream(b))
      case _ =>
        st.setNull(field, java.sql.Types.BLOB)
    }
  }

  val ReHeader = "^[^:]+:(.*)$".r
  val ReVec = "^\\s*\\((.*)\\)\\s*$".r

  class Loader(val cp: ConnectionProvider, val confTreeton: Config, val folder: File) {

    private[this] val logger = Logger.getLogger("DumpLoader")
    logger.setLevel(Level.WARN)

    val verseProcessor = VectorsProcessorImpl.createVerseProcessor(confTreeton, logger)

    val tagByName = readTags

    def load(): Unit = {
      val songsFiles = folder.listFiles()

      var j = 1

      case class TitleAuthor(title: String, author: String)

      var passed = Set.empty[TitleAuthor]
      val songsBuilder = Vector.newBuilder[DumpSong]

      songsFiles.toVector.view
        .filter(f => f.isFile && f.getPath.endsWith(".txt")).foreach { f =>

        print(".")
        if (j % 100 == 0) print(s"[$j]\n")
        j += 1

        Try {
          val bs = io.Source.fromFile(f)
          val rows = bs.getLines().toVector
          bs.close()
          val ReHeader(idS) = rows(0)
          val ReHeader(title) = rows(1)
          val ReHeader(author) = rows(2)
          val ReHeader(ext) = rows(3)
          val ReHeader(tags) = rows(4)

          val tagsIds = tags.split(";").toVector
            .flatMap(t => tagByName.get(t.trim.toLowerCase)) :+ 2
          val idOpt = Try { idS.trim.toInt }.toOption

          val text = rows.drop(5).mkString("\n")

          val vec = Try {
            val bs = io.Source.fromFile(s"${f.getPath}.vector")
            val ReVec(vecStr) = bs.getLines().toVector.head
            bs.close()
            vecStr.split(";").toVector.map(_.trim.toDouble)
          }.toOption.getOrElse(Seq.empty[Double])

          DumpSong(idOpt, title, author, text, ext, tagsIds, vec)
        } match {
          case Success(sn) =>
            val ta = TitleAuthor(sn.title, sn.author)
            if (!passed.contains(ta)) {
              songsBuilder += sn
              passed += ta
            }
          case Failure(x) =>
            println(s"Error loading ${f.getName}\n$x")
        }
      }

      val songs = songsBuilder.result().drop(17912)

//      val authors = songs.map(_.author).distinct

      val authorsMapBuilder = Map.newBuilder[String, Int]

      cp.select("SELECT id, name_rus, name_eng FROM authors").foreach { st =>
        val rs = st.executeQuery()
        @tailrec
        def nextAuthor(): Unit = {
          if (rs.next()) {
            val id = rs.getInt(1)
            val nameRus = Option(rs.getString(2))
            val nameEng = Option(rs.getString(3))
            nameRus.orElse(nameEng).foreach { n =>
              authorsMapBuilder += n -> id
            }
            nextAuthor()
          }
        }
        nextAuthor()

        rs.close()
        st.close()
      }

//      authors.foreach { a =>
//        cp.update("INSERT INTO authors (name_rus, name_eng) VALUES (?, ?)").foreach { st =>
//          val lang = FabrikaImporter.checkLang(a)
//          val (s, n) =  if (lang == LangTag.Eng) (2, 1) else (1, 2)
//          st.setString(s, a)
//          st.setNull(n, java.sql.Types.VARCHAR)
//          val keysCount = st.executeUpdate()
//          val rsKeys = st.getGeneratedKeys
//          val key: Option[Int] = if(rsKeys.next()) Some(rsKeys.getInt(1)) else None
//          rsKeys.close()
//          st.close()
//          key.foreach { k => authorsMapBuilder += a -> k }
//        }
//      }


      val authorsMap = authorsMapBuilder.result()
      println(s"Authors: ${authorsMap.size}")

      var i = 1
      songs.foreach { song =>
        print(".")
        if (i % 100 == 0) print(s"[$i]\n")
        i += 1

        cp.update("INSERT INTO songs (title_rus, title_eng, plain, old_id, vector) VALUES (?, ?, ?, ?, ?)")
          .foreach { st =>
            val lang = FabrikaImporter.checkLang(song.title)
            val (s, n) =  if (lang == LangTag.Eng) (2, 1) else (1, 2)
            st.setString(s, song.title)
            st.setNull(n, java.sql.Types.VARCHAR)
            st.setString(3, song.text)
            song.id match {
              case Some(id) => st.setInt(4, id)
              case _ => st.setNull(4, java.sql.Types.INTEGER)
            }
            if (song.vec.nonEmpty) {
              val songVecBytes = VerseMetrics.serializeVerseVec(song.vec)
              st.setBlob(5, new ByteArrayInputStream(songVecBytes))
            } else {
              st.setNull(5, java.sql.Types.BLOB)
            }

            st.executeUpdate()
            val rsKeys = st.getGeneratedKeys
            val songIdOpt = if(rsKeys.next()) Some(rsKeys.getInt(1)) else None
            rsKeys.close()
            st.close()

            songIdOpt.foreach { songId =>
              val authorId = authorsMap(song.author)
              cp.update(s"INSERT INTO song_author (song_id, author_id) VALUES ($songId, $authorId)")
                .foreach { st =>
                  st.executeUpdate()
                  st.close()
                }
              song.tags.foreach { tag =>
                cp.update(s"INSERT INTO tagged (song_id, tag_id) VALUES ($songId, $tag)")
                  .foreach { st =>
                    st.executeUpdate()
                    st.close()
                  }
              }
              processRows(songId, song.text)
            }
        }
      }
    }

    def saveCalculatedRows(id: Int, calculatedRows: Vector[(Int, String, Option[Vector[Syllable]])]): Unit = {
      calculatedRows.foreach { case (idx, row, syls) =>
        val sv = syls.map(serializeSyllables)
        cp.update("INSERT INTO rows (song_id, idx, plain, accents) VALUES (?, ?, ?, ?)")
          .foreach { st =>
            st.setInt(1, id)
            st.setInt(2, idx)
            st.setString(3, row)
            setBlobValue(st, 4, sv)
            val keysCount = st.executeUpdate()
            st.close()
          }
      }
    }

    def processRows(id: Int, text: String): Unit = {
      verseProcessor.foreach { p =>
        val rows = text
          .split("\n")
          .flatMap { r =>
            val rNorm = r.replace("\r", "").reverse.dropWhile(_.isSpaceChar).reverse
            if (rNorm.exists(_.isLetter) || rNorm.isEmpty) {
              Some(rNorm)
            } else None
          }

        val withIndex = rows.zipWithIndex.toVector
        val filteredForProcessing = withIndex.filter { case (r, _) => r.exists(isCyrillic) }
        if (filteredForProcessing.nonEmpty) {
          val javaCollection = filteredForProcessing.map(_._1).asJava
          Try { p.process(javaCollection, true).asScala } match {
            case Success(metricsArray) =>
              val index2Metrics = filteredForProcessing
                .zip(metricsArray)
                .map { case ((_, i), m) => i -> m }
                .toMap
              val calculatedRows = withIndex.map { case (r, i) =>
                val vmOpt = index2Metrics.get(i).map { m =>
                  val sv = m.syllables.asScala.toVector
                    .flatMap {
                      case si: SyllableInfo =>
                        Some(Syllable(si.startOffset, si.length, accentTypeForStress(si.stressStatus)))
                      case _ => None
                    }
                  sv
                }
                (i, r, vmOpt)
              }
              saveCalculatedRows(id, calculatedRows)

            case Failure(f) =>
              println(s"\nFailure on song: $id\n${failureMessage(f)}")
          }
        }
      }
    }


    def readTags: Map[String, Int] = {
      val tags = Map.newBuilder[String, Int]
      cp.select("SELECT id, name_eng FROM tags").foreach { st =>
        val rs = st.executeQuery()
        Try {
          @tailrec
          def nextTag(): Unit = {
            if (rs.next()) {
              tags += rs.getString(2).toLowerCase -> rs.getInt(1)
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
  }

  def main(args: Array[String]) {

    (for {
      fld <- Try { args.head }
      confRoot <- Try { ConfigFactory.load().getConfig(confRootKey) }
      confDb <- Try { confRoot.getConfig(confMsmxKey) }
      confTreeton <- Try { confRoot.getConfig(confTreetonKey) }
    } yield (fld, confDb, confTreeton)) match {
      case Success((folder, confDb, confTreeton)) =>
        val connProvider = new ConnectionProvider(confDb)
        val loader = new Loader(connProvider, confTreeton, new File(folder))

        loader.load()

        connProvider.bye()
      case Failure(f) =>
        println(failureMessage(f))
    }
  }
}
