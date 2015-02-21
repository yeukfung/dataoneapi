package doapi.actors.traffic

import amlibs.core.actor.ActorStack
import com.google.inject.Inject
import akkaguice.ActorInstance
import doapi.actors.common.Indexing
import amlibs.core.actor.NamedActorStack

object TrafficActor {
  case object DownloadSpeedData
  case object SyncLinkData

  case class ProcessDownloadedSpeedData(batchSize: Int)

}

class TrafficActor @Inject() (speedActor: ActorInstance[TrafficSpeedActor],
                              syncLDActor: ActorInstance[TrafficLinkActor],
                              processingActor: ActorInstance[TrafficSpeedProcessingActor],
                              indexingActor: ActorInstance[TrafficSpeedDataIndexingActor],
                              trafficHouseKeep: ActorInstance[TrafficHouseKeepingActor]) extends NamedActorStack {

  import TrafficActor._

  val actorName = "TrafficActor"
  
  def ops = {
    case msg: Indexing.PerformIndexing =>
      indexingActor.ref forward msg

    case msg: ProcessDownloadedSpeedData =>
      processingActor.ref forward msg

    case msg @ DownloadSpeedData =>
      speedActor.ref forward msg

    case msg @ SyncLinkData =>
      syncLDActor.ref forward msg
      
    case "housekeep" =>
      trafficHouseKeep.ref ! "housekeep"

  }
}