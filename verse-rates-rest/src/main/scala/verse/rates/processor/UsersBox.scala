package verse.rates.processor

import java.sql.Timestamp
import java.util.Calendar

import com.fasterxml.uuid.Generators
import verse.rates.model.MxUser
import scala.collection.concurrent.TrieMap


object UsersBox {
  val singlePassword = "needlyrics"
}

class UsersBox(cp: ConnectionProvider) {
  import UsersBox._

  val sessions = TrieMap.empty[String, MxUser]

  def register(email: String, pwd: Option[String]): Option[(MxUser, String)] = {
    if (pwd.contains(singlePassword)) {
      val userSess = cp.select("SELECT id, email, name FROM users WHERE email = ?")
        .flatMap { st =>
          st.setString(1, email)
          val rs = st.executeQuery()
          val usr = if (rs.next()) {
            val user = MxUser(rs.getInt(1), email, Option(rs.getString(3)))
            val session = Generators.randomBasedGenerator.generate
              .toString.replaceAll("-", "")
            cp.update("INSERT INTO visits (user_id,   visit_time) VALUES (?, ?)")
              .foreach { st =>
                val timestamp = new Timestamp(Calendar.getInstance.getTimeInMillis)
                st.setInt(1, user.id)
                st.setTimestamp(2, timestamp)
                st.executeUpdate()
                st.close()
              }
            Some(user -> session)
          } else None
          st.close()
          usr
        }
      userSess match {
        case Some((user, session)) =>
          sessions += session -> user
        case _ =>
          cp.update("INSERT INTO newbies (email, visit_time) VALUES (?, ?)")
            .foreach { st =>
              val timestamp = new Timestamp(Calendar.getInstance.getTimeInMillis)
              st.setString(1, email)
              st.setTimestamp(2, timestamp)
              st.executeUpdate()
              st.close()
            }
      }
      userSess
    } else None
  }

  def checkSession(session: String): Option[MxUser] =
    sessions.get(session)
}
