package verse.rates.sandbox

import java.nio.ByteBuffer

import boopickle.Default._

object BooPickleBytesApp {
  import verse.rates.model.VerseMetrics._

  case class YourClass (name : String, age : Int, dblSeq: Seq[Double])

  def main(args: Array[String]) {

    val obj = new YourClass("adbc", 67, Vector(1.6, 2.8, 3.9, 0.76765))

    val bpickled = Pickle.intoBytes(obj)

    val pickledBytes = new Array[Byte](bpickled.remaining)
    bpickled.get(pickledBytes)

    println(s"${pickledBytes.length}")

    val bufCopy = ByteBuffer.wrap(pickledBytes)

    val unbpickled = Unpickle[YourClass].fromBytes(bufCopy)

    val ss = Vector(
      Syllable(1, 3, AccentStressed),
      Syllable(7, 2, AccentUnstressed),
      Syllable(10, 4, AccentAmbiguous),
      Syllable(17, 2, AccentStressed),
      Syllable(20, 3, AccentUnstressed)
    )

    val vv = Vector(1.6, 2.8, 3.9, 0.76765, -653.008)

    val ssArr = serializeSyllables(ss)
    val vvArr = serializeVerseVec(vv)

    val ss2 = deserializeSyllables(ssArr)
    val vv2 = deserializeVerseVec(vvArr)

    println(s"Syllables 0: $ss\nSyllables 1: $ss2\nVerseVec 0:$vv\nVerseVec 1:$vv2")

    println(unbpickled.name + ", " + unbpickled.age + ", " + unbpickled.dblSeq.mkString("#"))
  }

}
