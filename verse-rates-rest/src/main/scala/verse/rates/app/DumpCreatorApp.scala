package verse.rates.app

import java.io.{PrintWriter, FileOutputStream}

import com.typesafe.config.ConfigFactory
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization._
import verse.rates.model.VerseMetrics.LangTag
import verse.rates.processor.{SongsBox, ConnectionProvider}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/** **
  *
  * ***/
object DumpCreatorApp {
  import ConfigHelper._

  val confMsmxLargeKey = "fabrika.large.mysql"
  val folder = "../musimatix-dump/"

  class Dumper(val cp: ConnectionProvider) {
    def dump(): Unit = {
      cp.select(
        """SELECT c.id, c.title, a.title, c.text_clean, c.external_link
          |FROM composition c
          |LEFT OUTER JOIN author_composition ac ON c.id = ac.composition_id
          |LEFT OUTER JOIN author a ON ac.author_id = a.id
          |WHERE c.external_link LIKE '%amdm.ru%'
        """.stripMargin
      ).foreach { st =>
        val rs = st.executeQuery()

        var cur = 0
        next()

        @tailrec
        def next(): Unit = {
          if (rs.next()) {
            val id = rs.getInt (1)
            val title = Option(rs.getString (2)).getOrElse("")
            val author = Option(rs.getString (3)).getOrElse("")
            val text = Option(rs.getString (4)).getOrElse("")
            val link = Option(rs.getString (5)).getOrElse("")

            val fos = new FileOutputStream(s"$folder$id.txt")
            val p = new PrintWriter(fos)
            p.println(s"id:$id")
            p.println(s"title:$title")
            p.println(s"author:$author  ")
            p.println(s"ext_link:$link  ")
            p.println(s"tags:")
            p.println(text)
            p.close()
            fos.close()

            cur += 1
            if (cur % 1000 == 0) println(cur)

            next()
          }


        }
        rs.close()
        st.close()
      }
    }
  }

  def main(args: Array[String]) {
    (for {
      confRoot <- Try { ConfigFactory.load().getConfig(confRootKey) }
      conf <- Try { confRoot.getConfig(confMsmxLargeKey) }
    } yield conf) match {
      case Success(confMsmx) =>
        val connProvider = new ConnectionProvider(confMsmx)
        val dumper = new Dumper(connProvider)

        dumper.dump()

        connProvider.bye()
      case Failure(f) =>
        println(f.getMessage)
    }
  }
}
