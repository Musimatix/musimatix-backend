package verse.rates.app

import com.typesafe.config.ConfigFactory
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.writePretty
import verse.rates.model.VerseMetrics.LangTag
import verse.rates.processor.{ConnectionProvider, SongsBox}

import scala.util.{Failure, Success, Try}

/** **
  *
  * ***/
object DumpLoaderApp {
  import ConfigHelper._

  implicit val json4sFormats = Serialization.formats(NoTypeHints)

  class Checker(val cp: ConnectionProvider) {
    def printSong(id: Int): Unit = {
      val sb = new SongsBox(cp)
      sb.getSongsByIds(Vector(id)).foreach { song =>
        println(writePretty(SongsBox.song2Json(song, LangTag.Rus)))
      }
    }

    def printStat(): Unit = {
      cp.select("SELECT COUNT(*) FROM songs").foreach { st =>
        val rs = st.executeQuery()
        if (rs.next()) {
          val nSongs = rs.getInt(1)
          println(s"Connected. Songs in collection: $nSongs.")
        } else {
          println(s"Connected. But songs collection is empty.")
        }
        rs.close()
        st.close()
      }
    }
  }

  def main(args: Array[String]) {
    (for {
      confRoot <- Try { ConfigFactory.load().getConfig(confRootKey) }
      conf <- Try { confRoot.getConfig("station.mysql") }
    } yield conf) match {
      case Success(confMsmx) =>
        val connProvider = new ConnectionProvider(confMsmx)
        val checker = new Checker(connProvider)

        checker.printStat()
//        checker.printSong(600)

        connProvider.bye()
      case Failure(f) =>
        println(f.getMessage)
    }
  }
}
