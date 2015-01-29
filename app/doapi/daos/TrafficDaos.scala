package doapi.daos

import amlibs.core.daos.JsObjectDao
import scala.concurrent.Future
import doapi.models.ModelCommon.state
import scala.concurrent.ExecutionContext.Implicits._
import play.api.libs.json.JsObject
import amlibs.core.daos.JsonQueryHelper

class TrafficSpeedDao extends JsObjectDao {
  val dbName = "trafficspeeds"
}

class TrafficSpeedDataDao extends JsObjectDao {
  val dbName = "trafficspeeddata"
}

class TrafficLinkDao extends JsObjectDao {
  val dbName = "trafficlinks"

  import JsonQueryHelper._

  def findFirstByLinkId(linkId: String) = {
    this.findFirst(qEq("linkId", linkId))
  }
}

class TrafficLinkMetaDao extends TrafficLinkDao {
  override val dbName = "trafficlinksmeta"
}


class CoordinateInfoDao extends JsObjectDao {
  val dbName = "coordinateinfos"
}