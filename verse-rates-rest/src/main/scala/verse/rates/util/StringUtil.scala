package verse.rates.util


object StringUtil {

  def isCyrillic(c: Char): Boolean =
    Character.UnicodeBlock.of(c).equals(Character.UnicodeBlock.CYRILLIC)

}
