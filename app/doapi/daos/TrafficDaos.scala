package doapi.daos

import amlibs.core.daos.JsObjectDao
import scala.concurrent.Future
import doapi.models.ModelCommon.state
import scala.concurrent.ExecutionContext.Implicits._
import play.api.libs.json.JsObject
import amlibs.core.daos.JsonQueryHelper
import JsonQueryHelper._
import play.api.libs.json.Json
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType

class TrafficSpeedDao extends JsObjectDao {
  val dbName = "trafficspeeds"
}

class TrafficSpeedHeaderDao extends JsObjectDao {
  val dbName = "trafficspeedsheader"
}

class TrafficSpeedDataDao extends JsObjectDao {
  val dbName = "trafficspeeddata"

  coll.indexesManager.ensure(Index(key = Seq(("captureDate", IndexType.Descending))))
}

class TrafficLinkDao extends JsObjectDao {
  val dbName = "trafficlinks"

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

class TrafficSpeedDataCodeDao extends JsObjectDao {
  val dbName = "trafficcodes"

  def findFirstByCode(group: String, key: String) = {
    val q = qEq("group", group) ++ qEq("key", key)
    this.findFirst(q)
  }

  def findFirstCodeValue(group: String, key: String): Future[Option[String]] = {
    findFirstByCode(group, key).map {
      optItem => optItem.map { item => (item._1 \ "keyValue").as[String] }
    }
  }

  def saveCode(group: String, key: String, keyValue: String) = {
    val js = qEq("group", group) ++ qEq("key", key) ++ qEq("keyValue", keyValue)
    this.findFirstByCode(group, key) flatMap {
      case Some(x) => this.update(x._2, js).map { _ => x._2 }
      case None    => this.insert(js)
    }
  }

}