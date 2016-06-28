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
  val tail2Ids = mutable.Map.empty[String, IdsBucket]
  val head2Ids = mutable.Map.empty[String, IdsBucket]
  val id2Title = mutable.Map.empty[Int, TitleBox]

  val treeTail: RadixTree[Set[Int]] = new ConcurrentRadixTree[Set[Int]](new DefaultCharArrayNodeFactory)
  val treeHead: RadixTree[Set[Int]] = new ConcurrentRadixTree[Set[Int]](new DefaultCharArrayNodeFactory)

  locally {
    buildTree()
  }

  def addToMap(w: String, id: Int, map: mutable.Map[String, IdsBucket]): Unit = {
    val ids = map.getOrElse(w, Set.empty[Int])
    if (!ids.contains(id)) map += w -> (ids + id)
  }

  def buildTree(): Unit = {
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
              val words = title.split("[\\s.,!?:;()\"\'$%&\\[\\]\\{\\}]")
                .filter( w => w.nonEmpty && w != "-")
                .map(_.toLowerCase)

              words.headOption.foreach(w => addToMap(w, id, head2Ids))
              if(words.size > 1) words.tail.foreach(w => addToMap(w, id, tail2Ids))
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

    println(s"Suggests: id2Title:${id2Title.size} head2Ids:${head2Ids.size} tail2Ids:${tail2Ids.size}")
  }

  def suggest(s: String, limit: Int): Seq[TitleBox] = {
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
