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
import verse.rates.face.{WebFaceServiceActor, WebFaceService}
import verse.rates.processor.VectorsProcessorImpl
import scala.concurrent.duration._


object RestServerApp {
  val appName = "Musimatix-Server"

  val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {

//    Logger.getRootLogger.setLevel(Level.WARN)

    val rootConf = ConfigFactory.load()

    val conf = rootConf.getConfig(confRootKey)

    implicit val system = ActorSystem(appName)

    val dbContext = conf.getString("db.context")

    val vectorsProcessor = new VectorsProcessorImpl(conf)
    val restService = system.actorOf(Props(classOf[VerseRatesRestServiceActor], vectorsProcessor), "rest-service")
    val faceService = system.actorOf(Props(classOf[WebFaceServiceActor], vectorsProcessor), "face-service")

    implicit val timeout = Timeout(5.seconds)

    val restConf = conf.getConfig(confRestKey)
    val restListenAt = restConf.getInt("port")
    val restIfc = restConf.getString("interface")

    val faceConf = conf.getConfig(confFaceKey)
    val faceListenAt = faceConf.getInt("port")
    val faceIfc = faceConf.getString("interface")

    val startedMsg = s"$appName started REST $restIfc:$restListenAt and Web $faceIfc:$faceListenAt with DB context '$dbContext'."

    IO(Http) ? Http.Bind(restService, interface = restIfc, port = restListenAt)
    IO(Http) ? Http.Bind(faceService, interface = faceIfc, port = faceListenAt)

    logger.info(startedMsg)
    println(startedMsg)
  }
}
