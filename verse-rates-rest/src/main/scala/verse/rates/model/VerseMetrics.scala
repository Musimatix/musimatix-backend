package verse.rates.model

import java.nio.ByteBuffer
import boopickle.Default._
import VerseMetrics._
import enumeratum.{Enum, EnumEntry}

case class VerseMetrics(vec: VerseVec, syl: Syllables)

object VerseMetrics {

  sealed trait LangTag extends EnumEntry

  object LangTag extends Enum[LangTag] {
    val values = findValues
    case object Eng extends LangTag
    case object Rus extends LangTag
  }

  sealed trait AccentType
  case object AccentStressed extends AccentType
  case object AccentUnstressed extends AccentType
  case object AccentAmbiguous extends AccentType

  case class Syllable(pos : Int, len : Int, accent: AccentType)
  type Syllables = Seq[Syllable]
  type VerseVec = Seq[Double]

  def serializeSyllables(ss: Syllables): Array[Byte] = {
    val bbuf = Pickle.intoBytes(ss)
    val barr = new Array[Byte](bbuf.remaining)
    bbuf.get(barr)
    barr
  }

  def deserializeSyllables(barr: Array[Byte]): Syllables = {
    Unpickle[Syllables].fromBytes(ByteBuffer.wrap(barr))
  }

  def serializeVerseVec(vv: VerseVec): Array[Byte] = {
    val bbuf = Pickle.intoBytes(vv)
    val barr = new Array[Byte](bbuf.remaining)
    bbuf.get(barr)
    barr
  }

  def deserializeVerseVec(barr: Array[Byte]): VerseVec = {
    Unpickle[VerseVec].fromBytes(ByteBuffer.wrap(barr))
  }
}
