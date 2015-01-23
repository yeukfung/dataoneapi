package amlibs.core.daos.vendors.reactivemongo

import scala.concurrent.{ ExecutionContext, Future }
import reactivemongo.bson.BSONObjectID
import reactivemongo.api.QueryOpts
import reactivemongo.core.commands.LastError
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.extensions._
import play.api.libs.iteratee.Enumerator
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import amlibs.core._
import scala.Left
import scala.Right

object `package` {
  implicit def BSONObjectIdBindable(implicit stringBinder: PathBindable[String]) =
    new PathBindable[BSONObjectID] {
      override def bind(key: String, value: String): Either[String, BSONObjectID] = {
        for {
          id <- stringBinder.bind(key, value).right
          bsonid <- BSONObjectID.parse(id).map(Right(_)).getOrElse(Left("Can't parse id")).right
        } yield bsonid
      }
      override def unbind(key: String, bsonid: BSONObjectID): String = {
        stringBinder.unbind(key, bsonid.toString)
      }
    }
}

class ReactiveMongoREST[T](coll: JSONCollection)(implicit format: Format[T]) extends RESTAPI[T, BSONObjectID, JsObject, JsObject, LastError] {
  override def insert(t: T)(implicit ctx: ExecutionContext): Future[BSONObjectID] = {
    val id = BSONObjectID.generate
    val obj = format.writes(t).as[JsObject]
    obj \ "_id" match {
      case _: JsUndefined =>
        coll.insert(obj ++ Json.obj("_id" -> id))
          .map { _ => id }

      case JsObject(Seq((_, JsString(oid)))) =>
        coll.insert(obj).map { _ => BSONObjectID(oid) }

      case JsString(oid) =>
        coll.insert(obj).map { _ => BSONObjectID(oid) }

      case f => sys.error(s"Could not parse _id field: $f")
    }
  }

  override def get(id: BSONObjectID)(implicit ctx: ExecutionContext): Future[Option[(T, BSONObjectID)]] = {
    coll.find(Json.obj("_id" -> id)).cursor[JsObject].headOption.map(_.map(js => (js.as[T], id)))
  }

  override def delete(id: BSONObjectID)(implicit ctx: ExecutionContext): Future[Unit] = {
    coll.remove(Json.obj("_id" -> id)).map(_ => ())
  }

  override def update(id: BSONObjectID, t: T)(implicit ctx: ExecutionContext): Future[Unit] = {
    coll.update(
      Json.obj("_id" -> id),
      Json.obj("$set" -> t)).map { _ => () }
  }

  override def updatePartial(id: BSONObjectID, upd: JsObject)(implicit ctx: ExecutionContext): Future[Unit] = {
    coll.update(
      Json.obj("_id" -> id),
      Json.obj("$set" -> upd)).map { _ => () }
  }

  override def batchInsert(elems: Enumerator[T])(implicit ctx: ExecutionContext): Future[LastError] = {
    val enum = elems.map { t =>
      val id = BSONObjectID.generate
      val obj = format.writes(t).as[JsObject]
      obj \ "_id" match {
        case _: JsUndefined => Json.obj("_id" -> id) ++ obj
        case _ => obj
      }
    }

    coll.bulkInsert(enum) map { nb =>
      LastError(true, None, None, None, None, 1, false)
    }
  }

  override def find(sel: JsObject, limit: Int = 0, skip: Int = 0)(implicit ctx: ExecutionContext): Future[Traversable[(T, BSONObjectID)]] = {
    val cursor = coll.find(sel).options(QueryOpts().skip(skip)).cursor[JsObject]
    val l = if (limit != 0) cursor.collect[Traversable](limit) else cursor.collect[Traversable]()
    l.map(_.map(js => (js.as[T], (js \ "_id").as[BSONObjectID])))
  }

  override def batchDelete(sel: JsObject)(implicit ctx: ExecutionContext): Future[LastError] = {
    coll.remove(sel)
  }

  override def batchUpdate(sel: JsObject, upd: JsObject)(implicit ctx: ExecutionContext): Future[LastError] = {
    coll.update(
      sel,
      upd,
      multi = true)
  }

}
