package doapi.actors.traffic

import amlibs.core.actor.ActorStack
import doapi.daos.TrafficSpeedDao
import doapi.daos.TrafficSpeedDataDao
import com.google.inject.Inject
import scala.concurrent.duration.Duration
import play.api.libs.json.Json
import amlibs.core.daos.JsonQueryHelper
import amlibs.core.playspecific.PlayMixin

/**
 * this actor aims to house keep the data for max last 3 days
 */

class TrafficHouseKeepingActor @Inject() (trafficSpeedDao: TrafficSpeedDao, trafficSpeedDataDao: TrafficSpeedDataDao) extends ActorStack with PlayMixin {

  val HOUSEKEEP_INTERVAL = Duration(1, "hour")

  val DAYS = 3
  val TRASH_AFTER_DAYS_Millis = Duration(DAYS, "day").toMillis

  import JsonQueryHelper._

  def ops = {
    case "housekeep" =>
      l.info(s"performing house keeping for $DAYS")
      val q = qLt("created", System.currentTimeMillis() - TRASH_AFTER_DAYS_Millis)
      // remove all expired
      trafficSpeedDao.batchDelete(q) map { le =>
        if (!le.ok) {
          l.error(s"deleteing the trafficSpeedDao with query: $q with errMsg: ${le.errMsg.getOrElse("Unknown")}")
        }
      }

      // remove all data over 3 days
      trafficSpeedDataDao.batchDelete(q) map { le =>
        if (!le.ok) {
          l.error(s"deleteing the trafficSpeedDataDao with query: $q with errMsg: ${le.errMsg.getOrElse("Unknown")}")
        }
      }

      context.system.scheduler.scheduleOnce(HOUSEKEEP_INTERVAL, self, "housekeep")
  }

}