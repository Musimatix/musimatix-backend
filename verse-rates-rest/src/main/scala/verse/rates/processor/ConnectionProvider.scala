package verse.rates.processor

import java.sql.{PreparedStatement, Statement, Connection}

import com.typesafe.config.Config
import verse.rates.app.MySqlParam
import verse.rates.env.LogHub

import scala.util.{Failure, Success}


object ConnectionProvider {
  val msgIllegalConfig = "Config parsing failed."
  val msgConnectionFailed = "Error while connectiong."

  var connectionProviderInstance: Option[ConnectionProvider] = None

  def init(cfg: Config): Unit = {
    connectionProviderInstance = Some(new ConnectionProvider(cfg)) // ConnectionProvider will make connection
  }

  def connection(): Option[Connection] =
    connectionProviderInstance.flatMap(_.ensureConnected().connection())
}

class ConnectionProvider(val cfg: Config) {
  import ConnectionProvider._

  private[this] var mySqlParam: Option[MySqlParam] = None
  private[this] var conn: Option[Connection] = None

  locally {
    init()
  }

  def failureMsg(f: Throwable): String = {
    val msg = Option(f.getMessage).fold("")(m => s" [$m]")
    s"${f.getClass.getCanonicalName}$msg"
  }

  def connection(): Option[Connection] = conn

  private[this] def init(): Unit = {
    mySqlParam = MySqlParam(cfg) match {
      case Success(p) => Some(p)
      case Failure(f) =>
        LogHub.log(s"$msgIllegalConfig ${failureMsg(f)}")
        None
    }
    reconnect()
  }

  private[this] def stripMultiline(s: String): String =
    s.stripMargin.replaceAll("\n", " ")

  def select(s: String): Option[PreparedStatement] =
    conn.map(_.prepareStatement(stripMultiline(s)))

  def update(s: String): Option[PreparedStatement] =
    conn.map(_.prepareStatement(stripMultiline(s), Statement.RETURN_GENERATED_KEYS))


  def reconnect(): Unit = {
    mySqlParam.foreach { p =>
      conn = p.connect() match {
        case Success(c) => Some(c)
        case Failure(f) =>
          LogHub.log(s"$msgConnectionFailed ${failureMsg(f)}")
          None
      }
    }
  }

  def ensureConnected(): ConnectionProvider = {
    if (conn.isEmpty) reconnect()
    this
  }

  def bye(): Unit = {
    conn.foreach(_.close())
    conn = None
  }

}
