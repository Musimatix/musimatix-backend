package verse.rates.processor

import org.json4s._
import org.json4s.jackson.Serialization
import spray.http._
import spray.http.MediaTypes._
import spray.http.HttpHeaders.RawHeader
import spray.routing.{HttpServiceActor, RequestContext, Route, HttpService}
import spray.http.MediaTypes._
import verse.rates.model.{MxUser, MxSong}
import verse.rates.model.VerseMetrics._
import verse.rates.processor.VectorsProcessor.TitleBox
import verse.rates.processor.WebFaceResponses.VectorsProcessorProvider
import verse.rates.util.StringUtil._

import scala.io.Source
import scala.util.{Failure, Try}

/** **
  *
  * ***/
object WebFaceResponses {
  trait VectorsProcessorProvider {
    val vectorsProcessor: VectorsProcessor
  }

  implicit val json4sFormats = Serialization.formats(NoTypeHints)

  val suggestLimit = 10
  val similarLimit = 6
}

abstract class WebFaceResponses extends HttpService with VectorsProcessorProvider {
  import WebFaceResponses._
  import VectorsProcessor._

  def respHtmlString(input: RequestContext => String): Route =
    respHtmlString(StatusCodes.OK)(input)

  def respHtmlString(code: StatusCode)(json: RequestContext => String): Route = {
    respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) { ctx =>
      ctx.complete(HttpResponse(code,
        HttpEntity(ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`),
          json(ctx)
        )))
    }
  }

  def respResourceExt(resourceName: String) = respHtmlString { ctx =>
    Source.fromInputStream(getClass.getResourceAsStream(resourceName))
      .getLines
      .mkString("\n")
  }
}
