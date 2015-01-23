package amlibs.core.router

import play.api.mvc.ActionBuilder
import play.api.mvc.EssentialAction
import play.api.mvc.RequestHeader
import play.api.mvc.PathBindable
import play.api.mvc.Handler
import play.api.Play
import play.api.mvc.Controller
import scala.concurrent.Future
import play.core.Router
import play.api.mvc.Action
import play.api.mvc.Result
import play.api.mvc.Request
import reactivemongo.bson.BSONObjectID

trait RESTController[Id] extends Controller {

  def insert: EssentialAction
  def get(id: Id): EssentialAction
  def delete(id: Id): EssentialAction
  def update(id: Id): EssentialAction
  def updatePartial(id: Id): EssentialAction

  def find: EssentialAction
  def findStream: EssentialAction

  def batchInsert: EssentialAction
  def batchDelete: EssentialAction
  def batchUpdate: EssentialAction

  /** Provides Hooks based on ActionBuilder */
  protected def defaultAction: ActionBuilder[Request] = Action
  protected def getAction: ActionBuilder[Request] = defaultAction
  protected def insertAction: ActionBuilder[Request] = defaultAction
  protected def updateAction: ActionBuilder[Request] = defaultAction
  protected def deleteAction: ActionBuilder[Request] = defaultAction

  protected def onBadRequest(request: RequestHeader, error: String): Future[Result] =
    Play.maybeApplication map { app =>
      app.global.onBadRequest(request, error)
    } getOrElse {
      Future.successful(BadRequest)
    }
}


abstract class RESTRouterContoller[Id](implicit idBindable: PathBindable[Id])
  extends Router.Routes
  with RESTController[Id] {

  private var path: String = ""

  private val Slash = "/?".r
  private val Id = "/([^/]+)/?".r
  private val Partial = "/([^/]+)/partial".r
  private val Find = "/find/?".r
  private val Batch = "/batch/?".r
  private val Stream = "/stream/?".r

  def withId(id: String, action: Id => EssentialAction) =
    idBindable.bind("id", id).fold(badRequest, action)

  def setPrefix(prefix: String) {
    path = prefix
  }

  def prefix = path
  def documentation = Nil
  def routes = new scala.runtime.AbstractPartialFunction[RequestHeader, Handler] {
    override def applyOrElse[RH <: RequestHeader, H >: Handler](rh: RH, default: RH => H) = {
      if (rh.path.startsWith(path)) {
        (rh.method, rh.path.drop(path.length)) match {
          case ("GET", Stream()) => findStream
          case ("GET", Id(id)) => withId(id, get)
          case ("GET", Slash()) => find

          case ("PUT", Batch()) => batchUpdate
          case ("PUT", Partial(id)) => withId(id, updatePartial)
          case ("PATCH", Partial(id)) => withId(id, updatePartial)
          case ("PUT", Id(id)) => withId(id, update)
          case ("PATCH", Id(id)) => withId(id, update)

          case ("POST", Batch()) => batchInsert
          case ("POST", Find()) => find
          case ("POST", Slash()) => insert

          case ("DELETE", Batch()) => batchDelete
          case ("DELETE", Id(id)) => withId(id, delete)
          case _ => default(rh)
        }
      } else {
        default(rh)
      }
    }

    def isDefinedAt(rh: RequestHeader) =
      if (rh.path.startsWith(path)) {
        (rh.method, rh.path.drop(path.length)) match {
          case ("GET", Stream() | Id(_) | Slash()) => true
          case ("PUT", Batch() | Partial(_) | Id(_)) => true
          case ("PATCH", Partial(_) | Id(_)) => true
          case ("POST", Batch() | Slash()) => true
          case ("DELETE", Batch() | Id(_)) => true
          case _ => false
        }
      } else {
        false
      }
  }
}