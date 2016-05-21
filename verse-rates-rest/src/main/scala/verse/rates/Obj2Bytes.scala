package verse.rates

import org.msgpack.annotation.Message
import org.msgpack.ScalaMessagePack

object Obj2Bytes {

  @Message // Don't forget to add Message annotation.
  class YourClass{
    var name : String = ""
    var age : Int = 2
  }

  def main(args: Array[String]) {

    val obj = new YourClass()
    obj.name = "hoge"
    obj.age = 22
    val serialized : Array[Byte] = ScalaMessagePack.write(obj)

    val deserialized : YourClass = ScalaMessagePack.read[YourClass](serialized)

    println(deserialized)
    println(deserialized.name + ", " + deserialized.age)
  }


}
