package doapi.models.traffic

import play.api.libs.json.JsObject
import play.api.libs.json.Json
import java.util.Date
import play.api.libs.json.Format
import play.api.libs.json.JsArray

object TrafficModels {

  object SpeedMap {
    val f_md5 = "md5"
    val f_src = "src"
    val f_state = "state"
    val f_version = "v"
    val f_result = "result"

    val v_state_downloaded = "downloaded"
    val v_state_processing = "processing"
    val v_state_failed = "failed"
    val v_state_ready = "ready"

    def createDownloadedJson(md5: String, src: String): JsObject = {
      Json.obj(f_md5 -> md5, f_src -> src, f_state -> v_state_downloaded)
    }

    def createProcessedResult(result: JsArray): JsObject = {
      Json.obj(f_result -> result, f_state -> v_state_ready)
    }

  }

  //case class SpeedMap ()

  /* <jtis_speedmap>
       * <LINK_ID>3006-30069</LINK_ID>
       * <REGION>K</REGION>
       * <ROAD_TYPE>URBAN ROAD</ROAD_TYPE>
       * <ROAD_SATURATION_LEVEL>TRAFFIC AVERAGE</ROAD_SATURATION_LEVEL>
       * <TRAFFIC_SPEED>29</TRAFFIC_SPEED>
       * <CAPTURE_DATE>2015-01-26T12:18:35</CAPTURE_DATE>
       * </jtis_speedmap>
*/
  case class TrafficSpeedData(
    linkId: String,
    region: String,
    roadType: String,
    roadSaturationLevel: String,
    trafficSpeed: Int,
    captureDate: String,
    parseCaptureDate: Date)

  object Formats {
    implicit val traiffSpeedDataFormat = Json.format[TrafficSpeedData]
  }

  object TrafficMeta {

    val f_startLat = "startLat"

    def create(linkJs: JsObject, current: TrafficSpeedData): JsObject = {
      Json.obj()
    }
  }
}