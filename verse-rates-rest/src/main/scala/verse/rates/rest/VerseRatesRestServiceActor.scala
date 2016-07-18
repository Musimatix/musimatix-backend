package verse.rates.rest

import akka.actor.Actor
import org.json4s.JsonAST.{JString, JField, JObject}
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.writePretty
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import spray.http.HttpHeaders.RawHeader
import verse.rates.processor.{VerseResponses, WebFaceResponses$, VectorsProcessor}
import spray.http.MediaTypes._
import spray.http._
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import verse.rates.processor.VectorsProcessor.{ErrorMessage => EM}
import org.log4s._


class VerseRatesRestServiceActor (override val vectorsProcessor: VectorsProcessor)
  extends VerseRatesRestService with Actor {

  def actorRefFactory = context
  def receive = runRoute(ciRoute)
}

object VerseRatesRestService {
  val msgUnknown = "Unknown request: %s"

  val schemaPath = "/schema/"

  val songsResourceName = schemaPath + "frontend.songs.response.sample.json"
  val tagsResourceName = schemaPath + "frontend.tags.response.sample.json"
  val suggestTitleResourceName = schemaPath + "frontend.suggest.title.response.sample.json"

  val suggestLimit = 10

  implicit val json4sFormats = Serialization.formats(NoTypeHints)
}

abstract class VerseRatesRestService
  extends VerseResponses {

  import VerseRatesRestService._

  import language.postfixOps

  private[this] val logger = getLogger

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

                vectorsProcessor.invokeCalculator(sVerse) match {
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
      pathPrefix("songs") {
        pathPrefix("search") {
          path("similar") {
            respSimilar()
          } ~
          path("byid") {
            respSongs()
          } ~
          path("keywords") {
            respResourceExt(songsResourceName)
          } ~
          path("suggest_title") {
            respSuggestion()
          } ~
          path("presyllables") {
            respPresyllables()
          }
        } ~
        pathPrefix("env") {
          path("feedback") {
            respFeedback()
          } ~
          path("video_id") {
            respVideoId()
          }
        }
      }
    } ~
    get {
      pathPrefix("songs" / "env") {
        path("similar") {
          parameters("id".as[Int]) { id =>
            respJsonString { ctx =>
              val songs = vectorsProcessor.findSimilarSimple(id, 20)

              val songsIds = songs.map(_.id).mkString(", ")
              s"""{
                  |  "ids" : [$songsIds]
                  |}
                 """.stripMargin
            }
          }
        } ~
        path("suggest_title") {
          parameters("prefix", "limit".as[Int] ?) { (prefix, limit) =>
            respSuggestion(prefix, limit)
          }
        } ~
        path("tags") {
          parameters("lang" ?) { lang =>
            respTags(lang.getOrElse("eng"))
          }
        } ~
        path("auth") {
          parameters("email", "password" ?) { (email, pwd) =>
            respAuth(email, pwd)
          }
        } ~
        path("recognize") {
          parameters("session")(respRecognize)
        } ~
        path("video_id") {
          parameters("song_id".as[Int])(respVideoId)
        }
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
