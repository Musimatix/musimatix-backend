package verse.rates.rest

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.apache.log4j.{Level, Logger}
import org.slf4j.LoggerFactory
import spray.can.Http
import verse.rates.env.ConfigHelper
import ConfigHelper._
import verse.rates.processor.VectorsProcessorImpl
import scala.concurrent.duration._


object RestServerApp {
  val appName = "Musimatix-REST"

  val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {

//    Logger.getRootLogger.setLevel(Level.WARN)

    val rootConf = ConfigFactory.load()

    val conf = rootConf.getConfig(confRootKey)

    implicit val system = ActorSystem(appName)
    val vectorsProcessor = new VectorsProcessorImpl(conf)
    val service = system.actorOf(Props(classOf[VerseRatesRestServiceActor], vectorsProcessor), "rest-service")

    implicit val timeout = Timeout(5.seconds)

    val serverConf = conf.getConfig(confRestKey)
    val listenAt = serverConf.getInt("port")
    val ifc = serverConf.getString("interface")

    logger.info(s"App $appName started at port $listenAt.")
    println(s"App $appName started at port $listenAt.")

    IO(Http) ? Http.Bind(service, interface = ifc, port = listenAt)
  }
}
