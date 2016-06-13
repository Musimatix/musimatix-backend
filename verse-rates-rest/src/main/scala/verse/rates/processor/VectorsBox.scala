package verse.rates.processor

import java.sql.Connection

import com.typesafe.config.Config
import net.sf.javaml.core.kdtree.KDTree
import treeton.prosody.musimatix.VerseProcessor

object VectorsBox {

}

class VectorsBox(cfg: Config) {

  var metricsProcessor: Option[VectorsProcessor] = None

  val kd = new KDTree(90)

  locally {
    init()
  }

  def init(): Unit = {
  }



  def bye(): Unit = {
  }

}
