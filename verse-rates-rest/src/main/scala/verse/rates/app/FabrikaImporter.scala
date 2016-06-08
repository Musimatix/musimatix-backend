package verse.rates.app

import java.nio.ByteBuffer
import java.sql.{Statement, ResultSet, PreparedStatement, Connection}
import boopickle.Default._

import scala.annotation.tailrec
import scala.util.Try
import enumeratum._
import FabrikaImporter._


object FabrikaImporter {


  sealed trait MxTag extends EnumEntry

  object MxTag extends Enum[MxTag] {
    val values = findValues
    case object Eng extends MxTag
    case object Rus extends MxTag
  }

  // authors - ids in old DB
  case class Composition(oldId: Int, title: String, text: String, lang: MxTag, authors: Seq[Int])

  // oldId - id in old DB
  // newId - id in new DB after import
  case class Author(oldId: Int, name: String, newId: Option[Int])

  trait Limit {
    val rus: Option[Int]
    val eng: Option[Int]
  }
  case object NoLimit extends Limit {
    val rus: Option[Int] = None
    val eng: Option[Int] = None
  }
  case class SomeLimit(rus: Option[Int], eng: Option[Int]) extends Limit
}

class FabrikaImporter(val conSource: Connection, val conTarget: Connection, limit: Limit = NoLimit) {

  def statement(s: String, con: Connection) = con.prepareStatement(s.stripMargin.replaceAll("\n", " "))
  def update(s: String, con: Connection) = con.prepareStatement(s.stripMargin.replaceAll("\n", " "), Statement.RETURN_GENERATED_KEYS)

  // composition id -> authorIds (in old DB)
  def getComposition2Author: Map[Int, Seq[Int]] = {
    val builder = Vector.newBuilder[(Int, Int)]
    val st = statement("SELECT author_id, composition_id FROM author_composition", conSource)
    val rs = st.executeQuery()
    Try {
      @tailrec
      def nextPair(): Unit = {
        if (rs.next()) {
          builder += rs.getInt(1) -> rs.getInt(2)
          nextPair()
        }
      }
      nextPair()
    }
    rs.close()
    st.close()

    val pairs = builder.result()
    val grouped = pairs
      .groupBy(_._2)
      .mapValues { v => v.map(_._1) }
    grouped
  }

  def getAuthors: Map[Int, Author] = {
    val builder = Map.newBuilder[Int, Author]
    val st = statement("SELECT * FROM author", conSource)
    val rs = st.executeQuery()
    Try {
      @tailrec
      def nextAuthor(): Unit = {
        if (rs.next()) {
          val id = rs.getInt(1)
          builder += id -> Author(id, rs.getString(2), None)
          nextAuthor()
        }
      }
      nextAuthor()
    }
    rs.close()
    st.close()
    builder.result()
  }

  def isCyrillic(c: Char): Boolean =
    Character.UnicodeBlock.of(c).equals(Character.UnicodeBlock.CYRILLIC)

  def checkLang(text: String): MxTag =
    if (text.view.take(100).exists(isCyrillic)) MxTag.Rus
    else MxTag.Eng

  def getCompositions(c2a: Int => Seq[Int]): Seq[Composition] = {
    val builder = Vector.newBuilder[Composition]
    val st = statement("SELECT id, title, text_clean FROM composition", conSource)
    val rs = st.executeQuery()
    Try {
      @tailrec
      def nextComposition(): Unit = {
        if (rs.next()) {
          val id = rs.getInt(1)
          val title = rs.getString(2)
          val text = rs.getString(3)
          val lang = checkLang(text)

          builder += Composition(id, title, text, lang, c2a(id))
          nextComposition()
        }
      }
      nextComposition()
    }
    rs.close()
    st.close()
    builder.result()
  }

  def saveAuthors(authors: Map[Int, Author]): Map[Int, Author] = {
    def save(a: Author): Option[Int] = {
      val st = update("INSERT INTO authors (name_rus, name_eng, old_id) VALUES (?, ?, ?)", conTarget)
      val lang = checkLang(a.name)
      val (s, n) =  if (lang == MxTag.Eng) (2, 1) else (1, 2)
      st.setString(s, a.name)
      st.setNull(n, java.sql.Types.VARCHAR)
      st.setInt(3, a.oldId)
      val keysCount = st.executeUpdate()
      val rsKeys = st.getGeneratedKeys
      val key: Option[Int] = if(rsKeys.next()) Some(rsKeys.getInt(1)) else None
      rsKeys.close()
      key
    }

    val sz = authors.size
    var i = 0
    for((_, author) <- authors) yield {
      val id = save(author)
      if (i % 100 == 0) {
        println(s"$i of ${authors.size}")
      }
      i += 1
      author.oldId -> author.copy(newId = id)
    }
  }

  def saveCompositions(compositions: Seq[Composition]) = {
    def save(c: Composition): Unit = {
      val st = update("INSERT INTO songs (title_rus, title_eng, plain, old_id) VALUES (?, ?, ?, ?)", conTarget)
      val lang = checkLang(c.title)
      val (s, n) =  if (lang == MxTag.Eng) (2, 1) else (1, 2)
      st.setString(s, c.title)
      st.setNull(n, java.sql.Types.VARCHAR)
      st.setString(3, c.text)
      st.setInt(4, c.oldId)
      st.executeUpdate()
      val rsKeys = st.getGeneratedKeys
      val songIdOpt = if(rsKeys.next()) Some(rsKeys.getInt(1)) else None
      rsKeys.close()
      st.close()

      songIdOpt.foreach { songId =>
        c.authors.foreach { authorId =>
          val stAuthors = update(s"INSERT INTO song_author (song_id, author_id) VALUES ($songId, $authorId)", conTarget)
          stAuthors.executeUpdate()
          stAuthors.close()
        }
        val tagId = if (c.lang == MxTag.Eng) 1 else 2
        val stTags = update(s"INSERT INTO tagged (song_id, tag_id) VALUES ($songId, $tagId)", conTarget)
        stTags.executeUpdate()
        stTags.close()
      }
    }

    for((c, i) <- compositions.zipWithIndex) {
      if (i % 100 == 0) {
        println(s"-- $i of ${compositions.size}")
      }
      save(c)
    }
  }

  def doImport(): Unit = {
    val authors = getAuthors
    val composition2authors = getComposition2Author.withDefaultValue(Seq.empty[Int])
    val savedAuthors = saveAuthors(authors)

    def mapAuthors(authors: Seq[Int]): Seq[Int] = {
      authors.flatMap { oldId =>
        savedAuthors.get(oldId).flatMap(_.newId)
      }
    }

    def authorsForComposition(id: Int): Seq[Int] = {
      mapAuthors(composition2authors(id))
    }

    val compositions = getCompositions(authorsForComposition)
    val (rusCompositions, engCompositions) = compositions.partition(_.lang == MxTag.Rus)
    val groupedCompositions =
      Vector(
        (rusCompositions, limit.rus),
        (engCompositions, limit.eng)
      ).map { case (cc, lm) =>
        lm match {
          case Some(n) => cc.take(n)
          case _ => cc
        }
      }

    groupedCompositions.foreach(saveCompositions)

    println(s"authors: ${authors.size} c2a: ${composition2authors.size} cr: ${rusCompositions.size} ce: ${engCompositions.size}")
  }

  case class YourClass (name : String, age : Int, dblSeq: Seq[Double])

  def save(): Unit = {
//    val st = update("INSERT INTO songs2 (title_rus, title_eng, vector) VALUES (?,'орпоро',?)", conTarget)
//
//    val obj = new YourClass("adbc", 67, Vector(1.6, 2.8, 3.9, 0.76765))
//
//    val bpickled = Pickle.intoBytes(obj)
//    val pickledBytes = new Array[Byte](bpickled.remaining)
//    bpickled.get(pickledBytes)
//
//    st.setString(1, "Привет")
////    st.setString(2, "Bye")
//    st.setBytes(2, pickledBytes)
//
//    val num = st.executeUpdate()
//    val rsKeys = st.getGeneratedKeys
//    while(rsKeys.next()) {
//      println(s"inserted with: ${rsKeys.getInt(1)}")
//    }
//
//    rsKeys.close()
//    st.close()
//
//    val st2 = statement("SELECT id, title_rus, title_eng, vector FROM songs2", conTarget)
//    val rs2 = st2.executeQuery()
//
//    while (rs2.next()) {
//      val id = rs2.getInt(1)
//      val title_rus = rs2.getString(2)
//      val title_eng = rs2.getString(3)
//      val vecBytes = rs2.getBytes(4)
//      val vecBuf = ByteBuffer.wrap(vecBytes)
//      val unbpickled = Unpickle[YourClass].fromBytes(vecBuf)
//
//      println(s"$id : $title_rus : $title_eng")
//      println(unbpickled.name + ", " + unbpickled.age + ", " + unbpickled.dblSeq.mkString("#"))
//
//    }
//
//    rs2.close()
//    st2.close()
  }
}
