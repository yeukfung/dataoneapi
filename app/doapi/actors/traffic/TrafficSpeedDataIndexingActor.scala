package doapi.actors.traffic

import amlibs.core.actor.ActorStack
import doapi.actors.common.Indexing
import doapi.daos.TrafficSpeedDataDao
import com.google.inject.Inject
import doapi.actors.common.IndexingActor
import amlibs.core.daos.JsonQueryHelper
import doapi.models.ModelCommon
import play.api.libs.json.Json
import doapi.models.traffic.TrafficModels.SpeedMap
import doapi.models.traffic.TrafficModels.TrafficSpeedData
import doapi.models.traffic.TrafficModels.Formats._
import doapi.daos.TrafficLinkDao
import doapi.daos.TrafficLinkMetaDao
import play.api.libs.json.JsNumber
import play.api.libs.json.JsString
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.Date
import java.text.DateFormat
import java.text.SimpleDateFormat
import amlibs.core.utils.ESMixin
import doapi.daos.TrafficSpeedDataCodeDao

class TrafficSpeedDataIndexingActor @Inject() (daoSpeedData: TrafficSpeedDataDao,
                                               daoLink: TrafficLinkDao,
                                               daoLinkMeta: TrafficLinkMetaDao,
                                               daoCode: TrafficSpeedDataCodeDao) extends IndexingActor with ESMixin {

  val ver = "1"

  val IDX = "traffic"

  esClient.createIndex(IDX)

  implicit val dur = Duration(5, "seconds")
  import JsonQueryHelper._
  import ModelCommon._

  def ops = {
    case Indexing.DeleteIndex =>
      esClient.deleteIndex(IDX)

    case req: Indexing.PerformIndexing =>
      l.info("received the indexing request")
      val q = if (!req.forceRefresh) qOr(qExists(state.name, false), state.create(state.v_ready), qAnd(state.create(state.v_indexed), qLt("es.ver", ver))) else Json.obj()

      l.debug(s"queryString: $q")
      val myself = self

      daoSpeedData.find(q, 500) map { list =>
        l.debug(s"returning list of size: ${list.size}")

        list.map {
          x =>
            val js = x._1
            val id = x._2

            val data = js.as[TrafficSpeedData]
            l.debug(s"traffic data generated with linkId: ${data.linkId} with jsContent: $js")

            val regionCode = (js \ "region").as[String]
            for {
              optLink <- daoLink.findFirstByLinkId(data.linkId)
              optLinkMeta <- daoLinkMeta.findFirstByLinkId(data.linkId)
              optRegionName <- daoCode.findFirstCodeValue("region", regionCode)
            } yield {

              val timestamp = qEq("@timestamp", toESTimeStamp(data.captureDate))

              val linkName = if (optLinkMeta.isDefined) (optLinkMeta.get._1 \ "name").as[String] else data.linkId
              val linkInfo = optLink.get._1
              val regionName = optRegionName.getOrElse(regionCode)

              l.debug(s"Inside the for loop of fetching link information optLink.isDefined = ${optLink.isDefined} & optLinkMeta.isDefined = ${optLinkMeta.isDefined} & linkName: $linkName")
              l.debug(s"optLink: ${optLink.get} ")

              val meta = qEq("meta", Json.obj(
                "startLongLat" -> Json.arr((linkInfo \ "startLng"), (linkInfo \ "startLat")),
                "endLongLat" -> Json.arr((linkInfo \ "endLng"), (linkInfo \ "endLat")),
                "regionName" -> regionName,
                "linkName" -> linkName))

              val newState = state.create(state.v_indexed) ++ qEq("es", Json.obj("ver" -> ver))

              val finalJs = js ++ meta ++ newState ++ timestamp
              l.debug(s"id: $id and finalJs: $finalJs")

              daoSpeedData.updatePartial(id, finalJs)

              val dataJs = finalJs ++ qEq("id", id)

              esClient.index("traffic", "speeddata", id, dataJs) map { _ =>
              }

            }
        }

        if (list.size > 0)
          context.system.scheduler.scheduleOnce(Duration(3, "seconds"), myself, req)
      }

      sender ! "ok"

  }
}