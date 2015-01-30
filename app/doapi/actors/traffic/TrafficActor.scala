package doapi.actors.traffic

import amlibs.core.actor.ActorStack
import com.google.inject.Inject
import akkaguice.ActorInstance
import doapi.actors.common.Indexing

object TrafficActor {
  case object DownloadSpeedData
  case object SyncLinkData

  case class ProcessDownloadedSpeedData(batchSize: Int)

}

class TrafficActor @Inject() (speedActor: ActorInstance[TrafficSpeedActor],
                              syncLDActor: ActorInstance[TrafficLinkActor],
                              processingActor: ActorInstance[TrafficSpeedProcessingActor],
                              indexingActor: ActorInstance[TrafficSpeedDataIndexingActor]) extends ActorStack {

  import TrafficActor._

  def ops = {
    case msg: Indexing.PerformIndexing =>
      indexingActor.ref forward msg

    case msg: ProcessDownloadedSpeedData =>
      processingActor.ref forward msg

    case msg @ DownloadSpeedData =>
      speedActor.ref forward msg

    case msg @ SyncLinkData =>
      syncLDActor.ref forward msg

  }
}