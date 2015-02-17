package controllers

import play.api._
import play.api.mvc._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.json.JsObject
import scala.concurrent.Future
import play.api.libs.json.Json
import play.autosource.reactivemongo.ReactiveMongoAutoSourceController
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import amlibs.core.router.RESTRouterContoller
import amlibs.core.daos.vendors.reactivemongo.RESTReactiveMongoController
import amlibs.core.router.RESTController
import amlibs.core.daos.vendors.reactivemongo.RESTReactiveMongoRouterContoller
import reactivemongo.bson.BSONObjectID
import doapi.models.traffic.TrafficModels.TrafficSpeedData
import doapi.models.traffic.TrafficModels.Formats._
import com.google.inject.Inject
import doapi.daos.TrafficSpeedDataDao
import doapi.daos.TrafficLinkDao
import amlibs.core.daos.JsonQueryHelper
import play.api.libs.json.JsArray
import java.util.Date
import scala.concurrent.duration.Duration

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def realtime = Action {
    Ok(views.html.realtime(""))
  }
}

object ReportAssets extends AssetsBuilder

class Persons extends RESTReactiveMongoController[JsObject] {
  val coll = db.collection[JSONCollection]("demo1")
}

object PersonsAPI extends RESTReactiveMongoRouterContoller {
  def resController = current.global.getControllerInstance(classOf[Persons]).asInstanceOf[RESTController[BSONObjectID]]
}

case class JsOutTrafficLink(
  linkId: String,
  startLat: Option[Double],
  startLng: Option[Double],
  endLat: Option[Double],
  endLng: Option[Double],
  startNode: String,
  endNode: String,
  startNodeEastings: Double,
  startNodeNorthings: Double,
  endNodeEastings: Double,
  endNodeNorthings: Double,
  region: String,
  roadType: String
  )

case class JsOutSpeedData(
  linkId: String,
  region: String,
  roadType: String,
  roadSaturationLevel: String,
  trafficSpeed: Int,
  captureDate: String)

object TrafficJsOutFormat {
  implicit val fmtJsOutTrafficLink = Json.format[JsOutTrafficLink]
  implicit val fmtJsOutSpeedData = Json.format[JsOutSpeedData]
}

class TrafficSpeedDataController @Inject() (linkDao: TrafficLinkDao, daoSpeedData: TrafficSpeedDataDao) extends RESTReactiveMongoController[TrafficSpeedData]() {

  val coll = db.collection[JSONCollection]("trafficspeeddata")

  import TrafficJsOutFormat._
  import JsonQueryHelper._
  def latest = Action.async { request =>
    val q = request.queryString.foldLeft(Json.obj())((acc, item) => acc ++ qEq(item._1, item._2.head.toString))
    val orderByDate = JsonQueryHelper.oDesc("captureDate")
    //    val orderByQuery = query(q) ++ JsonQueryHelper.orderBy(orderByDate)
    Logger.info(s"query: $q")
    for {
      list <- daoSpeedData.find(query(q) ++ JsonQueryHelper.orderBy(orderByDate), 617, 0)
      allLinkIds <- linkDao.cfind(Duration(1, "hour"))(Json.obj())
    } yield {
      if (list.size > 0) {

        val firstData = list.head._1.as[JsOutSpeedData].captureDate
        val optMd5 = (list.head._1 \ "md5").asOpt[String]
        Logger.info(s"optMd5 in the first item is: $optMd5 : ${list.head._1}")
        val jsList = list.filter(i => optMd5.map(md5 => (i._1 \ "md5").as[String] == md5) getOrElse (i._1.as[JsOutSpeedData].captureDate == firstData)).map { o =>
          val linkId = (o._1 \ "linkId").as[String]
          val linkIdContent = allLinkIds.filter(p => (p._1 \ "linkId").as[String] == linkId).head._1.as[JsOutTrafficLink]
          Json.toJson(o._1.as[JsOutSpeedData]).as[JsObject] ++ qEq("linkInfo", Json.toJson(linkIdContent))
        }
        Logger.info(s"got list count: ${jsList.size}")
        Ok(jsList.foldLeft(Json.arr())((acc, item) => acc.:+(item)))
      } else Ok(Json.arr())

    }
  }
}

object TrafficSpeedAPI extends RESTReactiveMongoRouterContoller {
  val controller = current.global.getControllerInstance(classOf[TrafficSpeedDataController])
  def resController = controller

  def latest = controller.latest
}

class TrafficLinkIdController @Inject() (linkDao: TrafficLinkDao) extends RESTReactiveMongoController[JsObject]() {

  import JsonQueryHelper._
  import play.api.libs.json._
  import play.api.libs.json.Reads._
  import play.api.libs.functional.syntax._

  val coll = db.collection[JSONCollection](linkDao.dbName)
  val outputTransform = (tPrune("_id") and
    (__ \ "geoJsonStart").json.copyFrom(tConcat("startLng", "startLat")) and
    (__ \ "geoJsonEnd").json.copyFrom(tConcat("endLng", "endLat"))) reduce

  def findByLinkId(linkId: String) = Action.async {
    linkDao.findFirstByLinkId(linkId).map {
      case Some(x) => Ok(x._1.transform(outputTransform).getOrElse(Json.obj()))
      case None    => NotFound
    }
  }

}

object TrafficLinkNodeAPI extends RESTReactiveMongoRouterContoller {
  val controller = current.global.getControllerInstance(classOf[TrafficLinkIdController])
  def resController = controller

  def findByLinkId(linkId: String) = controller.findByLinkId(linkId)
}
