package doapi.actors.traffic

import amlibs.core.actor.ActorStack
import com.google.inject.Inject
import akkaguice.ActorInstance

object TrafficActor {
  case object DownloadSpeedData
  case object SyncLinkData

  case object ProcessDownloadedSpeedData

}

class TrafficActor @Inject() (speedActor: ActorInstance[TrafficSpeedActor], syncLDActor: ActorInstance[TrafficLinkActor]) extends ActorStack {

  import TrafficActor._

  def ops = {
    case msg @ DownloadSpeedData =>
      speedActor.ref forward msg

    case msg @ SyncLinkData =>
      syncLDActor.ref forward msg

  }
}