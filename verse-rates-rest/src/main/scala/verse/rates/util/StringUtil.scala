package verse.rates.util


object StringUtil {

  def isCyrillic(c: Char): Boolean =
    Character.UnicodeBlock.of(c).equals(Character.UnicodeBlock.CYRILLIC)

  def failureMessage(trw: Throwable): String = {
    val msg = Option(trw.getMessage).fold("")(" " + _)
    s"[${trw.getClass.getCanonicalName}]$msg"
  }
}
