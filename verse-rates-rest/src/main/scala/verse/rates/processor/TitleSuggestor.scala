package verse.rates.processor

import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory
import com.googlecode.concurrenttrees.radix.{ConcurrentRadixTree, RadixTree}
import verse.rates.processor.VectorsProcessor.TitleBox

import scala.annotation.tailrec
import scala.collection.mutable
import TitleSuggestor._
import collection.JavaConverters._

import scala.util.Try

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
    cp.select(
      "SELECT id, title_rus FROM songs WHERE title_rus IS NOT NULL and vector IS NOT NULL"
    ).foreach { st =>
      val rs = st.executeQuery()
      Try {
        @tailrec
        def nextTitle(): Unit = {
          if (rs.next()) {
            val id = rs.getInt(1)
            Option(rs.getString(2)).foreach { title =>
              id2Title += id -> TitleBox(id, title)
              val words = title.split("[\\s.,!?:;()\"\'$%&\\[\\]\\{\\}]")
                .filter( w => w.nonEmpty && w != "-")
                .map(_.toLowerCase)

              words.headOption.foreach(w => addToMap(w, id, head2Ids))
              words.tail.foreach(w => addToMap(w, id, tail2Ids))
            }
            nextTitle()
          }
        }
        nextTitle()
      }
      rs.close()
      st.close()
    }

    head2Ids.foreach { case (word, ids) => treeHead.put(word, ids) }
    tail2Ids.foreach { case (word, ids) => treeTail.put(word, ids) }
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
