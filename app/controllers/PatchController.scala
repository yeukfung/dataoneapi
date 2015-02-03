package controllers

import play.api.mvc.Controller
import play.api.mvc.Action
import doapi.daos.TrafficSpeedDataDao
import doapi.daos.TrafficSpeedDao
import com.google.inject.Inject
import amlibs.core.daos.JsonQueryHelper
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global

class PatchController @Inject() (speedDao: TrafficSpeedDao, speedDataDao: TrafficSpeedDataDao) extends Controller {

  
  /** patch the created date for all js in speeddata and speeddatadao **/
  import JsonQueryHelper._
  def patchCreatedDate = Action.async { request =>
    val q = qExists("created", false)
    for {
      le1 <- speedDao.batchUpdate(q, Json.obj("created" -> System.currentTimeMillis()))
      le2 <- speedDataDao.batchUpdate(q, Json.obj("created" -> System.currentTimeMillis()))
    } yield {
      Ok((le1.ok && le2.ok).toString)
    }
  }
}