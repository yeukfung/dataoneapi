package specs.actors.traffic

import specs.AkkaSpec
import akkaguice.ActorInstance
import doapi.actors.traffic.TrafficSpeedActor
import play.api.Play
import play.api.test.FakeApplication
import doapi.actors.traffic.TrafficActor
import doapi.daos.TrafficSpeedDao
import com.google.inject.Inject
import specs.TestDBMixin
import play.api.libs.json.Json
import play.api.Application
import specs.TestGlobal
import doapi.models.traffic.TrafficModels.SpeedMap
import doapi.actors.traffic.TrafficSpeedProcessingActor
import doapi.daos.TrafficSpeedDataDao
import doapi.models.ModelCommon._
import amlibs.core.daos.JsonQueryHelper
import JsonQueryHelper._
import amlibs.core.utils.ESClient

class trafficSpeedProcessActorFactory @Inject() (val speedProcessingActor: ActorInstance[TrafficSpeedProcessingActor])

class TrafficSpeedProcessActorSpec extends AkkaSpec("TrafficSpeedProcessActorSpec") with TestDBMixin {
  localStart

  lazy val af = globals.injector.getInstance(classOf[trafficSpeedProcessActorFactory])
  lazy val dao = globals.injector.getInstance(classOf[TrafficSpeedDao])
  lazy val daoData = globals.injector.getInstance(classOf[TrafficSpeedDataDao])
  lazy val esClient = new ESClient("http://localhost:9200")

  val dbs = List(daoData)

  "TrafficSpeedProcessingActor" should {

    "able to process downloaded items" in {
      esClient.deleteIndex("traffic")
      dropAllDBs()
      val qDownloaded = state.create(state.v_downloaded)
      val qReady = state.create(state.v_ready)
      r(dao.batchUpdate(Json.obj(), qDownloaded))

      r(dao.findFirst(qReady)) must beNone
      r(daoData.findFirst(Json.obj())) must beNone

      af.speedProcessingActor.ref ! TrafficActor.ProcessDownloadedSpeedData(2)

      expectMsg("ok")
      expectMsg("ok")
      
      r(dao.findFirst(qReady)) must beSome
      r(daoData.findFirst(Json.obj())) must beSome
    }

  }

  localStop
}