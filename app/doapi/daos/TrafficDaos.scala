package doapi.daos

import amlibs.core.daos.JsObjectDao
import scala.concurrent.Future
import doapi.models.ModelCommon.state
import scala.concurrent.ExecutionContext.Implicits._
import play.api.libs.json.JsObject

class TrafficSpeedDao extends JsObjectDao {

  val dbName = "trafficspeeds"

}

class TrafficSpeedDataDao extends JsObjectDao {
  val dbName = "trafficspeeddata"
}

class TrafficLinkDao extends JsObjectDao {
  val dbName = "trafficlinks"
}

class CoordinateInfoDao extends JsObjectDao {
  val dbName = "coordinateinfos"
}