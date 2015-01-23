package amlibs.core.actor

import akka.actor.ActorLogging
import akka.actor.Actor

abstract class MasterActor extends Actor with ActorLogging {
  
  def ops: PartialFunction[Any, Unit] = Map.empty
  
  def receive = ops orElse {
    case msg @ _ =>
  }
}

abstract class AMActor extends Actor with ActorLogging {

  def ops: PartialFunction[Any, Unit] = Map.empty

  def receive = ops orElse {
    case msg @ _ => log.debug("unknown msg type: " + msg)
  }
}