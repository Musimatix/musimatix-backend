package verse.rates.app

import java.util

import org.apache.log4j.{Logger, Level}
import treeton.core.config.BasicConfiguration
import treeton.core.config.context.resources.LoggerLogListener
import treeton.core.config.context.{ContextConfigurationSyntaxImpl, ContextConfiguration}
import treeton.core.util.LoggerProgressListener
import treeton.prosody.musimatix.{VerseProcessingExample, VerseProcessor}
import scala.collection.JavaConverters._


/** **
  *
  * ***/
object TreetonExampleApp {

  val stressRestrictionViolationWeight: Double = 0.4
  val reaccentuationRestrictionViolationWeight: Double = 0.6
  val spacePerMeter: Int = 10

  private val logger = Logger.getLogger(classOf[VerseProcessingExample])

  def main(args: Array[String]) {
    println("Go treeton")

    BasicConfiguration.createInstance()
    ContextConfiguration.registerConfigurationClass(classOf[ContextConfigurationSyntaxImpl])
    ContextConfiguration.createInstance()
    Logger.getRootLogger.setLevel(Level.INFO)

    val metricGrammarPath = args(0)
    val processor = new VerseProcessor(metricGrammarPath, stressRestrictionViolationWeight, reaccentuationRestrictionViolationWeight, spacePerMeter)
    processor.setProgressListener(new LoggerProgressListener("Musimatix", logger))
    processor.addLogListener(new LoggerLogListener(logger))
    processor.initialize()

    val song = new util.ArrayList[String]()
    song.add("Мой дядя самых честных правил")
    song.add("Когда не в шутку занемог")
    song.add("Он уважать себя заставил")
    song.add("И лучше выдумать не мог")

    val verseDescriptions = processor.process(song).asScala
    for (verseDescription <- verseDescriptions) {
      println(verseDescription)
    }

    processor.deinitialize()
  }

}
