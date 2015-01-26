package amlibs.core.actor

import akka.actor.ActorLogging
import akka.actor.Actor

object ActorCommon {
  trait Req
  trait Resp
  trait Failed extends Resp
}

trait ActorStack extends Actor with ActorLogging {
  implicit val implicitContext = scala.concurrent.ExecutionContext.Implicits.global

  def ops: Receive

  def receive: Receive = ops orElse {
    case x => log.debug("unhandled exception: " + x)
  }
}