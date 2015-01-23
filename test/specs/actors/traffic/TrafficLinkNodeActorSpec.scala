package specs.actors.traffic

import doapi.daos.TrafficSpeedDao
import specs.AkkaSpec
import akkaguice.ActorInstance
import specs.TestDBMixin
import com.google.inject.Inject
import doapi.actors.traffic.TrafficSpeedActor
import doapi.actors.traffic.TrafficLinkActor
import doapi.daos.TrafficLinkDao
import doapi.daos.CoordinateInfoDao
import doapi.actors.traffic.TrafficActor

class actorFactoryForTrafficLinkNodeActorSpec @Inject() (val linkActor: ActorInstance[TrafficLinkActor])

class TrafficLinkNodeActorSpec extends AkkaSpec("TrafficLinkNodeActorSpec") with TestDBMixin {
  localStart

  lazy val af = globals.injector.getInstance(classOf[actorFactoryForTrafficLinkNodeActorSpec])
  lazy val daoLinkNode = globals.injector.getInstance(classOf[TrafficLinkDao])
  lazy val daoCoordinate = globals.injector.getInstance(classOf[CoordinateInfoDao])

  val dbs = List(daoLinkNode, daoCoordinate)

  "TrafficLinkNodeActor" should {

    "successfully download link file and perform conversion" in {
      dropAllDBs
      af.linkActor.ref ! TrafficActor.SyncLinkData

      r(daoLinkNode.findFirst()) must beNone
      expectMsg("ok")

      r(daoLinkNode.findFirst()) must beSome

      val Some(item) = r(daoLinkNode.findFirst())
      val lat = (item._1 \ "lat")
      lat.asOpt[String] must beNone

      r(nbWait(30))

      val Some(item2) = r(daoLinkNode.findFirst())
      val lat2 = (item2._1 \ "lat")
      lat2.asOpt[String] must beSome

    }
  }

  localStop
}