package verse.rates.calculator

/** **
  *
  * ***/
class SampleRatesCalculator {

  def init(): Unit = {
    println("init")
  }

  def calculate(s: String): Array[Array[java.lang.Double]] = {
    val rows = s.split("\n")

    def rowRates(row: String): Array[java.lang.Double] =
      row
        .split("\\s+")
        .map( w => java.lang.Double.valueOf(w.length / 2.0) )

    val rowsRates = rows.map(rowRates).toList

    val minWords = rowsRates.view.map(_.length).min
    val total = for(i <- 0 until minWords) yield {
      val wordRate = rowsRates.map(_.apply(i)).foldLeft(0.0)(_ + _) / rowsRates.size
      java.lang.Double.valueOf(wordRate)
    }

    (rowsRates :+ total.toArray).toArray
  }
}
