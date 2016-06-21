package verse.rates.processor

import java.io.File

import com.typesafe.config.Config
import org.apache.log4j.{Level, Logger}
import treeton.core.config.BasicConfiguration
import treeton.core.config.context.resources.LoggerLogListener
import treeton.core.config.context.{ContextConfigurationSyntaxImpl, ContextConfiguration}
import treeton.core.util.LoggerProgressListener
import treeton.prosody.musimatix.SyllableInfo.StressStatus
import treeton.prosody.musimatix.VerseProcessor
import verse.rates.model.MxSong
import verse.rates.model.VerseMetrics._

import scala.util.Try


/**
  * Created by ademin on 29.04.2016.
  */


object VectorsProcessor {

  type ErrorCode = Int

  type Rates = Seq[Double]
  type RatesSeq = Seq[Rates]

  case class VerseRates(rowsRates: RatesSeq, totalRates: Rates)

  object ErrorMessage {
    val ok = 0
    val emptyResponse = 404
    val badRequest    = 400
  }
  case class ErrorMessage(code: ErrorCode, message: Option[String])

  case class Author(id: Int, name: String)
  case class SongTag(id: Int, name: String)

  case class FullSong(id: Int, title: String, authors: Seq[Author],
    rowsPlain: Seq[String], syllables: Syllables, tags: Seq[SongTag],
    vector: VerseVec = Vector.empty[Double])

  case class TitleBox(id: Int, title: String)

  def createVerseProcessor(confTreeton: Config, logger: Logger): Option[VerseProcessor] = {
    val treetonDataPath = Try { confTreeton.getString("treeton.data.path") }.toOption
    val stressRestrictionViolationWeight = confTreeton.getDouble("stress.restriction.violation.weight")
    val reaccentuationRestrictionViolationWeight = confTreeton.getDouble("reaccentuation.restriction.violation.weight")
    val spacePerMeter = confTreeton.getInt("space.per.meter")
    val maxStressRestrictionViolations = confTreeton.getInt("max.stress.restriction.violations")
    val maxReaccentuationRestrictionViolations = confTreeton.getInt("max.reaccentuation.restriction.violations")
    val maxSyllablesPerVerse = confTreeton.getInt("max.syllables.per.verse")
    val metricGrammarPath = confTreeton.getString("metric.grammar.path")

    treetonDataPath.foreach { p =>
      BasicConfiguration.setRootURL(new File(p).toURI.toURL)
    }
    BasicConfiguration.createInstance()
    ContextConfiguration.registerConfigurationClass(classOf[ContextConfigurationSyntaxImpl])
    ContextConfiguration.createInstance()

    val processor = new VerseProcessor(metricGrammarPath, stressRestrictionViolationWeight,
      reaccentuationRestrictionViolationWeight, spacePerMeter, maxStressRestrictionViolations,
      maxReaccentuationRestrictionViolations, maxSyllablesPerVerse, false)
    processor.setProgressListener(new LoggerProgressListener("Musimatix", logger))
    processor.addLogListener(new LoggerLogListener(logger))
    processor.initialize()

    Some(processor)
  }

  def accentTypeForStress(ss: StressStatus): AccentType =
    ss match {
      case StressStatus.STRESSED   => AccentStressed
      case StressStatus.UNSTRESSED => AccentUnstressed
      case _ => AccentAmbiguous
    }
}

trait VectorsProcessor {
  import VectorsProcessor._

  def invokeCalculator(verse: String): Either[ErrorCode, VerseRates]

  def findSimilarSimple(id: Int, limit: Int): Seq[FullSong]

  def findSimilarSimple(rows: Seq[String], limit: Int): Seq[FullSong]

  def findSimilar(id: Int, limit: Int): Seq[MxSong]

  def findSimilar(rows: Seq[(String, Syllables)], limit: Int): Seq[MxSong]

  def suggest(s: String, limit: Int): Seq[TitleBox]

  def byid(ids: Seq[Int]): Seq[MxSong]

  def calcSyllables(rows: Seq[String]): Seq[(String, Syllables)]
}
