package doapi.actors

import amlibs.core.actor.ActorStack
import com.google.inject.Inject
import akkaguice.ActorInstance
import doapi.actors.traffic.TrafficActor
import doapi.actors.common.Indexing
import amlibs.core.actor.NamedActorStack

class JobsActor @Inject() (trafficActor: ActorInstance[TrafficActor]) extends NamedActorStack {

  val actorName = "JobsActor"
  def ops = {
    case "run15s" =>
      trafficActor.ref ! TrafficActor.DownloadSpeedData
    case "run10m" =>
      trafficActor.ref ! TrafficActor.SyncLinkData
    case "run1m" =>
      trafficActor.ref ! TrafficActor.ProcessDownloadedSpeedData(5)
    case "runOnce" =>
      trafficActor.ref ! "housekeep"
  }
}