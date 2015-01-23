package doapi.models.traffic

import play.api.libs.json.JsObject
import play.api.libs.json.Json

object TrafficModels {

  object SpeedMap {
    val f_md5 = "md5"
    val f_src = "src"
    val f_status = "state"
    val f_version = "v"

    val v_status_downloaded = "downloaded"
    val v_status_processing = "processing"
    val v_status_failed = "failed"
    val v_status_ready = "ready"

    def createDownloadedJson(md5: String, src: String): JsObject = {
      Json.obj(f_md5 -> md5, f_src -> src, f_status -> v_status_downloaded)
    }
    
  }

  //case class SpeedMap ()

}