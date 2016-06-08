package verse.rates.app

import java.sql.{Connection, DriverManager}

import com.typesafe.config.{Config, ConfigFactory}
import verse.rates.app.FabrikaImporter.SomeLimit

import scala.util.{Failure, Success, Try}

/** **
  *
  * ***/
object FabrikaImporterApp {

  val MYSQL_DRIVER_CLASS = "com.mysql.jdbc.Driver"
  val MYSQL_PREFIX = "jdbc:mysql://"
  val confRootKey = "verse.rates.rest"
  val confFabrikaKey = "fabrika.mysql"
  val confMsmxKey = "msmx.mysql"

  case class MySqlConnectionParameters(host: String, port: Int, user: String, password: String, schema: String)

  object MySqlConnectionParameters {
    def apply(conf: Config): Try[MySqlConnectionParameters] =
      for {
        host <- Try { conf.getString("host") }
        port <- Try { conf.getInt("port") }
        user <- Try { conf.getString("user") }
        password <- Try { conf.getString("password") }
        schema <- Try { conf.getString("schema") }
      } yield MySqlConnectionParameters(host, port, user, password, schema)
  }

  def mySqlConnection(p: MySqlConnectionParameters): Try[Connection] = {
    Try {
      Class.forName(MYSQL_DRIVER_CLASS)
      val cs = s"$MYSQL_PREFIX${p.host}:${p.port}/${p.schema}?user=${p.user}&password=${p.password}&useUnicode=true&characterEncoding=utf8"
      DriverManager.getConnection(cs)
    }
  }

  def mySqlConnection(key: String): Try[Connection] = {
    for {
      confRoot <- Try { ConfigFactory.load().getConfig(confRootKey) }
      conf <- Try { confRoot.getConfig(key) }
      p <- MySqlConnectionParameters(conf)
      c <- mySqlConnection(p)
    } yield c
  }

  def main(args: Array[String]) {
    println("Start importing")
    (for {
      conFabrika <- mySqlConnection(confFabrikaKey)
      conMsmx <- mySqlConnection(confMsmxKey)
    } yield conFabrika -> conMsmx) match {
      case Success((conFabrika, conMsmx)) =>
        val imp = new FabrikaImporter(conFabrika, conMsmx, SomeLimit(None, Some(10000)))
        imp.doImport()
//        imp.save()
        conFabrika.close()
        conMsmx.close()
      case Failure(f) =>
        println(f.getMessage)
    }
  }
}
