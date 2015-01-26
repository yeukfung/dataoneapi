import com.google.inject.Guice
import akkaguice.AkkaModule
import play.api.mvc.RequestHeader
import play.api.GlobalSettings
import play.api.Logger
import play.api.Application
import play.api.mvc.Action
import play.api.mvc.Results
import doapi.actors.GuiceActorFactory
import play.api.libs.concurrent.Akka
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits._

package object globals {
  val injector = Guice.createInjector(
    new AkkaModule())
}

object Global extends GlobalSettings {

  val log = Logger
  override def onStart(app: Application) {
    play.api.Play.mode(app) match {
      case play.api.Mode.Test => // do not schedule anything for Test
      case _                  => activateSchedule(app)
    }
    Logger.info("Data One API Application has started")
  }

  private def activateSchedule(app: Application) {
    Logger.info("activating the scheduler")
    val af = globals.injector.getInstance(classOf[GuiceActorFactory])
    Akka.system(app).scheduler.schedule(2 seconds, 10 minutes, af.jobsActor.ref, "run10m")
    Akka.system(app).scheduler.schedule(10 seconds, 1 minutes, af.jobsActor.ref, "run1m")
  }

  override def onStop(app: Application) {
    Logger.info("Data One API Application has stopped")
  }

  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    globals.injector.getInstance(controllerClass)
  }

  override def onRouteRequest(request: RequestHeader) = {
    super.onRouteRequest(request).orElse {
      Some(request.path).filter(_.endsWith("/")).map(p => Action(Results.MovedPermanently(p.dropRight(1))))
    }
  }

}
