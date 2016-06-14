package verse.rates

import verse.rates.model.VerseMetrics.{VerseVec, Syllables}


package object model {
  case class MxAuthor(id: Int, nameRus: Option[String], nameEng: Option[String])

  case class MxGroup(id: Int, nameRus: Option[String], nameEng: Option[String])

  case class MxTag(id: Int, nameRus: Option[String], nameEng: Option[String])

  case class MxSong(id: Int, titleRus: Option[String], titleEng: Option[String],
    rowsPlain: Seq[String], rowsAccents: Seq[Syllables], rowsVectors: Seq[VerseVec],
    vec: Option[VerseVec], tags: Seq[MxTag], group: Option[MxGroup], authors: Seq[MxAuthor])
}
