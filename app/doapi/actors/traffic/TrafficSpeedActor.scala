package doapi.actors.traffic

import amlibs.core.actor.ActorStack
import com.google.inject.Inject
import doapi.daos.TrafficSpeedDao
import play.api.libs.ws.WS
import org.apache.http.HttpStatus
import play.api.libs.Crypto
import play.api.libs.Codecs
import amlibs.core.daos.JsonQueryHelper
import doapi.models.traffic.TrafficModels.SpeedMap
import play.api.libs.json.Json
import amlibs.core.playspecific.PlayMixin
import akkaguice.ActorInstance
import scala.concurrent.duration.Duration

class TrafficSpeedActor @Inject() (speedmapDao: TrafficSpeedDao, trafficActor: ActorInstance[TrafficActor]) extends ActorStack with PlayMixin {

  import JsonQueryHelper._

  val SPEEDXML_URL = conf.getString("dataoneapi.traffic.speedxmlurl").getOrElse("http://data.one.gov.hk/others/td/speedmap.xml")

  def ops = {
      
    case TrafficActor.DownloadSpeedData =>

      
      val requestor = sender
      for {
        wsResp <- WS.url(SPEEDXML_URL).get()
      } {
        if (wsResp.status == HttpStatus.SC_OK) {
          val md5Code = Codecs.md5(wsResp.body.getBytes)
          val q = qEq(SpeedMap.f_md5, md5Code)

          // only insert when it is not found in db
          speedmapDao.find(q).map {
            _.headOption match {
              case None =>
                val js = SpeedMap.createDownloadedJson(md5Code, wsResp.body)
                speedmapDao.insert(js).map { id =>
                  val updatedJs = js ++ Json.obj("id" -> id)
                  requestor ! "ok"
                 
                  context.system.scheduler.scheduleOnce(Duration(5, "seconds") ,trafficActor.ref, TrafficActor.ProcessDownloadedSpeedData(2)) 
                  
                }
              case Some(x) => requestor ! "ok - discarded as duplicated"
            }
          }
        }
      }

  }

}