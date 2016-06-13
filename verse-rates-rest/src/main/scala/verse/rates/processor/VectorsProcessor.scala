package verse.rates.processor

import verse.rates.model.VerseMetrics.{VerseVec, Syllables}


/**
  * Created by ademin on 29.04.2016.
  */


object VectorsProcessor {

  type ErrorCode = Int

  type Rates = Seq[Double]
  type RatesSeq = Seq[Rates]

  case class VerseRates(rowsRates: RatesSeq, totalRates: Rates)

  object ErrorMessage {
    val ok = 0
    val emptyResponse = 404
    val badRequest    = 400
  }
  case class ErrorMessage(code: ErrorCode, message: Option[String])

  case class Author(id: Int, name: String)
  case class SongTag(id: Int, name: String)

  case class FullSong(id: Int, title: String, authors: Seq[Author],
    rowsPlain: Seq[String], syllables: Syllables, tags: Seq[SongTag],
    vector: VerseVec = Vector.empty[Double])

  case class TitleBox(id: Int, title: String)
}

trait VectorsProcessor {
  import VectorsProcessor._

  def invokeCalculator(verse: String): Either[ErrorCode, VerseRates]

  def findSimilar(id: Int, limit: Int): Seq[FullSong]

  def findSimilar(rows: Seq[String], limit: Int): Seq[FullSong]

  def suggest(s: String, limit: Int): Seq[TitleBox]
}
