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
import doapi.daos.TrafficSpeedDataCodeDao

class trafficInitFactory @Inject() ()

class TrafficInitSpec extends AkkaSpec("TrafficInitSpec") with TestDBMixin {
  localStart

  //  lazy val af = globals.injector.getInstance(classOf[trafficSpeedProcessActorFactory])
  lazy val daoCode = globals.injector.getInstance(classOf[TrafficSpeedDataCodeDao])
  lazy val daoData = globals.injector.getInstance(classOf[TrafficSpeedDataDao])
  lazy val esClient = new ESClient("http://localhost:9200")

  val dbs = List(daoData, daoCode)

  "Traffic Init Spec" should {

    "init all static data" in {
      dropAllDBs()
      r(esClient.deleteIndex("traffic") flatMap { res =>
        esClient.createIndex("traffic")
      })
      r(daoCode.saveCode("region", "K", "Kowloon"))
      r(daoCode.saveCode("region", "HK", "Hong Kong"))
      r(daoCode.saveCode("region", "TM", "Tuen Mun"))
      r(daoCode.saveCode("region", "ST", "Sha Tin"))

      true
    }

  }

  localStop
}