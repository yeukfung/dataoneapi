package amlibs.core.playspecific

import play.api.Play
import amlibs.core.utils.FrameworkUtils
import play.api.libs.ws.WS

trait PlayMixin {
  implicit val implicitPlay = Play.current

  lazy val conf = Play.current.configuration

}

class PlayUtils extends PlayMixin {
//  val ws_get(url)
}