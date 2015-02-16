package controllers

import play.api.mvc.Action
import play.api.mvc.Controller
import amlibs.core.playspecific.PlayMixin
import scala.concurrent.Future

case class KibanaConvertData(src: String)

trait StringReplaceMixin extends PlayMixin {
  import collection.JavaConversions._

  /**
   * values should be a -> b in string
   */

  def getList(confKey: String) = conf.getStringList(confKey).map(_.toList).getOrElse(List())

  def getMap(confKey: String) = {

    val values = getList(confKey)

    values.foldLeft(Map[String, String]())((acc, item) => {
      val arr = item.split("->")
      if (arr.length == 2) {
        acc + (arr(0).trim() -> arr(1).trim())
      } else acc
    })
  }

  def replaceWithConfigKey(src: String, confKey: String) = {

    val map = getMap(confKey)

    map.foldLeft(src)((acc, kv) => {
      acc.replaceAll(kv._1, kv._2)
    })
  }

}

object ESTools extends Controller with StringReplaceMixin {
  import play.api.data._
  import play.api.data.Forms._

  val convertForm = Form(
    mapping(
      "src" -> text)(KibanaConvertData.apply)(KibanaConvertData.unapply))

  def index = Action {
    Ok(views.html.estoolindex("title", convertForm))
  }

  def convert = Action { implicit request =>
    convertForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest("err")
      },
      data => {

        val confKey = "kibana.v3.1.2.readyonlyconvert"
        val src = data.src
        val result = replaceWithConfigKey(src, confKey)

        Ok(result)

      })
  }
}