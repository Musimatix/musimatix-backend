package verse.rates


package object model {
  case class MxAuthor(id: Int, nameRus: Option[String], nameEng: Option[String])

  case class MxGroup(id: Int, nameRus: Option[String], nameEng: Option[String])

  case class MxTag(id: Int, nameRus: String, nameEng: String)

  case class MxSong(id: Int, titleRus: Option[String], titleEng: Option[String],
    text: String, tags: Seq[MxTag], group: Option[MxGroup], authors: Seq[MxAuthor])
}
