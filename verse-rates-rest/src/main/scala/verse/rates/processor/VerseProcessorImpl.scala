package verse.rates.processor

import com.typesafe.config.Config
import verse.rates.calculator.SampleRatesCalculator
import verse.rates.processor.VerseProcessor.{VerseRates, ErrorCode}

/** **
  *
  * ***/
class VerseProcessorImpl(conf: Config) extends VerseProcessor {

  val calc = new SampleRatesCalculator

  override def invokeCalculator(verse: String): Either[ErrorCode, VerseRates] = {

    val rates = calc.calculate(verse)

    val ratesAsScala = rates
      .map(_.map(_.asInstanceOf[Double]).toVector)
      .toVector

    val rowsRates = ratesAsScala.take(rates.length - 1)
    val totalRates = ratesAsScala.last


    Right(
      VerseRates(
        rowsRates,
        totalRates
      )
    )
  }
}
