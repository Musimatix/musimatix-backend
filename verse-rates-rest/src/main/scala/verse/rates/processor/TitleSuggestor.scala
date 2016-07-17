package verse.rates.processor

import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory
import com.googlecode.concurrenttrees.radix.{ConcurrentRadixTree, RadixTree}
import verse.rates.processor.VectorsProcessor.TitleBox

import scala.annotation.tailrec
import scala.collection.mutable
import TitleSuggestor._
import collection.JavaConverters._

import scala.util.{Failure, Success, Try}

object TitleSuggestor {
  type IdsBucket = Set[Int]
}

class TitleSuggestor(val cp: ConnectionProvider) {

  val id2Title = mutable.Map.empty[Int, TitleBox]

  val treeTail: RadixTree[Set[Int]] = new ConcurrentRadixTree[Set[Int]](new DefaultCharArrayNodeFactory)
  val treeHead: RadixTree[Set[Int]] = new ConcurrentRadixTree[Set[Int]](new DefaultCharArrayNodeFactory)
  val treeAll: RadixTree[Set[Int]] = new ConcurrentRadixTree[Set[Int]](new DefaultCharArrayNodeFactory)

  val splitRegex = "[\\s.,!?:;()\"\'$%&\\[\\]\\{\\}]+"

  locally {
    buildTree()
  }

  def addToMap(w: String, id: Int, map: mutable.Map[String, IdsBucket]): Unit = {
    val ids = map.getOrElse(w, Set.empty[Int])
    if (!ids.contains(id)) map += w -> (ids + id)
  }

  def buildTree(): Unit = {

    val tail2Ids = mutable.Map.empty[String, IdsBucket]
    val head2Ids = mutable.Map.empty[String, IdsBucket]
    val all2Ids = mutable.Map.empty[String, IdsBucket]

    //    cp.select("SELECT s.id, s.title_rus, s.title_eng FROM songs s").foreach { st =>
    cp.select(
      """SELECT s.id, s.title_rus, s.title_eng, a.id, a.name_rus, a.name_eng
        |FROM songs s
        |LEFT OUTER JOIN song_author sa ON sa.song_id = s.id
        |LEFT OUTER JOIN authors a ON sa.author_id = a.id
        |WHERE s.vector IS NOT NULL
      """.stripMargin).foreach { st =>

      val rs = st.executeQuery()
      Try {
        @tailrec
        def nextTitle(): Unit = {
          if (rs.next()) {
            val id = rs.getInt(1)
            val titleOpt = Option(rs.getString(2)).orElse(Option(rs.getString(3)))
//            val author = Option.empty[String]
            val author = Option(rs.getString(5)).orElse(Option(rs.getString(6)))
            titleOpt.foreach { title =>
              id2Title += id -> TitleBox(id, title, author.map(_.trim))
              val words = title.split(splitRegex)
                .filter( w => w.nonEmpty && w != "-")
                .map(_.toLowerCase)

              words.headOption.foreach(w => addToMap(w, id, head2Ids))
              if(words.length > 1) words.tail.foreach(w => addToMap(w, id, tail2Ids))
              words.foreach(w => addToMap(w, id, all2Ids))
            }
            nextTitle()
          }
        }
        nextTitle()
      } match {
        case Success(_) =>
        case Failure(f) =>
          f.printStackTrace()
      }
      rs.close()
      st.close()
    }

    head2Ids.foreach { case (word, ids) => treeHead.put(word, ids) }
    tail2Ids.foreach { case (word, ids) => treeTail.put(word, ids) }
    all2Ids.foreach { case (word, ids) => treeAll.put(word, ids) }

    println(s"Suggests: id2Title:${id2Title.size} head2Ids:${head2Ids.size} tail2Ids:${tail2Ids.size}")
  }

  def suggest(s: String, limit: Int): Seq[TitleBox] = {
    suggestByAllWords(s.toLowerCase, limit)
  }

  case class SongIdWithIndices(id: Int, found: IndexedSeq[Int])

  private[this] def suggestByAllWords(s: String, limit: Int): Seq[TitleBox] = {

    def idsForWord(w: String): Set[Int] = {
      val builder = Set.newBuilder[Int]
      treeAll
        .getValuesForKeysStartingWith(w).asScala
        .foreach(ids => ids.foreach(id => builder += id))
      builder.result()
    }

    def substituteFrom(s: String, words: IndexedSeq[String], idx: Int): Option[Int] = {
      words.view.zipWithIndex
        .drop(idx)
        .find { case (w, i) => w.startsWith(s) }
        .map(_._2)
    }

    val words = s.split("\\s+").toVector
    if (words.nonEmpty) {
      var idsInitial = idsForWord(words.head)

      val idsIntersect = words.foldLeft(idsInitial) { case (ids, w) =>
        ids.intersect(idsForWord(w))
      }

      val songs = idsIntersect.flatMap { id =>
        val tb = id2Title(id)
        val splittedTitle = tb.title.split(splitRegex)
          .filterNot(_.isEmpty).map(_.toLowerCase)

        @tailrec
        def subst(wi: Int, from: Int, indices: IndexedSeq[Int]): IndexedSeq[Int] = {
          if (wi < words.size) {
            val found = substituteFrom(words(wi), splittedTitle, from)
            found match {
              case Some(i) => subst(wi + 1, i + 1, indices :+ i)
              case _ => Vector.empty[Int]
            }
          } else
            indices
        }

        val indices = subst(0, 0, Vector.empty[Int])

        if (indices.nonEmpty) Some(SongIdWithIndices(id, indices))
        else None
      }

      val intSeqOrdering: Ordering[IndexedSeq[Int]] = new Ordering[IndexedSeq[Int]] {
        override def compare(x: IndexedSeq[Int], y: IndexedSeq[Int]): Int = {
          val z = x.zipAll(y, -1, -1)
          @tailrec
          def cmp(i: Int): Int = {
            if (z.isDefinedAt(i)) {
              val (v1, v2) = z(i)
              math.signum(v1 - v2) match {
                case 0 => cmp(i + 1)
                case sg => sg
              }
            } else 0
          }
          cmp(0)
        }
      }

      songs.toVector.sortBy(_.found)(intSeqOrdering).map(sng => id2Title(sng.id))
    } else
      Vector.empty[TitleBox]
  }

  private[this] def suggestByOneWord(s: String, limit: Int): Seq[TitleBox] = {
    var idsSet = Set.empty[Int]
    var idsVec = Vector.empty[Int]
    def addIds(tree: RadixTree[Set[Int]]): Unit = {
      val buckets = tree.getValuesForKeysStartingWith(s.toLowerCase).asScala
      buckets.takeWhile { b =>
        if (idsVec.size < limit) {
          b.foreach { id =>
            if (!idsSet.contains(id)) {
              idsSet += id
              idsVec :+= id
            }
          }
        }
        idsVec.size < limit
      }
    }
    addIds(treeHead)
    if (idsVec.size < limit) addIds(treeTail)
    idsVec.take(limit).map(id2Title)
  }

}
