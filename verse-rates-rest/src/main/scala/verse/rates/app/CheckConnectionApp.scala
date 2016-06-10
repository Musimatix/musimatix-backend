package verse.rates.app

import java.sql.Connection

import com.typesafe.config.ConfigFactory

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
      p <- MySqlParam(conf)
      c <- p.connect()
    } yield c) match {
      case Success(conMsmx) =>
        new Checker(conMsmx).printStat()
        conMsmx.close()
      case Failure(f) =>
        println(f.getMessage)
    }
  }
}
