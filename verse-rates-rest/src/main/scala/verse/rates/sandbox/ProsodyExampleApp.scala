package verse.rates.sandbox

import java.io.File
import java.util

import org.apache.log4j.{Level, Logger}
import treeton.core.config.BasicConfiguration
import treeton.core.config.context.resources.LoggerLogListener
import treeton.core.config.context.{ContextConfiguration, ContextConfigurationSyntaxImpl}
import treeton.core.util.LoggerProgressListener
import treeton.prosody.musimatix.{VerseProcessingExample, VerseProcessor}

import scala.collection.JavaConverters._


/** **
  *
  * ***/
object ProsodyExampleApp {

  val stressRestrictionViolationWeight: Double = 0.4
  val reaccentuationRestrictionViolationWeight: Double = 0.6
  val spacePerMeter: Int = 10

  private val logger = Logger.getLogger(classOf[VerseProcessingExample])

  def main(args: Array[String]) {
    println("Go treeton")

    BasicConfiguration.setRootURL(new File("./third-party/treeton/conf/").toURI.toURL)
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

    Vector( // time
      "Ти моя остання любов,",
      "Моя машина, моя машина.",
      "Ти i я напилися знов,",
      "Моя єдина , смак бензина й кави...",
      "День i нiчь ,дихає час",
      "А ми з тобою живемо двое,",
      "Автомобiль буде у нас,",
      "Мое ти сонце"
    ).foreach(song.add)

//    Vector(
////      "Dmaj7~ Cmaj7~ Bmaj7~",
////      "[446564] [224342] [002120] [224242]",
//      "Зима ли или лето ли она всегда в зеленом",
//      "Она желанна ее зовут Марьяна",
//      "Она ходит по рукам ее хватит на всех",
//      "Даст то даст кто не даст пылью пыль смеху смех",
//      "Припев:",
//      "Марьяна, Марьяна не пла-а-а-ч-ч-ч-ч.",
//      "Марьяна, Марьяна не пла-а-а-ч-ч-ч-ч.",
//      "Тишина, тссс, тише, тишина, тссс.",
//      "Тишина, тссс, тише, тишина, тссс.",
//      "Тишина. Тссс,",
//      "Тишина.",
//      "Но оказалось я ее люблю не один",
//      "Mom and Diki, Mom and Dayman, Mom and Make вместе с ним",
//      "Корабли моей гавани меньше чем стаканы.",
//      "Она со мной, мысли вокруг – великаны.",
//      "Припев.",
//      "Я узнаю ее по запаху, где бы я бы не был.",
//      "Ею пахнет ветер, ею дышит небо.",
//      "Иногда я убегаю, но от нее не скрыться.",
//      "Я не птица, я не боюсь разбиться.",
//      "Припев.",
//      "Хорошо смеется тот, кто смеется последним.",
//      "Хорошо смеется тот, кто смеется.",
//      "Хорошо смеется тот кто.",
//      "Хорошо смеется тот.",
//      "Хорошо смеется хорошо-о-о хорошо-о-о",
//      "Хорошо-о хорошо-о хорошо-о-о-о-о-о-о",
//      "Е-е-е хорошо-о хорошо-о-о хорошо-о-о аааа",
//      "Ау еее"
//    ).foreach(song.add)

//    Vector(
//      "Вступление:",
//      "Мне приснился маленький дом возле теплого моря",
//      "И мы живем в этом доме без спешки, без боли и горя",
//      "Я проснулся с грустной улыбкой, как будто что-то случилось",
//      "Мятый разгром простыней - мне все просто приснилось.",
//      "Душно, я встал под душ, но не стало легче.",
//      "Июль - поджигатель душ, но мне стало проще.",
//      "Поверить во все, что не верил узнать, о чем боялся спросить",
//      "Холодные струи воды... Так хочется жить!",
//      "И в душной бетонной коробке можно быть просто счастливым,",
//      "&gt; 2 раза",
//      "И даже, наверное, нужно, иначе здесь невыносимо! /",
//      "Проигрыш:",
//      "Где-то намного хуже, но мы слишком устали",
//      "Помнить про каждую боль, о которой узнали",
//      "Моя жалость ничего не изменит, наши страхи никого не спасут",
//      "Не остановим новой беды. Жаль, но от нас и не ждут...",
//      "И значит в душной бетонной коробке мне можно быть просто счастливым,",
//      "И даже, наверное, нужно иначе здесь невыносимо!",
//      "В душной бетонной коробке...",
//      "Можно быть просто счастливым...",
//      "И даже, наверное, нужно...",
//      "Иначе здесь невыносимо!..",
//      "Мы живем в душных бетонных коробках",
//      "Иногда в голове сгорают все пробки",
//      "Бытовые проблемы, проблемы с деньгами",
//      "И рабочие дни, как танцы с волками",
//      "И кажется что кто-то другой виноват,",
//      "Но если ты такой умный - отчего не богат?",
//      "Ты хочешь все больше, а жизнь льет мимо",
//      "Ты просто забыл, как быть счастливым...",
//      "Кода: &gt; 4 раза"
//    ).foreach(song.add)

//
//    song.add("Мой дядя самых честных правил")
//    song.add("Когда не в шутку занемог")
//    song.add("Он уважать себя заставил")
//    song.add("И лучше выдумать не мог")
////    song.add("")
//    song.add("Его пример другим наука")
//    song.add("UYTU порпо uymbnm:")
//    song.add("     Когда я был там, я видел их - лошадей(пони и зебр) и рыбов: карасей и лещей")

    val start = System.nanoTime
    val verseDescriptions = processor.process(song).asScala
    val workingTime = System.nanoTime - start
    println(s"Time: ${workingTime / 1000000}ms")
    for (verseDescription <- verseDescriptions) {
      println(verseDescription)
    }

    processor.deinitialize()
  }

}
