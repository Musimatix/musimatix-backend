package verse.rates.app

import java.sql.Connection

import com.typesafe.config.ConfigFactory
import verse.rates.processor.ConnectionProvider

import scala.util.{Failure, Success, Try}

/** **
  *
  * ***/
object CheckConnectionApp {
  import ConfigHelper._

  class Checker(val con: Connection) {

    def statement(s: String) = con.prepareStatement(s.stripMargin.replaceAll("\n", " "))

    def printStat(): Unit = {
      val st = statement("SELECT COUNT(*) FROM songs")
      val rs = st.executeQuery()
      if (rs.next()) {
        val nSongs = rs.getInt(1)
        println(s"Connected. Songs in collection: ${nSongs}.")
      } else {
        println(s"Connected. But songs collection is empty.")
      }
      rs.close()
      st.close()
    }
  }

  def main(args: Array[String]) {
    (for {
      confRoot <- Try { ConfigFactory.load().getConfig(confRootKey) }
      conf <- Try { confRoot.getConfig(confMsmxKey) }
    } yield conf) match {
      case Success(confMsmx) =>
        val connProvider = new ConnectionProvider(confMsmx)
        connProvider.connection().foreach { conn =>
          new Checker(conn).printStat()
        }
        connProvider.bye()
      case Failure(f) =>
        println(f.getMessage)
    }
  }
}
