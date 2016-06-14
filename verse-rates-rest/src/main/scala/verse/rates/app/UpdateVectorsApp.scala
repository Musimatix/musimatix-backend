package verse.rates.app

import UpdateVectorsApp._
import com.typesafe.config.ConfigFactory
import org.apache.log4j.{Level, Logger}
import verse.rates.processor.VectorsProcessor$
import verse.rates.processor.VectorsProcessor.{VerseRates, ErrorCode}

import scala.util.{Failure, Success, Try}

object UpdateVectorsApp {
  import ConfigHelper._


  def main(args: Array[String]) {
    println("Updating vectors")
    println("Reading...")

    Logger.getRootLogger.setLevel(Level.WARN)

    (for {
      sta <- Try { args.headOption.map(_.trim.toInt) }
      crt <- Try { ConfigFactory.load().getConfig(confRootKey) }
    } yield (sta, crt)) match {
      case Success((start, _)) =>
        val upd = new VectorsUpdater(start)
        upd.updateTables()
        upd.bye()
        println("done")
      case Failure(f) =>
        println(f.getMessage)
    }
  }
}

