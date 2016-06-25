package verse.rates.app

import java.io.File
import java.nio.file.Files

import com.typesafe.config.ConfigFactory
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.writePretty
import verse.rates.model.VerseMetrics.LangTag
import verse.rates.processor.{ConnectionProvider, SongsBox}

import scala.util.{Failure, Success, Try}

/** **
  * Put dump to DB
  * ***/
object DumpLoaderApp {
  import ConfigHelper._

  implicit val json4sFormats = Serialization.formats(NoTypeHints)

  case class DumpSong(id: Int, title: String, author: String, text: String)


  val ReHeader = "^[^:]+:(.*)$".r

  class Loader(val cp: ConnectionProvider, folder: File) {
    def load(): Unit = {
      val songsFiles = folder.listFiles()

      var j = 1
      val songs = songsFiles.toVector.view.filter(_.isFile).map { f =>

        print(".")
        if (j % 100 == 0) print(s"[$j]\n")
        j += 1

        Try {
          val bs = io.Source.fromFile(f)
          val rows = bs.getLines().toVector
          bs.close()
          val ReHeader(id) = rows(0)
          val ReHeader(title) = rows(1)
          val ReHeader(author) = rows(2)
          val text = rows.drop(5).mkString("\n")
          DumpSong(id.toInt, title, author, text)
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

        cp.update("INSERT INTO songs (title_rus, title_eng, plain, old_id) VALUES (?, ?, ?, ?)")
          .foreach { st =>
            val lang = FabrikaImporter.checkLang(song.title)
            val (s, n) =  if (lang == LangTag.Eng) (2, 1) else (1, 2)
            st.setString(s, song.title)
            st.setNull(n, java.sql.Types.VARCHAR)
            st.setString(3, song.text)
            st.setInt(4, song.id)
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
              cp.update(s"INSERT INTO tagged (song_id, tag_id) VALUES ($songId, 2)")
                .foreach { st =>
                  st.executeUpdate()
                  st.close()
                }
          }
        }
      }
    }
  }

  def main(args: Array[String]) {
    (for {
      fld <- Try { args.head }
      confRoot <- Try { ConfigFactory.load().getConfig(confRootKey) }
      conf <- Try { confRoot.getConfig("station.mysql") }
    } yield (conf, fld)) match {
      case Success((confMsmx, folder)) =>
        val connProvider = new ConnectionProvider(confMsmx)
        val loader = new Loader(connProvider, new File(folder))

        loader.load()

        connProvider.bye()
      case Failure(f) =>
        println(f.getMessage)
    }
  }
}
