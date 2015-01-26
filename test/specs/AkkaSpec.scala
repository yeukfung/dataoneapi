package specs

import org.specs2.mutable.After
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import play.api.libs.concurrent.Akka
import play.api.Play.current
import org.specs2.mutable.SpecificationLike
import play.api.test.WithApplication
import amlibs.core.daos.JsObjectDao
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.Awaitable
import play.api.test.FakeApplication
import play.api.Play
import scala.concurrent.Future
import amlibs.core.daos.JsonQueryHelper
/* A tiny class that can be used as a Specs2 'context'. 
 * From: http://blog.xebia.com/2012/10/01/testing-akka-with-specs2/
 * I don't think we need it as we will be testing under Play context, 
 * but I leave this here just in case, i also cannot seems to get this
 * working with Play as it fail to pick up the configuration from
 * application.conf
 * */
object TestGlobal {
  var started = false
}
abstract class AkkaSpec(actorSysName: String) extends TestKit(ActorSystem(actorSysName))
  with ImplicitSender
  with SpecificationLike {

  def localStart = if (!TestGlobal.started) step(Play.start(FakeApplication()))
  def localStop = if (!TestGlobal.started) step(Play.stop)

}

trait ShortCodeUtil {
  def r[T](aw: Awaitable[T])(implicit dur: Duration): T = {
    Await.result(aw, dur);
  }

  def tc[T](f: => T) {
    try {
      f
    } catch {
      case e: Exception => println(s"ex: ${e.getMessage()}")
    }
  }
  
  def nbWait(sec: Int): Future[Boolean] = {
    Future.successful {
      Thread.sleep(sec * 1000)
      true
    }
  }

}

trait TestDBMixin extends ShortCodeUtil {
  
  
  def dbName = "dummy"

  implicit val dur = Duration(5, "seconds")
  import scala.concurrent.ExecutionContext.Implicits._

  def dbs: List[JsObjectDao]

  def dropAllDBs() { dbs.foreach { db => tc(r(db.coll.drop())) } }

}