package verse.rates.env


object LogHub {
  var logInstance: Option[LogHub] = None

  def init(): Unit = {
    logInstance = Some(new LogHub)
  }

  def log(m: String): Unit = logInstance.foreach(_.log(m))
}

class LogHub {
  def log(m: String): Unit = {
    //
  }
}
