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
import amlibs.core.daos.JsonQueryHelper
import play.api.libs.json.Json
import scala.concurrent.Future
import amlibs.core.actor.NamedActorStack

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

  object CoordinateInfo {
    val f_northings = "northings"
    val f_eastings = "eastings"
    val f_lat = "lat"
    val f_long = "lng"

    def create(n: Double, e: Double, lat: Double, long: Double) = {
      Json.obj(f_northings -> n, f_eastings -> e, f_lat -> lat, f_long -> long)
    }
  }
}

class CoordinateConvertActor @Inject() (coordinateInfoDao: CoordinateInfoDao) extends NamedActorStack with PlayMixin {
  import CoordinateConvertActor._
  import JsonQueryHelper._

  val actorName = "CoordinateConvertActor"
  val formUrl = conf.getString("dataoneapi.traffic.convertFormUrl").getOrElse("http://www.geodetic.gov.hk/smo/tform/tform.aspx")

  val BATCH_SIZE = 10

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

        def genTextboxMap(acc: Map[String, String], cnt: Int): Map[String, String] = if (cnt > BATCH_SIZE) {
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
            try {

              val item = queue.front

              log.debug("processing current item: " + item)
              processingItems ::= (item, targetLat, targetLong)
              map = acc ++ Map(
                currentText1 -> item._1.toId,
                currentText2 -> item._1.northings.toString,
                currentText3 -> item._1.eastings.toString)

              queue.dequeue

            } catch {
              case e: Exception =>
                log.error("Exception: " + e.getMessage)
                map = acc ++ Map(
                  currentText1 -> "",
                  currentText2 -> "",
                  currentText3 -> "")

                log.error("why item is null? queue dump, should send mail to admin, resetting all case inside queue")
                queue = Queue.empty
            }

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
              val lat = result1.get.toDouble
              val long = result2.get.toDouble

              val js = CoordinateInfo.create(item._1._1.northings, item._1._1.eastings, lat, long)
              coordinateInfoDao.insert(js) map { id =>
                requestor ! HK1980GRIDtoWGS84Resp(lat, long, item._1._1.reqMeta)
              }
            //              log.debug(result1.get)
            //              log.debug(result2.get)
          }
          //log.debug(rs.body)

        }
        //easting

      }

      if (started && queue.size > 0) {
        Akka.system.scheduler.scheduleOnce(Duration(300, "millis"), self, "batch")
      } else started = false

    case req: HK1980GRIDtoWGS84Request =>

      val requestor = sender
      import CoordinateInfo._
      val q = qEq(f_northings, req.northings) ++ qEq(f_eastings, req.eastings)
      coordinateInfoDao.findFirst(q).map {
        case Some(item) =>
          log.debug("data exists in db, sending record back")
          val lat = (item._1 \ f_lat).as[Double]
          val long = (item._1 \ f_long).as[Double]
          requestor ! HK1980GRIDtoWGS84Resp(lat, long, req.reqMeta)

        case None =>
          log.info("data does not exist in db, query the web service")
          queue.enqueue((req, requestor))
      }

      // create a non-blocking wait
      Future.successful {
        if (!started) {
          Thread.sleep(100)
          self ! "start"
        }
      }

  }
}