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

class TrafficSpeedDataIndexingActor @Inject() (daoSpeedData: TrafficSpeedDataDao, daoLink: TrafficLinkDao, daoLinkMeta: TrafficLinkMetaDao) extends IndexingActor {

  val ver = "1"

  import JsonQueryHelper._
  import ModelCommon._
  def ops = {
    case Indexing.PerformIndexing =>
      val q = qOr(state.create(state.v_ready), qAnd(state.create(state.v_indexed), qLt("es.ver", ver)))
      daoSpeedData.find(q, 100) map { list =>
        list.foreach {
          case (js, id) =>
            val data = js.as[TrafficSpeedData]

            val metaJs = for {
              optLink <- daoLink.findFirstByLinkId(data.linkId)
              optLinkMeta <- daoLinkMeta.findFirstByLinkId(data.linkId)
            } yield {
              qEq("meta", Json.obj(
                "startLongLat" -> Json.arr((js \ "startLng").as[JsNumber], (js \ "startLat").as[JsNumber]),
                "endLongLat" -> Json.arr((js \ "endLng").as[JsNumber], (js \ "endLat").as[JsNumber]),
                "link_name" -> JsString(optLinkMeta.map(item => (item._1 \ "name").as[String]).getOrElse(data.linkId))))
            }

            val newState = state.create(state.v_indexed) ++ qEq("es", Json.obj("ver" -> ver))
        }
      }
  }
}