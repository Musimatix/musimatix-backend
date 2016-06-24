package verse.rates.app

import java.io.ByteArrayInputStream
import java.sql.{PreparedStatement, Statement, Connection}
import java.util

import com.typesafe.config.{ConfigFactory, Config}
import org.apache.log4j.{Level, Logger}
import treeton.core.config.BasicConfiguration
import treeton.core.config.context.resources.LoggerLogListener
import treeton.core.config.context.{ContextConfigurationSyntaxImpl, ContextConfiguration}
import treeton.core.util.LoggerProgressListener
import treeton.prosody.musimatix.SyllableInfo.StressStatus
import treeton.prosody.musimatix.{SyllableInfo, VerseProcessor, VerseProcessingExample}
import verse.rates.app.ConfigHelper._
import verse.rates.app.VectorsUpdater.Song
import verse.rates.model.VerseMetrics._
import verse.rates.processor.{VectorsProcessor, TitleSuggestor, ConnectionProvider}
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}
import verse.rates.model.VerseMetrics
import verse.rates.model.VerseMetrics._
import verse.rates.util.StringUtil._
import collection.JavaConverters._
import VectorsProcessor._

object VectorsUpdater {
  case class Song(id: Int, text: String)
}

class VectorsUpdater(start: Option[Int] = None) {

  private[this] var verseProcessor: Option[VerseProcessor] = None
  private[this] var connectionProvider: Option[ConnectionProvider] = None

  private val logger = Logger.getLogger(classOf[VerseProcessingExample])

  locally {
    init()
  }

  def init(): Unit = {
    (for {
      confRoot <- Try { ConfigFactory.load().getConfig(confRootKey) }
      confMsmx <- Try { confRoot.getConfig(confMsmxKey) }
      confTreeton <- Try { confRoot.getConfig(confTreetonKey) }
    } yield (confMsmx, confTreeton)) match {
      case Success((confMsmx, confTreeton)) =>
        verseProcessor = VectorsProcessor.createVerseProcessor(confTreeton, logger)
        connectionProvider = Some(new ConnectionProvider(confMsmx))
      case Failure(f) =>
        println(f.getMessage)
    }

    verseProcessor.foreach { p =>
      val dim = p.getMetricVectorDimension
      println(s"Vector dimension: $dim")
    }
  }

  def rowMetrics(s: String): Try[VerseMetrics] = {
    Failure(new IllegalArgumentException("Void result"))
  }

  def getSongs: Seq[Song] = {
    val builder = Vector.newBuilder[Song]
    // get russian songs
    for (
      conn <- connectionProvider;
      st <- conn.select("SELECT s.id, s.plain FROM songs s, tagged t WHERE s.id = t.song_id AND t.tag_id = 2")
    ) {
      val rs = st.executeQuery()
      Try {
        @tailrec
        def nextSong(): Unit = {
          if (rs.next()) {
            val id = rs.getInt(1)
            builder += Song(id, rs.getString(2))
            nextSong()
          }
        }
        nextSong()
      }
      rs.close()
      st.close()
    }
    builder.result()
  }

  def saveCalculatedRows(id: Int, calculatedRows: Seq[(Int, String, Option[VerseMetrics])]): Unit = {
    def setBlobValue(st: PreparedStatement, field: Int, bOpt: Option[Array[Byte]]): Unit = {
      bOpt match {
        case Some(b) =>
          st.setBlob(field, new ByteArrayInputStream(b))
        case _ =>
          st.setNull(field, java.sql.Types.BLOB)
      }
    }

    for (conn <- connectionProvider) {
      calculatedRows.foreach { case (idx, row, vmOpt) =>
        val vv = vmOpt.map( vm => serializeVerseVec(vm.vec) )
        val sv = vmOpt.map( vm => serializeSyllables(vm.syl) )
        conn.update("INSERT INTO rows (song_id, idx, plain, accents, vector) VALUES (?, ?, ?, ?, ?)")
          .foreach { st =>
            st.setInt(1, id)
            st.setInt(2, idx)
            st.setString(3, row)
            setBlobValue(st, 4, sv)
            setBlobValue(st, 5, vv)
            val keysCount = st.executeUpdate()
            st.close()
          }
      }
      val vectors = calculatedRows.flatMap { case (_, _, vmOpt) => vmOpt.map(_.vec) }

      def sumVec(vec1: VerseVec, vec2: VerseVec): VerseVec = {
        vec1.zip(vec2).map { case (d1, d2) => d1 + d2 }
      }
      val songVec = vectors
        .reduceOption(sumVec)
        .map ( vec => vec.map(_ / vectors.size))
      val songVecBytes = songVec.map(serializeVerseVec)
      conn.update("UPDATE songs SET vector=? WHERE id=?")
        .foreach { st =>
          setBlobValue(st, 1, songVecBytes)
          st.setInt(2, id)
          val updated = st.executeUpdate()
          st.close()
        }
    }
  }

  def processSong(song: Song): Unit = {
    verseProcessor.foreach { p =>
      val rows = song.text
        .split("\n")
        .flatMap { r =>
          val rNorm = r.replace("\r", "").reverse.dropWhile(_.isSpaceChar).reverse
          if (rNorm.exists(_.isLetter) || rNorm.isEmpty) {
            Some(rNorm)
          } else None
        }

      val withIndex = rows.zipWithIndex.toVector
      val filteredForProcessing = withIndex.filter { case (r, _) => r.exists(isCyrillic) }
      if (filteredForProcessing.nonEmpty) {
        val javaCollection = filteredForProcessing.map(_._1).asJava
        Try { p.process(javaCollection).asScala } match {
          case Success(metricsArray) =>
            val index2Metrics = filteredForProcessing
              .zip(metricsArray)
              .map { case ((_, i), m) => i -> m }
              .toMap
            val calculatedRows = withIndex.map { case (r, i) =>
              val vmOpt = index2Metrics.get(i).map { m =>
                val vv = m.metricVector.asScala.toVector
                  .map {
                    case d: java.lang.Double => Double.unbox(d)
                    case _ => 0.0
                  }
                val sv = m.syllables.asScala.toVector
                  .flatMap {
                    case si: SyllableInfo =>
                      Some(Syllable(si.startOffset, si.length, accentTypeForStress(si.stressStatus)))
                    case _ => None
                  }
                VerseMetrics(vv, sv)
              }
              (i, r, vmOpt)
            }
            saveCalculatedRows(song.id, calculatedRows)

          case Failure(f) =>
            println(s"\nFailure: ${f.getClass} (${f.getMessage}). Song: ${song.id}")
        }
      }
    }
  }

  def updateTables(): Unit = {
    val songs = getSongs.sortBy(_.id)

    val part = start match {
      case Some(i) => songs.dropWhile(_.id < i)
      case _ => songs
    }

    part.zipWithIndex.foreach { case (song, i) =>
      val start = System.nanoTime
      processSong(song)
      val time = (System.nanoTime - start) / 1000000L
      print(".")
      if (time > 15000L) print(s"[${song.id}:${time}ms]")
      if (i % 50 == 0) println(s" : $i (id: ${song.id})")
    }

    println(s"songs:${songs.size}")
  }

  def bye(): Unit = {
    verseProcessor.foreach(_.deinitialize())
    connectionProvider.foreach(_.bye())
  }
}
