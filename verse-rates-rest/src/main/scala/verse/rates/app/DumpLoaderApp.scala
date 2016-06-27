package verse.rates.app

import java.io.{ByteArrayInputStream, File}
import java.nio.file.Files

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.log4j.Logger
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.writePretty
import treeton.prosody.musimatix.{VerseProcessingExample, VerseProcessor}
import verse.rates.model.{VerseMetrics, MxTag}
import verse.rates.model.VerseMetrics.LangTag
import verse.rates.processor.{VectorsProcessorImpl, ConnectionProvider, SongsBox}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/** **
  * Put dump to DB
  * ***/
object DumpLoaderApp {
  import ConfigHelper._

  implicit val json4sFormats = Serialization.formats(NoTypeHints)

  case class DumpSong(id: Option[Int], title: String, author: String, text: String, ext: String,
    tags: Seq[Int], vec: Seq[Double])

  val ReHeader = "^[^:]+:(.*)$".r
  val ReVec = "^\\s*\\((.*)\\)\\s*$".r

  class Loader(val cp: ConnectionProvider, val confTreeton: Config, val folder: File) {

    private[this] val logger = Logger.getLogger(classOf[VerseProcessingExample])

    val verseProcessor = VectorsProcessorImpl.createVerseProcessor(confTreeton, logger)

    val tagByName = readTags

    def load(): Unit = {
      val songsFiles = folder.listFiles()

      var j = 1
      val songs = songsFiles.toVector.view
        .filter(f => f.isFile && f.getPath.endsWith(".txt")).map { f =>

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
            .flatMap(t => tagByName.get(t.trim)) :+ 2
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
          case Success(sn) => sn
          case Failure(x) =>
            println(s"Error loading ${f.getName}\n$x")
            throw x
        }
      }.force

      val authors = songs.map(_.author).distinct

      val authorsMapBuilder = Map.newBuilder[String, Int]
      authors.foreach { a =>
        cp.update("INSERT INTO authors (name_rus, name_eng) VALUES (?, ?)").foreach { st =>
          val lang = FabrikaImporter.checkLang(a)
          val (s, n) =  if (lang == LangTag.Eng) (2, 1) else (1, 2)
          st.setString(s, a)
          st.setNull(n, java.sql.Types.VARCHAR)
          val keysCount = st.executeUpdate()
          val rsKeys = st.getGeneratedKeys
          val key: Option[Int] = if(rsKeys.next()) Some(rsKeys.getInt(1)) else None
          rsKeys.close()
          st.close()
          key.foreach { k => authorsMapBuilder += a -> k }
        }
      }
      val authorsMap = authorsMapBuilder.result()
      println(s"Authors: ${authorsMap.size}")

      var i = 1
      songs.foreach { song =>
        print(".")
        if (i % 100 == 0) print(s"[$i]\n")
        i += 1

        cp.update("INSERT INTO songs (title_rus, title_eng, plain, old_id, vector) VALUES (?, ?, ?, ?)")
          .foreach { st =>
            val lang = FabrikaImporter.checkLang(song.title)
            val (s, n) =  if (lang == LangTag.Eng) (2, 1) else (1, 2)
            st.setString(s, song.title)
            st.setNull(n, java.sql.Types.VARCHAR)
            st.setString(3, song.text)
            song.id.foreach(id => st.setInt(4, id))
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
        println(f.getMessage)
    }
  }
}
