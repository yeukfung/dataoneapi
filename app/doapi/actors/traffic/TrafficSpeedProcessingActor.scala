package doapi.actors.traffic

import amlibs.core.playspecific.PlayMixin
import amlibs.core.actor.ActorStack
import com.google.inject.Inject
import doapi.daos.TrafficSpeedDao
import amlibs.core.daos.DaoUtils
import doapi.models.traffic.TrafficModels.SpeedMap
import amlibs.core.daos.JsonQueryHelper
import akka.actor.Props
import play.api.libs.json.Json
import java.util.Date
import java.util.Calendar
import doapi.daos.TrafficSpeedDataDao
import doapi.models.traffic.TrafficModels.TrafficSpeedData
import play.api.libs.json.JsObject
import amlibs.core.utils.ESClient
import akka.actor.PoisonPill
import JsonQueryHelper._
import java.text.SimpleDateFormat
import doapi.models.ModelCommon._
import doapi.actors.common.Indexing
import akkaguice.ActorInstance
import scala.concurrent.duration.Duration
import doapi.actors.common.JsDataLogging
import amlibs.core.actor.NamedActorStack

class TrafficSpeedProcessingActor @Inject() (trafficSpeedDao: TrafficSpeedDao,
                                             trafficSpeedDataDao: TrafficSpeedDataDao,
                                             trafficActor: ActorInstance[TrafficActor]) extends NamedActorStack with PlayMixin {

  val actorName = "TrafficSpeedProcessingActor"
  val indexingEnabled = conf.getBoolean("indexing.enabled").getOrElse(false)
  def ops = {

    case req: TrafficActor.ProcessDownloadedSpeedData =>
      val requestor = sender
      val q = qEq(SpeedMap.f_state, SpeedMap.v_state_downloaded)
      trafficSpeedDao.find(q, req.batchSize).map { resultList =>

        resultList.map {
          item =>
            val worker = context.actorOf(Props(new TrafficSpeedProcessingWorker(trafficSpeedDao, trafficSpeedDataDao)))
            worker.tell(item, requestor)
        }

        // perform indexing after 3 seconds of process
        if (indexingEnabled && resultList.size > 0)
          context.system.scheduler.scheduleOnce(Duration(3, "seconds"), trafficActor.ref, Indexing.PerformIndexing())
      }

  }

}

class TrafficSpeedProcessingWorker(dao: TrafficSpeedDao, speedDataDao: TrafficSpeedDataDao) extends NamedActorStack with JsDataLogging {

  import scala.xml._
  import doapi.models.traffic.TrafficModels.Formats._

  val actorName = "TrafficSpeedProcessingWorker"
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
  def ops = {
    case (item: play.api.libs.json.JsObject, id: String) =>

      val requestor = sender
      val myself = self

      log.info(s"processing traffic speed data record in id: $id")

      (item \ SpeedMap.f_src).asOpt[String] map { srcString =>

        try {

          val md5 = (item \ "md5").as[String]
          val xml = scala.xml.XML.loadString(srcString)
          log.debug(s"xml is successfully loaded for id: $id")
          /**
           * <jtis_speedmap>
           * <LINK_ID>3006-30069</LINK_ID>
           * <REGION>K</REGION>
           * <ROAD_TYPE>URBAN ROAD</ROAD_TYPE>
           * <ROAD_SATURATION_LEVEL>TRAFFIC AVERAGE</ROAD_SATURATION_LEVEL>
           * <TRAFFIC_SPEED>29</TRAFFIC_SPEED>
           * <CAPTURE_DATE>2015-01-26T12:18:35</CAPTURE_DATE>
           * </jtis_speedmap>
           */
          val nodes = xml \\ "jtis_speedmap"

          dao.markState(id, state.create(state.v_processing))

          var allResult = Json.arr()
          nodes foreach { n =>
            val linkId = (n \ "LINK_ID").text
            val region = (n \ "REGION").text
            val roadType = (n \ "ROAD_TYPE").text
            val roadSaturationLevel = (n \ "ROAD_SATURATION_LEVEL").text
            val trafficSpeed = (n \ "TRAFFIC_SPEED").text.toInt
            val captureDate = (n \ "CAPTURE_DATE").text
            val parsedCaptureDate = dateFormat.parse(captureDate)

            val data = TrafficSpeedData(linkId, region, roadType, roadSaturationLevel, trafficSpeed, captureDate, parsedCaptureDate)

            val logJs = Json.toJson(data).as[JsObject]

            val newJs = Json.toJson(data).as[JsObject] ++ qEq("md5", md5)
            speedDataDao.insert(newJs) map { id =>
              jlSpeedmap(logJs ++ qEq("id", id))
            }
            allResult = allResult.append(newJs)
          }

          val processedResult = SpeedMap.createProcessedResult(allResult)

          log.debug(s"result has been processed for id: $id")

          dao.updatePartial(id, processedResult)

          log.debug(s"finished processing traffic speed data record in id: $id")
          requestor ! "ok"
        } catch {
          case e: Exception =>
            val formatedErr = s"exception durring processing item: ${e.getMessage} with srcString: $srcString"
            log.error(formatedErr)
            dao.markState(id, state.create(state.v_failed) ++ error.create(formatedErr, Some("TrafficSpeedProcessingWorker")))
            requestor ! "err"
        }
      } getOrElse {
      }

      context.system.scheduler.scheduleOnce(Duration(10, "seconds"), myself, PoisonPill)

  }
}