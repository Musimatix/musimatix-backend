package verse.rates.processor

import org.json4s.JsonAST.{JObject, JField, JInt, JString}
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.writePretty
import spray.http._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import spray.http.MediaTypes._
import spray.http.HttpHeaders.RawHeader
import spray.routing.{RequestContext, Route, HttpService}
import spray.http.MediaTypes._
import verse.rates.processor.VectorsProcessor.TitleBox
import verse.rates.processor.VerseResponses.VectorsProcessorProvider

import scala.io.Source
import scala.util.{Failure, Try}

/** **
  *
  * ***/
object VerseResponses {
  trait VectorsProcessorProvider {
    val vectorsProcessor: VectorsProcessor
  }

  implicit val json4sFormats = Serialization.formats(NoTypeHints)

  val suggestLimit = 10
}

abstract class VerseResponses extends HttpService with VectorsProcessorProvider {
  import VerseResponses._
  import VectorsProcessor._

  def respJsonString(json: RequestContext => String): Route =
    respJsonString(StatusCodes.OK)(json)

  def respJsonString(code: StatusCode)(json: RequestContext => String): Route = {
    respondWithMediaType(`application/json`) {
      respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) { ctx =>
        ctx.complete(HttpResponse(StatusCodes.OK,
          HttpEntity(ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`),
            json(ctx)
          )))
      }
    }
  }

  def respResourceExt(resourceName: String) = respJsonString { ctx =>
    Source.fromInputStream(getClass.getResourceAsStream(resourceName))
      .getLines
      .mkString("\n")
  }

  private[this] def writeTitleBoxes(titles: Seq[TitleBox]): String = {
    val titlesVal = JField("titles",
      titles.map { tb =>
        JObject(
          JField("id", JInt(tb.id)),
          JField("title", JString(tb.title))
        )
      }
    )
    val rootObj = JObject(
      JField("object", JString("frontend.suggest.title.response")),
      JField("version", JString("1.0")),
      JField("lang", JString("eng")),
      titlesVal
    )
    writePretty(rootObj)
  }

  def respSuggestion(prefix: String, limit: Option[Int]) = respJsonString { ctx =>
    val titles = Try {
      vectorsProcessor.suggest(prefix, limit.getOrElse(suggestLimit))
    } match {
      case scala.util.Success(tbxs) => tbxs
      case Failure(_) =>
        println(s"Failed on vector producing [$prefix:$limit]")
        Seq.empty[TitleBox]
    }
    writeTitleBoxes(titles)
  }

  def respSuggestion() = respJsonString { ctx =>
    val jsonBody = ctx.request.entity.asString
    val titles = Try {
      val json = parse(jsonBody) \ "suggestTitle"
      (json \ "keywords", json \  "limit") match {
        case (JString(s), JInt(l)) => vectorsProcessor.suggest(s, l.toInt)
        case (JString(s), JNothing) => vectorsProcessor.suggest(s, suggestLimit)
        case _ => throw new IllegalArgumentException("Illegal request.\n" + jsonBody)
      }
    } match {
      case scala.util.Success(tt) => tt
      case Failure(f) =>
        val msg = Option(f.getMessage).fold("")(" " + _)
        println(s"${f.getClass.getCanonicalName}.$msg")
        Seq.empty[TitleBox]
    }
    writeTitleBoxes(titles)
  }
}
