package doapi.actors.common

import amlibs.core.actor.ActorStack
import play.api.Logger
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import amlibs.core.actor.NamedActorStack

trait IndexingActor extends NamedActorStack {

  def ver:String
  
}

trait JsDataLogging {
  val jsDataLogger = Logger("jsData")
  
  def jdl(tag:String)(js:JsObject) = {
    val jsToLog = js ++ Json.obj("tag" -> tag)
    jsDataLogger.info(jsToLog.toString)
  }  
  
  def jlSpeedmap(js:JsObject) = jdl("speedmap")(js)
    
}