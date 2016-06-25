package verse.rates.app

import java.sql.Connection

import com.typesafe.config.ConfigFactory
import org.apache.commons.io.IOUtils
import verse.rates.env.MySqlParam
import verse.rates.model.VerseMetrics.{VerseVec, Syllables}
import verse.rates.model.VerseMetrics._

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/** **
  *
  * ***/
object ReadSongRowsApp {

  val confRootKey = "verse.rates.rest"
  val confMsmxKey = "msmx.mysql"

  case class RowData(idx: Int, plain: String, accents: Option[Syllables], vec: Option[VerseVec])

  def readRowsForSong(con: Connection, id: Int): Seq[RowData] = {
    def statement(s: String) = con.prepareStatement(s.stripMargin.replaceAll("\n", " "))

    val builder = Vector.newBuilder[RowData]

    val st = statement("SELECT idx, plain, accents, vector FROM rows WHERE song_id = ? ORDER BY idx")
    st.setInt(1, id)
    val rs = st.executeQuery()
    Try {
      @tailrec
      def nextRow(): Unit = {
        if (rs.next()) {
          val idx = rs.getInt(1)
          val plain = Option(rs.getString(2)).getOrElse("")
          val accents = Option(rs.getBlob(3))
            .map( blob => IOUtils.toByteArray(blob.getBinaryStream) )
            .map(deserializeSyllables)
          val vector = Option(rs.getBlob(4))
            .map( blob => IOUtils.toByteArray(blob.getBinaryStream) )
            .map(deserializeVerseVec)
          builder += RowData(idx, plain, accents, vector)
          nextRow()
        }
      }
      nextRow()
    }
    rs.close()
    st.close()
    builder.result()
  }

  def readVectorForSong(con: Connection, id: Int): Option[VerseVec] = {
    def statement(s: String) = con.prepareStatement(s.stripMargin.replaceAll("\n", " "))

    val st = statement("SELECT vector FROM songs WHERE id = ?")
    st.setInt(1, id)
    val rs = st.executeQuery()
    val vector = if (rs.next()) {
      Option(rs.getBlob(1))
        .map( blob => IOUtils.toByteArray(blob.getBinaryStream) )
        .map(deserializeVerseVec)
    } else {
      None
    }
    rs.close()
    st.close()
    vector
  }


  def main(args: Array[String]) {
    println("Reading...")

    (for {
      confRoot <- Try { ConfigFactory.load().getConfig(confRootKey) }
      conf <- Try { confRoot.getConfig(confMsmxKey) }
      p <- MySqlParam(conf)
      c <- p.connect()
    } yield c) match {
      case Success(conMsmx) =>
        val rows = readRowsForSong(conMsmx, 7)
        println(s"rows: ${rows.size}")
        val vec = readVectorForSong(conMsmx, 7)
        println(s"vec: $vec")

        conMsmx.close()
      case Failure(f) =>
        println(f.getMessage)
    }
  }
}
