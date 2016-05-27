package verse.rates

import java.nio.ByteBuffer

import boopickle.Default._

object BooPickleBytes {

  case class YourClass (name : String, age : Int, dblSeq: Seq[Double])

  def main(args: Array[String]) {

    val obj = new YourClass("adbc", 67, Vector(1.6, 2.8, 3.9, 0.76765))

    val bpickled = Pickle.intoBytes(obj)

    val pickledBytes = new Array[Byte](bpickled.remaining)
    bpickled.get(pickledBytes)

    println(s"${pickledBytes.length}")

    val bufCopy = ByteBuffer.wrap(pickledBytes)

    val unbpickled = Unpickle[YourClass].fromBytes(bufCopy)

    println(unbpickled.name + ", " + unbpickled.age + ", " + unbpickled.dblSeq.mkString("#"))
  }

}
