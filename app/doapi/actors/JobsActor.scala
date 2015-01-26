package doapi.actors

import amlibs.core.actor.ActorStack
import com.google.inject.Inject
import akkaguice.ActorInstance
import doapi.actors.traffic.TrafficActor

class JobsActor @Inject() (trafficActor: ActorInstance[TrafficActor]) extends ActorStack {

  def ops = {
    case "run10m" =>
      trafficActor.ref ! TrafficActor.SyncLinkData
    case "run1m" =>
      trafficActor.ref ! TrafficActor.DownloadSpeedData
    case "run" =>

  }
}