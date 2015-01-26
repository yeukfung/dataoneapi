package amlibs.core.playspecific

import play.api.Play
import amlibs.core.utils.FrameworkUtils
import play.api.libs.ws.WS
import play.api.Logger

trait PlayMixin {
  implicit val implicitPlay = Play.current

  lazy val conf = Play.current.configuration
  
  lazy val l = Logger

}

class PlayUtils extends PlayMixin {
//  val ws_get(url)
}