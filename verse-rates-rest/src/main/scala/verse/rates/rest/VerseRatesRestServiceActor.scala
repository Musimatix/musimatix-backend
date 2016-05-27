package verse.rates.rest

import akka.actor.Actor
import org.json4s.JsonAST.{JString, JField, JObject}
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.writePretty
import spray.http.HttpHeaders.RawHeader
import verse.rates.processor.VerseProcessor
import verse.rates.rest.VerseRatesRestService._
import spray.http.MediaTypes._
import spray.http._
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.routing._
import spray.routing.directives.BasicDirectives
import scala.io.Source
import VerseProcessor.{ErrorMessage => EM}


class VerseRatesRestServiceActor (override val verseProcessor: VerseProcessor)
  extends VerseRatesRestService with Actor with HttpService {

  def actorRefFactory = context
  def receive = runRoute(ciRoute)
}

object VerseRatesRestService {
  val msgUnknown = "Unknown request: %s"

  val schemaPath = "/schema/"

  val metaResourceName = schemaPath + "meta.json"
  val errorResourceName = schemaPath + "error.json"
  val songsResourceName = schemaPath + "frontend.songs.response.sample.json"
  val tagsResourceName = schemaPath + "frontend.tags.response.sample.json"
  val suggestTitleResourceName = schemaPath + "frontend.suggest.title.response.sample.json"

  def respResource(ctx: RequestContext, resourceName: String) =
    ctx.complete(HttpResponse(StatusCodes.OK,
      HttpEntity(ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`),
      Source.fromInputStream(getClass.getResourceAsStream(resourceName))
        .getLines
        .mkString("\n")
    )))
}

abstract class VerseRatesRestService
  extends HttpService {

  import VerseRatesRestService._

  import language.postfixOps

  implicit val json4sFormats = Serialization.formats(NoTypeHints)

  def verseProcessor: VerseProcessor

  def respResource(resourceName: String) =
    respondWithMediaType(`application/json`) { ctx =>
      ctx.complete(HttpResponse(StatusCodes.OK,
        HttpEntity(ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`),
          Source.fromInputStream(getClass.getResourceAsStream(resourceName))
            .getLines
            .mkString("\n")
        )))
    }

  def respResourceExt(resourceName: String) =
    respondWithMediaType(`application/json`) {
      respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
        complete(HttpResponse(StatusCodes.OK,
          HttpEntity(ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`),
            Source.fromInputStream(getClass.getResourceAsStream(resourceName))
              .getLines
              .mkString("\n")
          )))
      }
    }

  case class VerseRows(rows: Seq[String])

  case class Verse(verse: VerseRows)

  object VerseJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
    implicit val VerseRowsFormats = jsonFormat1(VerseRows)
    implicit val VerseFormats = jsonFormat1(Verse)
  }

  import VerseJsonSupport._

  val ciRoute =
    post {
      pathPrefix("songs" / "rates") {
        path("invoke") {
          entity(as[Verse]) { verse =>
            respondWithMediaType(`application/json`) { ctx =>
              val (code, output) = {
                val sVerse = verse.verse.rows.mkString("\n")

                verseProcessor.invokeCalculator(sVerse) match {
                  case Right(rates) => StatusCodes.OK -> {

                    val rowsRates = rates.rowsRates
                      .map { row =>
                        val rowVals = row.map(JDouble.apply)
                        JArray(rowVals.toList)
                      }
                      .toList
                    val jsonRowsRates = JArray(rowsRates)

                    val totalRates = rates.totalRates
                      .map(JDouble.apply)
                      .toList
                    val jsonTotalRates = JArray(totalRates)

                    JObject(JField("verse", JObject(
                      JField("rates",
                        JObject(
                          JField("rows", jsonRowsRates),
                          JField("total", jsonTotalRates)
                        )
                      )
                    )))
                  }

                  case Left(EM.`badRequest`) => StatusCodes.BadRequest ->
                    List(EM(EM.badRequest, Some("Illegal request")))
                  case _ => StatusCodes.NotFound ->
                    List(EM(EM.emptyResponse, Some("Can't calculate rates")))
                }
              }
              ctx.complete(
                HttpResponse(code, HttpEntity(ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`),
                  writePretty(output))))
            }
          }
        }
      } ~
      pathPrefix("songs" / "search") {
        path("similar") {
          respResourceExt(songsResourceName)
        } ~
        path("byid") {
          respResourceExt(songsResourceName)
        } ~
        path("keywords") {
          respResourceExt(songsResourceName)
        } ~
        path("suggest_title") {
          respResourceExt(suggestTitleResourceName)
        }
      }
    } ~
    get {
      pathPrefix("songs" / "env" / "tags") {
        respResourceExt(tagsResourceName)
      } ~
      pathEndOrSingleSlash {
        respondWithMediaType(`text/html`) {
          complete {
            val httpPrefix = s"http://host:port/"
            val pathPrefix = s"songs/rates/invoke"
            <html>
              <body>
                <p>
                  <b>Requests format:</b>
                </p>
                <pre>
                  {s"""
                    |URL: $httpPrefix$pathPrefix
                    |Method: POST
                    |Content-type: application/json
                    |""".stripMargin
                  }
                </pre>
                <p>
                  <b>Input JSON example:</b>
                </p>
                <pre>
                  {"""
                    |{
                    |  "verse": {
                    |    "rows" : [
                    |      "I've lived a life that's full.",
                    |      "I've traveled each and every highway;",
                    |      "And more, much more than this,",
                    |      "I did it my way."
                    |    ]
                    |  }
                    |}
                    |""".stripMargin
                  }
                </pre>
                <p>
                  <b>Output JSON example:</b>
                </p>
                <pre>
                  {"""
                    |{
                    |  "verse": {
                    |    "rates": {
                    |      "rows": [
                    |        [ 0.75, 0.2, 0.789 ],
                    |        [ 1.75, 1.2, 1.789 ],
                    |        [ 2.75, 2.2, 2.789 ],
                    |        [ 3.75, 3.2, 3.789 ]
                    |      ],
                    |      "total": [
                    |        10.75, 10.2, 10.789
                    |      ]
                    |    }
                    |  }
                    |}
                    |""".stripMargin
                  }
                </pre>
                <p><b>Good luck.</b></p>
              </body>
            </html>
          }
        }
      } ~
      path(Rest) { s =>
        respondWithMediaType(`application/json`) { ctx =>
          ctx.complete(HttpResponse(StatusCodes.BadRequest, HttpEntity(ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`),
            writePretty(List(EM(EM.badRequest, Some("Illegal request URL")))))))
        }
      }
  }
}
