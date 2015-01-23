package amlibs.core.daos

import play.api.libs.json._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.Play
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.bson.BSONObjectID
import reactivemongo.core.commands.LastError
import play.api.libs.iteratee.Enumerator
import reactivemongo.api.QueryOpts
import amlibs.core.RESTAPI
import amlibs.core.daos.vendors.reactivemongo.ReactiveMongoREST
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat

trait ReactiveMongoDao[T] extends RESTAPI[T, String, JsObject, JsObject, LastError]

trait DaoUtils {
  import JsonQueryHelper._
}

trait JsObjectDao extends ReactiveMongoDao[JsObject] {
  //abstract members
  def dbName: String

  /** abstract members **/
  implicit lazy val app = Play.current

  object JsObjectFormat extends Format[JsObject] {
    def reads(json: JsValue) = json.validate[JsObject]
    def writes(json: JsObject) = json
  }

  /** Returns the current instance of the driver. */
  def driver = ReactiveMongoPlugin.driver
  /** Returns the current MongoConnection instance (the connection pool manager). */
  def connection = ReactiveMongoPlugin.connection
  /** Returns the default database (as specified in `application.conf`). */
  def db = ReactiveMongoPlugin.db

  def coll = db.collection[JSONCollection](dbName)

  def format: Format[JsObject] = JsObjectFormat
  // for override purpose
  protected val reader: Reads[JsObject] = format
  protected val writer: Writes[JsObject] = format

  lazy val res = new ReactiveMongoREST[JsObject](coll)(Format(reader, writer))

  /** useful writer and reader **/

  //  private implicit val idWriter = Writes[BSONObjectID] { id =>
  //    Json.obj("id" -> id.stringify)
  //  }

  private implicit def stringToBSONObjectId(str: String) = BSONObjectID(str)

  /** impls **/

  import play.modules.reactivemongo.json.BSONFormats._
  val removeBSONObjectID = (__ \ "_id").json.prune

  private def bsonIdToId(json: JsObject): JsObject = {
    json \ "_id" match {
      case js =>
        val id = (js.as[BSONObjectID]).stringify

        json.transform(removeBSONObjectID) map {
          jsObj => jsObj ++ Json.obj("id" -> id)
        } getOrElse (json)

      case _ => json
    }
  }

  override def insert(obj: JsObject)(implicit ctx: ExecutionContext): Future[String] = {
    val id = BSONObjectID.generate
    obj \ "id" match {
      case _: JsUndefined =>
        coll.insert(obj ++ Json.obj("_id" -> id))
          .map { _ => id.stringify }
      case js: JsValue =>
        update(js.as[String], obj) map {
          case _ =>
            js.as[String].replace("\"", "")
        }
    }
  }

  override def get(id: String)(implicit ctx: ExecutionContext): Future[Option[(JsObject, String)]] = {
    coll.find(Json.obj("_id" -> id)).cursor[JsObject].headOption.map(_.map(js => (js, id)))
  }

  override def delete(id: String)(implicit ctx: ExecutionContext): Future[Unit] = {
    coll.remove(Json.obj("_id" -> BSONObjectID(id))).map(_ => ())
  }

  override def update(id: String, t: JsObject)(implicit ctx: ExecutionContext): Future[Unit] = {
    coll.update(
      Json.obj("_id" -> BSONObjectID(id)),
      Json.obj("$set" -> t)).map { _ => () }
  }

  override def updatePartial(id: String, upd: JsObject)(implicit ctx: ExecutionContext): Future[Unit] = {
    coll.update(
      Json.obj("_id" -> BSONObjectID(id)),
      Json.obj("$set" -> upd)).map { _ => () }
  }

  override def batchInsert(elems: Enumerator[JsObject])(implicit ctx: ExecutionContext): Future[LastError] = {
    val enum = elems.map { obj =>
      val id = BSONObjectID.generate
      obj \ "_id" match {
        case _: JsUndefined => Json.obj("_id" -> id) ++ obj
        case _              => obj
      }
    }
    coll.bulkInsert(enum) map { nb =>
      LastError(true, None, None, None, None, 1, false)
    }
  }

  override def find(sel: JsObject, limit: Int = 0, skip: Int = 0)(implicit ctx: ExecutionContext): Future[List[(JsObject, String)]] = {
    val cursor = coll.find(sel).options(QueryOpts().skip(skip)).cursor[JsObject]
    val l = if (limit != 0) cursor.collect[List](limit) else cursor.collect[List]()
    l.map(_.map(js => (js, ((js \ "_id").as[BSONObjectID]).stringify)))
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

  def findFirst(sel: JsObject = Json.obj())(implicit ctx: ExecutionContext) = this.find(sel).map { _.headOption }

}