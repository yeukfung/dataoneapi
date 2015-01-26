package amlibs.core.actor.es

import amlibs.core.actor.ActorStack
import amlibs.core.playspecific.PlayMixin
import play.api.libs.json.JsObject
import akka.actor.ActorRef
import amlibs.core.actor.ActorCommon.Req

object ElasticSearchActor {

  trait ESRequest extends Req {
    def params: JsObject
    def callback: Option[ActorRef]
  }

  case class Create(params: JsObject, callback: Option[ActorRef] = None) extends ESRequest
  
}

class ElasticSearchActor extends ActorStack with PlayMixin {

  def ops = {
    case _ =>
  }
}