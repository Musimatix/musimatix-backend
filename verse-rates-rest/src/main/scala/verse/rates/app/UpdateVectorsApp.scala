package verse.rates.app

import UpdateVectorsApp._
import com.typesafe.config.ConfigFactory
import verse.rates.processor.VerseProcessor
import verse.rates.processor.VerseProcessor.{VerseRates, ErrorCode}

import scala.util.{Failure, Success, Try}

object UpdateVectorsApp {
  import ConfigHelper._


  def main(args: Array[String]) {
    println("Updating vectors")
    println("Reading...")

    (for {
      confRoot <- Try { ConfigFactory.load().getConfig(confRootKey) }
      conf <- Try { confRoot.getConfig(confMsmxKey) }
      p <- MySqlParam(conf)
      c <- p.connect()
    } yield c) match {
      case Success(conMsmx) =>
        val upd = new VectorsUpdater(conMsmx)
        upd.updateTables()
        upd.bye()
        conMsmx.close()
      case Failure(f) =>
        println(f.getMessage)
    }
  }
}

