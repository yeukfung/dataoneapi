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
import doapi.actors.traffic.CoordinateConvertActor
import doapi.actors.traffic.CoordinateConvertActor.HK1980GRIDtoWGS84Request
import doapi.actors.traffic.CoordinateConvertActor.HK1980GRIDtoWGS84Resp

class actorFactoryForTrafficLinkNodeActorSpec @Inject() (val linkActor: ActorInstance[TrafficLinkActor], val coordinateActor: ActorInstance[CoordinateConvertActor])

class TrafficLinkNodeActorSpec extends AkkaSpec("TrafficLinkNodeActorSpec") with TestDBMixin {
  localStart

  lazy val af = globals.injector.getInstance(classOf[actorFactoryForTrafficLinkNodeActorSpec])
  lazy val daoLinkNode = globals.injector.getInstance(classOf[TrafficLinkDao])
  lazy val daoCoordinate = globals.injector.getInstance(classOf[CoordinateInfoDao])

  val dbs = List(daoLinkNode, daoCoordinate)

  "TrafficLinkNodeActor" should {

//    "successfully download link file and perform conversion" in {
//      dropAllDBs
//      af.linkActor.ref ! TrafficActor.SyncLinkData
//
//      r(daoLinkNode.findFirst()) must beNone
//      expectMsg("ok")
//
//      r(daoLinkNode.findFirst()) must beSome
//
//      val Some(item) = r(daoLinkNode.findFirst())
//      val lat = (item._1 \ "lat")
//      lat.asOpt[String] must beNone
//
//      r(nbWait(30))
//
//      val Some(item2) = r(daoLinkNode.findFirst())
//      val lat2 = (item2._1 \ "lat")
//      lat2.asOpt[String] must beSome
//      //      true
//    }
  }

  "Coordinate Actor" should {
    /**
     * { "linkId" : "788-868",
     * "startNode" : "788.0",
     * "startNodeEastings" : 836606.459,
     * "startNodeNorthings" : 815875.71,
     * "endNode" : "868.0",
     * "endNodeEastings" : 836682.388,
     * "endNodeNorthings" : 815883.302,
     * "region" : "HK", "roadType" : "MAJOR_ROUTE", "_id" : ObjectId("54c22a0b3d00003f00910926") }
     */

    "resolve the coordinate to latlng" in {
      af.coordinateActor.ref ! HK1980GRIDtoWGS84Request(815875.71, 836606.459)

      val result = this.expectMsgPF(dur) {
        case msg: HK1980GRIDtoWGS84Resp => true
        case _                          => false
      }

      result must beTrue
    }
  }

  localStop
}