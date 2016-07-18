package verse.rates.face

import akka.actor.Actor
import com.sun.tools.internal.ws.wscompile.AuthInfo
import org.json4s.JsonAST.{JField, JObject}
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.writePretty
import org.log4s._
import spray.http.MediaTypes._
import spray.http._
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.routing.AuthorizationFailedRejection
import spray.routing.authentication.{BasicAuth, UserPass}
import spray.routing.directives.AuthMagnet
import verse.rates.processor.VectorsProcessor.{ErrorMessage => EM}
import verse.rates.processor.{VectorsProcessor, WebFaceResponses}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global


class WebFaceServiceActor (override val vectorsProcessor: VectorsProcessor)
  extends WebFaceService with Actor {

  def actorRefFactory = context
  def receive = runRoute(ciRoute)
}

object WebFaceService {
  val msgUnknown = "Unknown request: %s"

  val schemaPath = "/schema/"
  val htmlPath = "/schema/"

  implicit val json4sFormats = Serialization.formats(NoTypeHints)
}

abstract class WebFaceService
  extends WebFaceResponses {

  import WebFaceService._

  import language.postfixOps

  private[this] val logger = getLogger

  def basicUserAuthenticator(implicit ec: ExecutionContext): AuthMagnet[String] = {
    def validateUser(userPass: Option[UserPass]): Option[String] = {
      for {
        p <- userPass
        if p.user == "admin" && p.pass == "xxx"
      } yield p.user
    }

    def authenticator(userPass: Option[UserPass]): Future[Option[String]] = Future { validateUser(userPass) }

    BasicAuth(authenticator _, realm = "Musimatix Private")
  }

  val ciRoute =
    post {
      pathPrefix("users") {
        path("list") {
          authenticate(basicUserAuthenticator) { userName =>
            respHtmlString(_ => "stat.songs")
          }
        }
      } ~
      pathPrefix("stat") {
        path("songs") {
          authenticate(basicUserAuthenticator) { userName =>
            respHtmlString(_ => s"stat.songs $userName")
          }
        }
      }
    } ~
    get {
      pathPrefix("stat") {
        authenticate(basicUserAuthenticator) { userName =>
          path("songs") {
            respHtmlString(_ => s"stat.songs $userName")
          }
        }
      } ~
      pathEndOrSingleSlash {
        respResourceExt("/html/root.html")
      } //~
//      path(Rest) { s =>
//        respondWithMediaType(`text/html`) { ctx =>
//          ctx.complete(HttpResponse(StatusCodes.BadRequest, HttpEntity(ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`),
//            writePretty(List(EM(EM.badRequest, Some("Illegal request URL")))))))
//        }
//      }
  }
}
