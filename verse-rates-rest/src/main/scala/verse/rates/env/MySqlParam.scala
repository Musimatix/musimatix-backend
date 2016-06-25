package verse.rates.env

import java.sql.{Connection, DriverManager}

import com.typesafe.config.Config

import scala.util.Try

/** **
  *
  * ***/

case class MySqlParam(host: String, port: Int, user: String, password: String, schema: String) {
  import MySqlParam._
  def connect(): Try[Connection] =
    Try {
      Class.forName(MYSQL_DRIVER_CLASS)
      val cs = s"$MYSQL_PREFIX$host:$port/$schema?user=$user&password=$password&useUnicode=true&characterEncoding=utf8"
      DriverManager.getConnection(cs)
    }
}

object MySqlParam {
  val MYSQL_DRIVER_CLASS = "com.mysql.jdbc.Driver"
  val MYSQL_PREFIX = "jdbc:mysql://"

  def apply(conf: Config): Try[MySqlParam] =
    for {
      host <- Try { conf.getString("host") }
      port <- Try { conf.getInt("port") }
      user <- Try { conf.getString("user") }
      password <- Try { conf.getString("password") }
      schema <- Try { conf.getString("schema") }
    } yield MySqlParam(host, port, user, password, schema)

}
