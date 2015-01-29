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
import amlibs.core.router.RESTController
import play.modules.reactivemongo.MongoController
import amlibs.core.router.RESTRouterContoller

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
  val FIND_MAX_LIMIT = 100

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
        case _              => obj
      }
    }

    coll.bulkInsert(enum) map { nb =>
      LastError(true, None, None, None, None, 1, false)
    }
  }

  override def find(sel: JsObject, limit: Int = 0, skip: Int = 0)(implicit ctx: ExecutionContext): Future[Traversable[(T, BSONObjectID)]] = {
    val cursor = coll.find(sel).options(QueryOpts().skip(skip)).cursor[JsObject]
    val l = if (limit > 0 && limit <= FIND_MAX_LIMIT) cursor.collect[Traversable](limit) else cursor.collect[Traversable](FIND_MAX_LIMIT)
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

abstract class RESTReactiveMongoRouterContoller extends RESTRouterContoller[BSONObjectID]

abstract class RESTReactiveMongoController[T](implicit ctx: ExecutionContext, format: Format[T])
  extends RESTController[BSONObjectID]
  with MongoController {

  import play.api.libs.iteratee.{ Enumerator, Done, Input }

  def coll: JSONCollection

  /**
   * Override this to customize how JsErrors are reported.
   * The implementation should call onBadRequest
   */
  protected def onJsError(request: RequestHeader)(jsError: JsError): Future[SimpleResult] =
    onBadRequest(request, JsError.toFlatJson(jsError).toString)

  /** Override to customize deserialization and add validation. */
  protected val reader: Reads[T] = format
  /** Override to customize serialization. */
  protected val writer: Writes[T] = format

  lazy val res = new ReactiveMongoREST[T](coll)(Format(reader, writer))

  /** Override to cutomize deserialization of queries. */
  protected val queryReader: Reads[JsObject] = implicitly[Reads[JsObject]]

  /** Override to cutomize deserialization of updates. */
  protected val updateReader: Reads[JsObject] = implicitly[Reads[JsObject]]

  /** Override to cutomize deserialization of queries and batch updates. */
  protected val batchReader: Reads[(JsObject, JsObject)] = (
    (__ \ "query").read(queryReader) and
    (__ \ "update").read(updateReader)).tupled

  private implicit val writerWithId = Writes[(T, BSONObjectID)] {
    case (t, id) =>
      val ser = writer.writes(t).as[JsObject].updateAllKeyNodes {
        case (_ \ "_id", value) => ("id" -> value \ "$oid")
      }
      if ((__ \ "id")(ser).isEmpty) ser.as[JsObject] ++ Json.obj("id" -> id.stringify)
      else ser
  }
  private implicit val idWriter = Writes[BSONObjectID] { id =>
    Json.obj("id" -> id.stringify)
  }

  private def bodyReader[A](reader: Reads[A]): BodyParser[A] =
    BodyParser("ReactiveMongoAutoSourceController body reader") { request =>
      parse.json(request) mapM {
        case Right(jsValue) =>
          jsValue.validate(reader) map { a =>
            Future.successful(Right(a))
          } recoverTotal { jsError =>
            onJsError(request)(jsError) map Left.apply
          }
        case left_simpleResult =>
          Future.successful(left_simpleResult.asInstanceOf[Either[SimpleResult, A]])
      }
    }

  override def insert =
    insertAction.async(bodyReader(reader)) { request =>
      res.insert(request.body) map { id =>
        Ok(Json.toJson(id))
      }
    }

  override def get(id: BSONObjectID) =
    getAction.async {
      res.get(id) map {
        case None      => NotFound(s"ID ${id.stringify} not found")
        case Some(tid) => Ok(Json.toJson(tid))
      }
    }

  override def delete(id: BSONObjectID) =
    deleteAction.async {
      res.delete(id) map { _ => Ok(Json.toJson(id)) }
    }

  override def update(id: BSONObjectID) =
    updateAction.async(bodyReader(reader)) { request =>
      res.update(id, request.body) map { _ => Ok(Json.toJson(id)) }
    }

  override def updatePartial(id: BSONObjectID) =
    updateAction.async(bodyReader(updateReader)) { request =>
      res.updatePartial(id, request.body) map { _ => Ok(Json.toJson(id)) }
    }

  override def batchInsert =
    insertAction.async(bodyReader(Reads.seq(reader))) { request =>
      res.batchInsert(Enumerator.enumerate(request.body)) map { lasterror =>
        Ok(Json.obj("nb" -> lasterror.updated))
      }
    }

  private def requestParser[A](reader: Reads[A], default: A): BodyParser[A] =
    BodyParser("ReactiveMongoAutoSourceController request parser") { request =>
      request.queryString.get("q") match {
        case None =>
          if (request.contentType.exists(m => m.equalsIgnoreCase("text/json")
            || m.equalsIgnoreCase("application/json")))
            bodyReader(reader)(request)
          else
            Done(Right(default), Input.Empty)
        case Some(Seq(str)) =>
          parse.empty(request) mapM { _ =>
            try {
              Json.parse(str).validate(reader) map { a =>
                Future.successful(Right(a))
              } recoverTotal { jsError =>
                onJsError(request)(jsError) map Left.apply
              }
            } catch {
              // catch exceptions from Json.parse
              case ex: java.io.IOException =>
                onBadRequest(request, "Expecting Json value for query parameter 'q'!") map Left.apply
            }
          }
        case Some(seq) =>
          parse.empty(request) mapM { _ =>
            onBadRequest(request, "Expecting single value for query parameter 'q'!") map Left.apply
          }
      }
    }

  private def extractQueryStringInt(request: RequestHeader, param: String): Int =
    request.queryString.get(param) match {
      case Some(Seq(str)) =>
        try { str.toInt } catch { case ex: NumberFormatException => 0 }
      case _ => 0
    }

  override def find =
    getAction.async(requestParser(queryReader, Json.obj())) { request =>
      val query = request.body
      val limit = extractQueryStringInt(request, "limit")
      val skip = extractQueryStringInt(request, "skip")

      res.find(query, limit, skip) map { s =>
        Ok(Json.toJson(s))
      }
    }

  override def batchDelete =
    deleteAction.async(requestParser(queryReader, Json.obj())) { request =>
      val query = request.body
      res.batchDelete(query) map { lasterror => Ok(Json.obj("nb" -> lasterror.updated)) }
    }

  override def batchUpdate =
    updateAction.async(requestParser(batchReader, Json.obj() -> Json.obj())) { request =>
      val (q, upd) = request.body
      res.batchUpdate(q, upd) map { lasterror => Ok(Json.obj("nb" -> lasterror.updated)) }
    }

}

