package verse.rates.processor

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.http.{HttpTransport, HttpRequest, HttpRequestInitializer}
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.{SearchResult, SearchListResponse}
import collection.JavaConverters._
import scala.util.Try


class YoutubeSearch {

  // Define a global instance of the HTTP transport.
  val httpTransport = new NetHttpTransport

  // Define a global instance of the JSON factory.
  val jsonFactory = new JacksonFactory

  val apiKey: String = "AIzaSyD3UzkrAeLtCLNGFWb34cvVecrhlJC0ZcE"

  val videoReturned = 8

  // This object is used to make YouTube Data API requests. The last
  // argument is required, but since we don't need anything
  // initialized when the HttpRequest is initialized, we override
  // the interface and provide a no-op function.
  val youtube = new YouTube.Builder(httpTransport, jsonFactory,
    new HttpRequestInitializer() {
      def initialize(request: HttpRequest) {
    }
  }).setApplicationName("youtube-cmdline-search-sample").build

  def search(s: String): Option[String] = {
    Try {
      val search = youtube.search.list("id,snippet")

      // Set your developer key from the Google Developers Console for
      // non-authenticated requests. See:
      // https://console.developers.google.com/
      search.setKey(apiKey)
      search.setQ(s)

      // Restrict the search results to only include videos. See:
      // https://developers.google.com/youtube/v3/docs/search/list#type
      search.setType("video")

      // To increase efficiency, only retrieve the fields that the
      // application uses.
      search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)")
      search.setMaxResults(java.lang.Long.valueOf(videoReturned))
      val searchResponse = search.execute

      val searchResultList = searchResponse.getItems.asScala.toVector

      searchResultList.headOption.map(_.getId.getVideoId)
    }.toOption.flatten
  }
}
