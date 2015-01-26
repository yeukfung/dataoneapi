package doapi.models

import play.api.libs.json.Json
import play.api.libs.json.JsObject
import amlibs.core.daos.JsonQueryHelper

object ModelCommon {
  import JsonQueryHelper._

  object state {
    val name = "state"

    val v_ready = "ready"
    val v_downloaded = "downloaded"
    val v_processing = "processing"
    val v_failed = "failed"

    def create(s: String): JsObject = Json.obj(name -> s)

  }

  object error {
    val name = "error"

    val f_error_message = "msg"
    val f_error_time = "time"
    val f_error_source = "src"

    def create(msg: String, src: Option[String] = None): JsObject =
      qEq(name, Json.obj(f_error_message -> msg, f_error_source -> src, f_error_time -> System.currentTimeMillis()))
  }

  //  object StateMode {
  //    val READY = StateMode("ready")
  //    val PROCESSING = Sta
  //  }
  //  case class StateMode(value: String) {
  //    def is(s: StateMode) = (s.value == value)
  //  }
  //
  //  case class State(mode: StateMode) {
  //    def toJs = state.create(mode.value)
  //  }

}