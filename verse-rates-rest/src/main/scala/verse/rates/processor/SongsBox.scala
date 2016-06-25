package verse.rates.processor

import java.math.MathContext

import org.apache.commons.io.IOUtils
import org.json4s.JsonAST._
import verse.rates.model.VerseMetrics._
import verse.rates.model.{MxAuthor, MxTag, MxSong}

import scala.annotation.tailrec
import ConnectionProvider._

object SongsBox {

  def tag2Json(tag: MxTag, lang: LangTag): JObject = {
    val name = (lang match {
      case LangTag.Eng => tag.nameEng
      case _ => tag.nameRus
    }).getOrElse("NA")
    JObject(
      JField("id", JInt(tag.id)),
      JField("name", JString(name))
    )
  }

  def author2Json(author: MxAuthor, lang: LangTag): JObject = {
    val name = (lang match {
      case LangTag.Eng => author.nameEng.orElse(author.nameRus)
      case _ => author.nameRus.orElse(author.nameEng)
    }).getOrElse("NA")
    JObject(
      JField("id", JInt(author.id)),
      JField("name", JString(name))
    )
  }

  def song2Json(song: MxSong, lang: LangTag): JObject = {
    val title = (lang match {
      case LangTag.Eng => song.titleEng.orElse(song.titleRus)
      case _ => song.titleRus.orElse(song.titleEng)
    }).getOrElse("Undefined")
    val group = song.authors.headOption.flatMap { author =>
      lang match {
        case LangTag.Eng => author.nameEng.orElse(author.nameRus)
        case _ => author.nameRus.orElse(author.nameEng)
      }
    }

    var fields = List(
      JField("id", JInt(song.id))
    )
    group.foreach(g => fields :+= JField("group", JString(g)))
    if (song.authors.nonEmpty) {
      fields :+= JField("authors", JArray(
        song.authors.map(a => author2Json(a, lang)).toList))
    }
    fields :+= JField("title", JString(title))
    if (song.rowsPlain.nonEmpty) {
      fields :+= JField("rowsPlain", JArray(
        song.rowsPlain.map(r => JString(r)).toList))

      val adjust = math.max(0, song.rowsPlain.size - song.rowsAccents.size)
      val accents = song.rowsAccents ++ Vector.fill(adjust)(Seq.empty[Syllable])

      val tagged = song.rowsPlain.zip(accents)
        .map { case (r, ra) =>
          ra.filter(_.accent == AccentStressed)
            .foldLeft((r, 0)) { case ((row, inserted), acc) =>
              val left = acc.pos + inserted
              val right = left + acc.len
              (row.substring(0, left) + "{" + row.substring(left, right) + "}" + row.substring(right), inserted + 2)
            }
        }
        .map(p => JString(p._1))
        .toList

      fields :+= JField("rowsTagged", JArray(tagged))
    }
    if (song.tags.nonEmpty) {
      fields :+= JField("tags", JArray(
        song.tags.map(a => tag2Json(a, lang)).toList))
    }
    song.similarity.foreach { sim =>
      fields :+= JField("similarity", JDecimal(BigDecimal(sim, new MathContext(2, java.math.RoundingMode.HALF_UP))))
    }
    JObject(fields)
  }
}

class SongsBox(val cp: ConnectionProvider) {

  var tagToSongs = Map.empty[Int, Set[Int]]

  locally {
    buildTagToSongs()
  }

  def filterByTags(ids: Seq[Int], tags: Seq[Int]): Seq[Int] = {
    if (tags.isEmpty) ids
    else {
      val allFiltered = tags.view.map(tagToSongs).reduce((s1, s2) => s1.intersect(s2))
      ids.filter(allFiltered.contains)
    }
  }

  def buildTagToSongs(): Unit = {
    cp.select("SELECT song_id, tag_id FROM tagged").foreach { st =>
      val rs = st.executeQuery()

      @tailrec
      def nextPair(): Unit = {
        if (rs.next()) {
          val song_id = rs.getInt(1)
          val tag_id = rs.getInt(2)

          tagToSongs += tag_id -> (
            tagToSongs.get(tag_id) match {
              case Some(songs) => songs + song_id
              case _ => Set(song_id)
            })

          nextPair()
        }
      }
      nextPair()

      rs.close()
      st.close()
    }
    tagToSongs = tagToSongs.withDefaultValue(Set.empty[Int])
  }

  def getSongsByIds(ids: Seq[Int]): Seq[MxSong] = {
    ids.flatMap { id =>
      val song = cp.select(
        """|SELECT s.id song_id, s.title_rus title_rus, s.title_eng title_eng,
           |a.id author_id, a.name_rus author_name_rus, a.name_eng author_name_eng,
           |t.id tag_id, t.name_rus tag_name_rus, t.name_eng tag_name_eng,
           |s.vector vec
           |FROM songs s
           |LEFT OUTER JOIN song_author sa ON sa.song_id = s.id
           |LEFT OUTER JOIN authors a ON sa.author_id = a.id
           |LEFT OUTER JOIN tagged st ON st.song_id = s.id
           |LEFT OUTER JOIN tags t ON st.tag_id = t.id
           |WHERE s.id = ?
        """.stripMargin).flatMap { st =>
        st.setInt(1, id)
        implicit val rs = st.executeQuery()
        var authorsMap = Map.empty[Int, MxAuthor]
        var tagsMap = Map.empty[Int, MxTag]
        var song = Option.empty[MxSong]

        @tailrec
        def nextSongRec(): Unit = {
          if (rs.next()) {
            if (song.isEmpty) {

              song = Some(MxSong(
                rs.getInt(1),
                Option(rs.getString(2)),
                Option(rs.getString(3)),
                Seq.empty[String],
                Seq.empty[Syllables],
                Seq.empty[VerseVec],
                Option(rs.getBlob(10))
                  .map(blob => deserializeVerseVec(IOUtils.toByteArray(blob.getBinaryStream))),
                Seq.empty[MxTag],
                None,
                Seq.empty[MxAuthor],
                None
              ))
            }

            val author = getIntOpt(4)
              .map(id => MxAuthor(id, Option(rs.getString(5)), Option(rs.getString(6))))
            val tag = getIntOpt(7)
              .map(id => MxTag(id, Option(rs.getString(8)), Option(rs.getString(9))))
            author.foreach(a => authorsMap += a.id -> a)
            tag.foreach(t => tagsMap += t.id -> t)

            nextSongRec()
          }
        }
        nextSongRec()

        rs.close()
        st.close()
        song.map(_.copy(authors = authorsMap.values.toVector, tags = tagsMap.values.toVector))
      }

      song.map { sg =>
        var rowsPlainVec = Vector.empty[String]
        var rowsAccentsVec = Vector.empty[Syllables]
        var rowsVectorsVec = Vector.empty[VerseVec]

        cp.select("SELECT plain, accents, vector, idx FROM rows WHERE song_id = ? ORDER BY idx")
          .foreach { st =>
            st.setInt(1, id)
            val rs = st.executeQuery()

            @tailrec
            def nextRow(): Unit = {
              if (rs.next()) {
                rowsPlainVec :+= Option(rs.getString(1)).getOrElse("")

                rowsAccentsVec :+= Option(rs.getBlob(2))
                  .map( blob => deserializeSyllables(IOUtils.toByteArray(blob.getBinaryStream)) )
                  .getOrElse(Seq.empty[Syllable])

                rowsVectorsVec :+= Option(rs.getBlob(3))
                  .map( blob => deserializeVerseVec(IOUtils.toByteArray(blob.getBinaryStream)) )
                  .getOrElse(Seq.empty[Double])

                nextRow()
              }
            }
            nextRow()
          }
        sg.copy(rowsPlain = rowsPlainVec, rowsAccents = rowsAccentsVec, rowsVectors = rowsVectorsVec)
      }
    }
  }
}

