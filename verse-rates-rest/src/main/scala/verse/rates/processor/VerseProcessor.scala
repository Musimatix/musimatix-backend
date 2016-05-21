package verse.rates.processor


/**
  * Created by ademin on 29.04.2016.
  */


object VerseProcessor {

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


}

trait VerseProcessor {
  import VerseProcessor._

  def invokeCalculator(verse: String): Either[ErrorCode, VerseRates]
}
