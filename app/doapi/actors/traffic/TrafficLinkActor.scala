package doapi.actors.traffic

import amlibs.core.actor.ActorStack
import akkaguice.ActorInstance
import com.google.inject.Inject
import scala.concurrent.Await
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.Writes
import com.ning.http.client.Response
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import doapi.daos.TrafficLinkDao
import play.api.libs.ws.WS
import play.api.libs.json.JsObject
import scala.concurrent.duration.Duration
import amlibs.core.playspecific.PlayMixin
import doapi.models.ModelCommon

/**
 * an actor to keep track of the link node from following link:
 * @url: http://www.gov.hk/en/theme/psi/datasets/tsm_link_and_node_info_v2.xls
 */

sealed trait REGION
case object HK extends REGION
case object K extends REGION
case object TM extends REGION
case object ST extends REGION

sealed trait ROAD_TYPE
case object MAJOR_ROUTE extends ROAD_TYPE
case object URBAN_ROUTE extends ROAD_TYPE

sealed trait ROAD_SATURATION_LEVEL
case object TRAFFIC_GOOD extends ROAD_SATURATION_LEVEL
case object TRAFFIC_AVERAGE extends ROAD_SATURATION_LEVEL
case object TRAFFIC_BAD extends ROAD_SATURATION_LEVEL

case class LinkNode(
  linkId: String,
  startNode: String,
  startNodeEastings: Double,
  startNodeNorthings: Double,
  endNode: String,
  endNodeEastings: Double,
  endNodeNorthings: Double,
  region: REGION,
  roadType: ROAD_TYPE)

object TrafficLinkActor extends PlayMixin {
  val REPEAT_IN_MINUTES = 10
  val DATASETURL = conf.getString("dataoneapi.traffic.linknodexlsurl").getOrElse("http://www.gov.hk/en/theme/psi/datasets/tsm_link_and_node_info_v2.xls")
}

object MyWriter {
  implicit val anyValWriter = Writes[Any](a => a match {
    case v: String => Json.toJson(v)
    case v: Int => Json.toJson(v)
    case v: JsString => v
    case v: Double => Json.toJson(v)
    case v: REGION if v == HK => JsString("HK")
    case v: REGION if v == K => JsString("K")
    case v: REGION if v == TM => JsString("TM")
    case v: REGION if v == ST => JsString("ST")
    case v: ROAD_TYPE if v == URBAN_ROUTE => JsString("URBAN_ROUTE")
    case v: ROAD_TYPE if v == MAJOR_ROUTE => JsString("MAJOR_ROUTE")
    case v: ROAD_SATURATION_LEVEL if v == TRAFFIC_GOOD => JsString("TRAFFIC_GOOD")
    case v: ROAD_SATURATION_LEVEL if v == TRAFFIC_AVERAGE => JsString("TRAFFIC_AVERAGE")
    case v: ROAD_SATURATION_LEVEL if v == TRAFFIC_BAD => JsString("TRAFFIC_BAD")

    // or, if you don't care about the value
    case a @ _ =>
      println(a.getClass)
      throw new RuntimeException("unserializeable type")
  })

  implicit val linkNodeWriter = Writes[LinkNode](a => a match {
    case v: LinkNode => Json.obj(
      "linkId" -> v.linkId,
      "startNode" -> v.startNode,
      "startNodeEastings" -> v.startNodeEastings,
      "startNodeNorthings" -> v.startNodeNorthings,
      "endNode" -> v.endNode,
      "endNodeEastings" -> v.endNodeEastings,
      "endNodeNorthings" -> v.endNodeNorthings,
      "region" -> Json.toJson(v.region),
      "roadType" -> Json.toJson(v.roadType))
  })
}

class TrafficLinkActor @Inject() (trafficLinkDao: TrafficLinkDao, convertActor: ActorInstance[CoordinateConvertActor]) extends ActorStack with PlayMixin {
  import TrafficLinkActor._
  import MyWriter._
  import doapi.actors.traffic.CoordinateConvertActor._

  def ops = {
    case req: HK1980GRIDtoWGS84Resp =>
      req.reqMeta.map { m =>
        val linkId = m("linkId").toString
        val mode = m("mode").toString
        log.info(s"checking the linkID: $linkId from resp: $req")
        trafficLinkDao.findFirst(Json.obj("linkId" -> linkId)) map {
          case Some(linkNode) =>
            val upd = Json.obj(mode + "Lat" -> req.lat, mode + "Lng" -> req.long, ModelCommon.state.name -> ModelCommon.state.v_ready)
            log.info(s"found some linkNode with linkId: $linkId with dataToUpdate: $upd")
            trafficLinkDao.updatePartial(linkNode._2, upd) map { le =>
              log.info("updated linkId with latLong : " + req.lat + "," + req.long)
            }
          case None =>
            log.info(s"There is no such linkId exist in db: $linkId")
        }
      }

    case TrafficActor.SyncLinkData =>
      val requestor = self
      val originator = sender
      WS.url(DATASETURL).get().map { r =>
        log.info(s"received xls response from $DATASETURL")
        val rawResp = r.underlying[Response]
        var stream = rawResp.getResponseBodyAsStream()

        val workbook = new HSSFWorkbook(stream)

        val sheet = workbook.getSheetAt(0);

        var rowIterator = sheet.iterator()
        while (rowIterator.hasNext()) {

          val row = rowIterator.next()

          val linkID = row.getCell(0).toString()

          val col2 = row.getCell(1).toString()
          if (linkID.equalsIgnoreCase("link id") || col2.startsWith("出發")) {
            log.debug(s"skipping header line : $linkID and $col2")
          } else {

            Await.result(trafficLinkDao.findFirst(Json.obj("linkId" -> linkID, ModelCommon.state.name -> ModelCommon.state.v_ready)) map { nodeOpt =>
              log.debug("after query with isDefined: " + nodeOpt.isDefined)

              if (!nodeOpt.isDefined) {

                val startNode = row.getCell(1).toString()
                val startEastings = row.getCell(2).toString().toDouble
                val startNorthings = row.getCell(3).toString().toDouble
                val endNode = row.getCell(4).toString()
                val endEastings = row.getCell(5).toString().toDouble
                val endNorthings = row.getCell(6).toString().toDouble

                val region = row.getCell(7).toString() match {
                  case "HK" => HK
                  case "K"  => K
                  case "TM" => TM
                  case "ST" => ST
                  case _    => HK
                }

                val roadType = row.getCell(8).toString() match {
                  case "MAJOR ROUTE" => MAJOR_ROUTE
                  case "URBAN ROUTE" => URBAN_ROUTE
                  case _             => MAJOR_ROUTE
                }

                val linkNode = LinkNode(linkID, startNode, startEastings, startNorthings, endNode, endEastings, endNorthings, region, roadType)
                val js = Json.toJson(linkNode)(linkNodeWriter).as[JsObject]
                log.debug(js.toString)
                trafficLinkDao.insert(js) map { le =>
                  convertActor.ref.tell(HK1980GRIDtoWGS84Request(startNorthings, startEastings, Some(Map("linkId" -> linkID, "mode" -> "start"))), requestor)
                  convertActor.ref.tell(HK1980GRIDtoWGS84Request(endNorthings, endEastings, Some(Map("linkId" -> linkID, "mode" -> "end"))), requestor)
                  originator ! "ok"
                }

              } else {
                log.debug("skipping the existing linkID: " + linkID)
              }

            }, Duration(5, "seconds"))

          }
        }
        log.debug("closing stream")
        stream.close()
      }
  }

}