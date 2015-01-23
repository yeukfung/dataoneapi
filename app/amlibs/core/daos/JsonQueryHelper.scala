package amlibs.core.daos

import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.JsString
import play.api.libs.json.JsNumber



object JsonQueryHelper {

  /** Json Parser **/
  private def jp(str: String) = {
    //        println(s"query String: $str")
    Json.parse(str).as[JsObject]
  }

  /** smart wrap to detect if double quote is required base on the content type **/
  private def smartWrap[T](fv: T) = fv match {
    case v: String => s""""$v""""
    case _ => fv
  }
  private def smartWrapJs[T](fv: T): JsValue = fv match {
    case v: String => JsString(v)
    case i: Int => JsNumber(i)
    case i: Long => JsNumber(i)
    case other @ _ => JsString(other.toString)
  }

  private def f2[T](fn: String, fv: T) = s"""{"$fn" : ${smartWrap(fv)} }""" //field 2
  private def f3[T](fn: String, fv: T, tag: String) = s"""{ "$fn" : { "$$$tag" : ${smartWrap(fv)} }}"""

  /** In **/
  def qIn[T](fn: String, fv: T*): JsObject = {
    val arr = fv.foldLeft(Json.arr())((acc, item) => acc :+ smartWrapJs(item))
    qEq(fn, qEq("$in", arr))
  }

  /** Equal **/
  def qEq[T](fn: String, fv: T): JsObject = jp(f2(fn, fv))

  /** Greater Than **/
  def qGt[T](fn: String, fv: T): JsObject = jp(f3(fn, fv, "gt"))

  /** Less Than **/
  def qLt[T](fn: String, fv: T): JsObject = jp(f3(fn, fv, "lt"))

  /** exist **/
  def qExists[T](fn: String, fv: T): JsObject = jp(f3(fn, fv, "exists"))

  /** or **/
  def qOr[T](objs: JsObject*): JsObject = {
    qEq("$or", objs.foldLeft(Json.arr())((acc, item) => acc :+ item.as[JsValue]))
  }

  /** and **/
  def qAnd[T](objs: JsObject*): JsObject = {
    qEq("$and", objs.foldLeft(Json.arr())((acc, item) => acc :+ item.as[JsValue]))
  }

  /** orders **/
  def oDesc(tag: String): JsObject = qEq(tag, -1)
  def oAsc(tag: String): JsObject = qEq(tag, 1)

  def orderBy(orderTags: JsObject*): JsObject = {
    val finalQ = orderTags.foldLeft(Json.obj())((acc, item) => acc ++ item)
    qEq("$orderby", finalQ)
  }
}
