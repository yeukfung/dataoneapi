package doapi.actors.traffic

import akka.actor.Actor
import play.api.libs.ws.WS
import play.api.libs.concurrent.Akka
import scala.concurrent.duration.Duration
import scala.collection.mutable.Queue
import akka.actor.ActorRef
import org.openqa.selenium.WebDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.Select
import amlibs.core.actor.ActorStack
import amlibs.core.playspecific.PlayMixin
import doapi.daos.CoordinateInfoDao
import com.google.inject.Inject

/**
 * from website:
 * http://www.geodetic.gov.hk/smo/tform/tform.aspx
 * Input - HK 1980 Grid Coordinate
 * Output - WGS84 (ITRF96 Reference Frame)
 *
 *
 *
 */

object CoordinateConvertActor {
  case class HK1980GRIDtoWGS84Request(northings: Double, eastings: Double, reqMeta: Option[Map[String, Any]] = None) {
    def toId = s"${northings}_${eastings}"
  }
  case class HK1980GRIDtoWGS84Resp(lat: Double, long: Double, reqMeta: Option[Map[String, Any]])
}

class CoordinateConvertActor @Inject() (coordinateInfoDao: CoordinateInfoDao) extends ActorStack with PlayMixin {
  import CoordinateConvertActor._
  val formUrl = conf.getString("dataoneapi.traffic.convertFormUrl").getOrElse("http://www.geodetic.gov.hk/smo/tform/tform.aspx")

  var cnt = 0

  var queue: Queue[(HK1980GRIDtoWGS84Request, ActorRef)] = Queue.empty

  var started = false
  def ops = {

    case "start" =>
      if (!started) {
        started = true
        self ! "batch"
        //Akka.system.scheduler.scheduleOnce(Duration(10, "second"), self, "batch")
      }

    case "stop" =>
      started = false

    case "batch" =>
      log.info(s"processing with batch size: ${queue.size}")
      if (queue.size > 0) {
        val driver: WebDriver = new HtmlUnitDriver(true)
        driver.get(formUrl)

        val eventTarget = driver.findElement(By.id("__EVENTTARGET")).getAttribute("value")
        val eventArgument = driver.findElement(By.id("__EVENTARGUMENT")).getAttribute("value")
        val viewState = driver.findElement(By.id("__VIEWSTATE")).getAttribute("value")
        val eventValidation = driver.findElement(By.id("__EVENTVALIDATION")).getAttribute("value")
        val paramCore = Map(
          "__EVENTTARGET" -> eventTarget,
          "__EVENTARGUMENT" -> eventArgument,
          "__LASTFOCUS" -> "",
          "__VIEWSTATE" -> viewState,
          "__EVENTVALIDATION" -> eventValidation,
          "DropDownList1" -> "inHK1980",
          "DropDownList2" -> "outITRF96",
          "RadioButtonList1" -> "DecDegree",
          "Submit1" -> "Submit")

        var processingItems: List[((HK1980GRIDtoWGS84Request, ActorRef), String, String)] = List()

        def genTextboxMap(acc: Map[String, String], cnt: Int): Map[String, String] = if (cnt > 10) {
          acc
        } else {
          val currentText1 = s"TextBox${cnt}1"
          val currentText2 = s"TextBox${cnt}2"
          val currentText3 = s"TextBox${cnt}3"

          /**
           * <tr style="font-size: 12pt">
           * <td bordercolor="lightgrey" style="width: 71px;">
           * <input name="TextBox71" type="text" value="825962.9759940933_828232.3585854033" maxlength="12" id="TextBox71" /></td>
           * <td bordercolor="lightgrey">
           * <input name="TextBox72" type="text" value="825962.9759940933" maxlength="10" id="TextBox72" /></td>
           * <td bordercolor="lightgrey">
           * <input name="TextBox73" type="text" value="828232.3585854033" maxlength="10" id="TextBox73" /></td>
           * <td bordercolor="lightgrey">
           * <input name="TextBox74" type="text" id="TextBox74" disabled="disabled" /></td>
           * <td bordercolor="lightgrey">
           * <span id="Label75" style="display:inline-block;"><font size="2">825962.9759940933_828232.3585854033</font></span></td>
           * <td bordercolor="lightgrey" style="width: 105px;">
           * <span id="Label76" style="display:inline-block;"><font size="2">22.372833005</font></span></td>
           * <td bordercolor="lightgrey">
           * <span id="Label77" style="display:inline-block;"><font size="2">114.098850974</font></span></td>
           * <td bordercolor="lightgrey">
           * <span id="Label78" style="display:inline-block;"><font size="2"></font></span></td>
           *
           * </tr>
           *
           */
          var targetLat = s"Label${cnt}6"
          var targetLong = s"Label${cnt}7"

          var map: Map[String, String] = Map.empty
          if (queue.size > 0) {
            val item = queue.dequeue
            processingItems ::= (item, targetLat, targetLong)
            map = acc ++ Map(
              currentText1 -> item._1.toId,
              currentText2 -> item._1.northings.toString,
              currentText3 -> item._1.eastings.toString)

          } else {
            map = acc ++ Map(
              currentText1 -> "",
              currentText2 -> "",
              currentText3 -> "")
          }
          genTextboxMap(map, cnt + 1)
        }

        val textWithParam = genTextboxMap(Map(), 1)

        val param = paramCore ++ textWithParam

        log.debug("==== after ====")
        //        log.debug(driver.getPageSource())
        //log.debug("result = " + driver.findElement(By.cssSelector("#Label16")).getText() + " .... " + driver.findElement(By.cssSelector("#Label17")).getText())
        val resp = WS.url(formUrl).withHeaders("Content-Type" -> "application/x-www-form-urlencoded").post(param.map(item => item._1 -> Seq(item._2)))
        resp map { rs =>
          processingItems.foreach {
            item =>
              val pattern1 = s"""<span id="${item._2}" style="display:inline-block;"><font size="2">(\\d+\\.\\d+)</font></span>""".r
              val pattern2 = s"""<span id="${item._3}" style="display:inline-block;"><font size="2">(\\d+\\.\\d+)</font></span>""".r
              val result1 = pattern1.findFirstMatchIn(rs.body) map { _ group 1 }
              val result2 = pattern2.findFirstMatchIn(rs.body) map { _ group 1 }

              val requestor = item._1._2
              requestor ! HK1980GRIDtoWGS84Resp(result1.get.toDouble, result2.get.toDouble, item._1._1.reqMeta)
            //              log.debug(result1.get)
            //              log.debug(result2.get)
          }
          //log.debug(rs.body)

        }
        //easting

      }

      if (started) {
        Akka.system.scheduler.scheduleOnce(Duration(1, "second"), self, "batch")
      }

    case req: HK1980GRIDtoWGS84Request =>
      if (!started) self ! "start"
      queue.enqueue((req, sender))
  }
}