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
import doapi.daos.TrafficSpeedDataDao
import doapi.models.ModelCommon._
import amlibs.core.daos.JsonQueryHelper
import JsonQueryHelper._
import amlibs.core.utils.ESClient
import doapi.actors.traffic.TrafficSpeedDataIndexingActor
import play.api.libs.json.JsObject
import doapi.actors.common.Indexing
import play.api.libs.json.JsArray
import doapi.daos.TrafficSpeedDataCodeDao

class trafficSpeedDataIndexingActorFactory @Inject() (val indexingActor: ActorInstance[TrafficSpeedDataIndexingActor])

class TrafficSpeedDataIndexingActorSpec extends AkkaSpec("TrafficSpeedDataIndexingActor") with TestDBMixin {
  localStart

  lazy val af = globals.injector.getInstance(classOf[trafficSpeedDataIndexingActorFactory])
  lazy val daoData = globals.injector.getInstance(classOf[TrafficSpeedDataDao])
  lazy val codeDao = globals.injector.getInstance(classOf[TrafficSpeedDataCodeDao])
  lazy val esClient = new ESClient("http://localhost:9200")

  val dbs = List(daoData, codeDao)

  val beforeJs1 = Json.parse("""
    { "captureDate" : 1422528155000, "linkId" : "34372-3450", "region" : "K", "roadSaturationLevel" : "TRAFFIC AVERAGE", "roadType" : "MAJOR ROUTE", "trafficSpeed" : 43 }
    """).as[JsObject]

  "TrafficSpeedProcessingActor" should {

    "able to index data successfully" in {
      dropAllDBs()
      r(codeDao.saveCode("region", "K", "Kowloon"))
      val qIndexed = state.create(state.v_indexed)
      r(daoData.insert(beforeJs1))

      r(daoData.findFirst(qIndexed)) must beNone
      af.indexingActor.ref ! Indexing.PerformIndexing()

      expectMsg("ok")
      nbWait(2)

      r(daoData.findFirst(qIndexed)) must beSome
      val Some(item) = r(daoData.findFirst(qIndexed))

      (item._1 \ "meta" \ "linkName").asOpt[String] must beSome
      (item._1 \ "meta" \ "regionName").asOpt[String] must beSome("Kowloon")
      (item._1 \ "meta" \ "startLongLat").asOpt[JsArray] must beSome
      val Some(longlat) = (item._1 \ "meta" \ "startLongLat").asOpt[JsArray]

      longlat.toString must_!= "[null,null]"

    }

  }

  localStop
}