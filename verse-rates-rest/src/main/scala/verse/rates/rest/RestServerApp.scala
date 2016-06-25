package verse.rates.rest

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import spray.can.Http
import verse.rates.processor.VectorsProcessorImpl
import scala.concurrent.duration._


object RestServerApp {
  val appName = "Musimatix-REST"

  def main(args: Array[String]): Unit = {
    val rootConf = ConfigFactory.load()

    val conf = rootConf.getConfig("verse.rates.rest")

    implicit val system = ActorSystem(appName)
    val vectorsProcessor = new VectorsProcessorImpl(conf)
    val service = system.actorOf(Props(classOf[VerseRatesRestServiceActor], vectorsProcessor), "rest-service")

    implicit val timeout = Timeout(5.seconds)

    val listenAt = conf.getInt("rest.port")
    val ifc = conf.getString("rest.interface")

    println(s"App $appName started at port $listenAt.")

    IO(Http) ? Http.Bind(service, interface = ifc, port = listenAt)
  }
}
