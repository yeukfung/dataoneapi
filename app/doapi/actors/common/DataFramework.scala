package doapi.actors.common

import amlibs.core.actor.ActorCommon.Req

object Indexing {
  case object DeleteIndex extends Req
  case class PerformIndexing(forceRefresh: Boolean = false) extends Req
}