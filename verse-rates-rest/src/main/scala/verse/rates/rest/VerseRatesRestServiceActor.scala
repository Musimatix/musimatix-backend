package verse.rates.rest

import akka.actor.Actor
import org.json4s.JsonAST.{JString, JField, JObject}
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.writePretty
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import spray.http.HttpHeaders.RawHeader
import verse.rates.processor.VectorsProcessor
import spray.http.MediaTypes._
import spray.http._
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.routing._
import scala.io.Source
import verse.rates.processor.VectorsProcessor.{ErrorMessage => EM, TitleBox}


class VerseRatesRestServiceActor (override val vectorsProcessor: VectorsProcessor)
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

  val suggestLimit = 10

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

  def vectorsProcessor: VectorsProcessor

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

  def respSuggestion() =
    respondWithMediaType(`application/json`) {
      respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) { ctx =>
        val jsonBody = ctx.request.entity.asString
        val json = parse(jsonBody) \ "suggestTitle"
        val titles = (json \ "keywords", json \  "limit") match {
          case (JString(s), JInt(l)) => vectorsProcessor.suggest(s, l.toInt)
          case (JString(s), JNothing) => vectorsProcessor.suggest(s, suggestLimit)
          case _ => Seq.empty[TitleBox]
        }

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

        ctx.complete(HttpResponse(StatusCodes.OK,
          HttpEntity(ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`),
            writePretty(rootObj)
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
          respSuggestion()
        }
      }
    } ~
    get {
      pathPrefix("songs" / "env") {
        path("similar") {
          parameters("id".as[Int]) { id =>
            respondWithMediaType(`application/json`) {
              respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
                val songs = vectorsProcessor.findSimilar(id, 20)

                val songsIds = songs.map(_.id).mkString(", ")
                val s =
                  s"""{
                      |  "ids" : [$songsIds]
                      |}
                   """.stripMargin

                complete(HttpResponse(StatusCodes.OK,
                  HttpEntity(ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`),
                  s
                )))
              }
            }
          }
        } ~
        path("suggest_title") {
          parameters("prefix", "limit".as[Int] ?) { (prefix, limit) =>
            respondWithMediaType(`application/json`) {
              respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
                val titles = vectorsProcessor.suggest(prefix, limit.getOrElse(suggestLimit))
                  .map { tb => "\"" + tb.title + "\"" }

                val s =
                  s"""{
                     |  "titles" : [
                     |${titles.mkString(",\n")}
                     |  ]
                     |}
                   """.stripMargin

                complete(HttpResponse(StatusCodes.OK,
                  HttpEntity(ContentType(MediaTypes.`application/json`, HttpCharsets.`UTF-8`),
                  s
                )))
              }
            }
          }
        } ~
        path("tags") {
          respResourceExt(tagsResourceName)
        }      } ~
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
