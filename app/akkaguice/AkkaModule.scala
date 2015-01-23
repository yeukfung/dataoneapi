package akkaguice

import akka.actor.ActorSystem
import com.google.inject._
import com.typesafe.config.Config
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import scala.reflect.runtime.universe._
import play.api.libs.concurrent.Akka
import play.api.Play.current

object AkkaModule {
  class ActorSystemProvider @Inject() (val injector: Injector) extends Provider[ActorSystem] {
    override def get() = {
      val system = Akka.system
      // add the GuiceAkkaExtension to the system, and initialize it with the Guice injector
      GuiceAkkaExtension(system).initialize(injector)
      system
    }
  }
}

///**
// * A module providing an Akka ActorSystem.
// */
class AkkaModule extends AbstractModule {
  import AkkaModule._
  override def configure() {
    bind(classOf[ActorSystem]).toProvider(classOf[ActorSystemProvider])
  }
}

@Singleton
class ActorInstance[T <: Actor] @Inject() (
  systemProvider: Provider[ActorSystem], builder: ActorBuilder, provider: Provider[T]) {
  lazy val ref: ActorRef = builder(systemProvider.get, provider)
}

@ImplementedBy(classOf[ActorBuilderImpl])
trait ActorBuilder {
  def apply(system: ActorSystem, provider: Provider[_ <: Actor]): ActorRef
}

class ActorBuilderImpl extends ActorBuilder {
  def apply(system: ActorSystem, provider: Provider[_ <: Actor]): ActorRef = {
    val p = Props { provider.get }
    system.actorOf(p)
  }
}
