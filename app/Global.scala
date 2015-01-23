import com.google.inject.Guice
import akkaguice.AkkaModule
import play.api.mvc.RequestHeader
import play.api.GlobalSettings
import play.api.Logger
import play.api.Application
import play.api.mvc.Action
import play.api.mvc.Results


package object globals {
  val injector = Guice.createInjector(
    new AkkaModule())
}

object Global extends 
GlobalSettings {

  val log = Logger
  override def onStart(app: Application) {
    Logger.info("Data One API Application has started")
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
