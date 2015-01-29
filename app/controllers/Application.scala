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

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

}

class Persons extends RESTReactiveMongoController[JsObject] {
  val coll = db.collection[JSONCollection]("demo1")
}

object PersonsAPI extends RESTReactiveMongoRouterContoller {
  def resController = current.global.getControllerInstance(classOf[Persons]).asInstanceOf[RESTController[BSONObjectID]]
}


class TrafficSpeedDataController extends RESTReactiveMongoController[TrafficSpeedData]() {
  val coll = db.collection[JSONCollection]("trafficspeeddata")
}

object TrafficSpeedAPI extends RESTReactiveMongoRouterContoller {
  def resController = current.global.getControllerInstance(classOf[TrafficSpeedDataController]).asInstanceOf[RESTController[BSONObjectID]]
}
