package amlibs.core.utils

import play.api.libs.json._

object JsFlattener {

  val removeId = (__ \ "_id").json.prune

  def apply(js: JsValue): JsObject = flatten(js.transform(removeId).get).foldLeft(JsObject(Nil))(_ ++ _.as[JsObject])

  def flatten(js: JsValue, prefix: String = ""): Seq[JsValue] = {
    js.as[JsObject].fieldSet.toSeq.flatMap {
      case (key, values) =>
        values match {
          case JsBoolean(x) => Seq(Json.obj(concat(prefix, key) -> x))
          case JsNumber(x)  => Seq(Json.obj(concat(prefix, key) -> x))
          case JsString(x)  => Seq(Json.obj(concat(prefix, key) -> x))
          case JsArray(seq) => Seq(Json.obj(concat(prefix, key) -> JsArray(seq)))
          case x: JsObject  => flatten(x, concat(prefix, key))
          case _            => Seq(Json.obj(concat(prefix, key) -> JsNull))
        }
    }
  }

  def concat(prefix: String, key: String): String = if (prefix.nonEmpty) s"${prefix}${key.capitalize}" else key

}