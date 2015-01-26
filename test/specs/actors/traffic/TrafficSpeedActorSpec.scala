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

class actorFactory @Inject() (val speedActor: ActorInstance[TrafficSpeedActor])

class TrafficSpeedActorSpec extends AkkaSpec("TrafficSpeedActor") with TestDBMixin {
  localStart

  lazy val af = globals.injector.getInstance(classOf[actorFactory])
  lazy val dao = globals.injector.getInstance(classOf[TrafficSpeedDao])

  val dbs = List(dao)

  "TrafficSpeedActor Download" should {

    "able to download the data and store correctly with state = downloaded" in {
      this.dropAllDBs()
      r(dao.find(Json.obj()).map(_.size)) must_== 0

      af.speedActor.ref ! TrafficActor.DownloadSpeedData

      this.expectMsg("ok")
      val list = r(dao.find(Json.obj()))
      list.size must_== 1
      val status = list.head._1 \ SpeedMap.f_state

      status.as[String] must_== SpeedMap.v_state_downloaded

    }

    "able to avoid duplicated download" in {
      this.dropAllDBs()
      af.speedActor.ref ! TrafficActor.DownloadSpeedData
      af.speedActor.ref ! TrafficActor.DownloadSpeedData

      this.expectMsg("ok")
      r(dao.find(Json.obj()).map(_.size)) must_== 1
    }

  }

  localStop
}