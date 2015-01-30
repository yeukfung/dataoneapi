package amlibs.core.utils

import scala.concurrent.Future
import play.api.libs.ws.WS
import play.api.libs.ws.WSResponse
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.Application
import amlibs.core.playspecific.PlayMixin
import java.text.SimpleDateFormat
import java.util.Date

trait ESMixin extends PlayMixin {

  val timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

  def toESTimeStamp(date: Date): String = {
    timestampFormat.format(date)
  }

  val esUrl = conf.getString("es.url").getOrElse("http://localhost:9200")
  val esClient = new ESClient(esUrl)

}

class ESClient(esURL: String) extends PlayMixin {

  private def baseUrl(idx: String, t: String, action: String) = esURL + s"/$idx/$t/$action".replaceAll("///", "/").replaceAll("//", "/")

  def bulk(index: Option[String] = None, t: Option[String] = None, data: JsObject): Future[WSResponse] = {
    val url = baseUrl(index.getOrElse(""), t.getOrElse(""), "_bulk")
    WS.url(url).post(data)
  }

  def count(indices: Seq[String], types: Seq[String], query: String): Future[WSResponse] = {
    val url = baseUrl(indices.mkString(","), types.mkString(","), "_count")
    WS.url(url).get
  }

  def createIndex(name: String, settings: Option[JsObject] = None): Future[WSResponse] = {
    val url = s"$esURL/$name"
    settings match {
      case Some(js) => WS.url(url).put(js)
      case None     => WS.url(url).put(Json.obj())
    }
  }

  def deleteIndex(name: String): Future[WSResponse] = {
    val url = s"$esURL/$name"
    WS.url(url).delete()
  }

  def get(index: String, `type`: String, id: String): Future[WSResponse] = {
    val url = baseUrl(index, `type`, id)
    WS.url(url).get
  }

  def getMapping(indices: Seq[String], types: Seq[String]): Future[WSResponse] = {
    val url = baseUrl(indices.mkString(","), "_mapping", types.mkString(","))
    WS.url(url).get
  }

  def index(index: String, `type`: String, id: String, data: JsObject, refresh: Boolean = false): Future[WSResponse] = {
    val url = baseUrl(index, `type`, id)
    if (id == "_mapping") l.info(s"url = $url with data: $data")
    WS.url(url).put(data)
  }

  def refresh(index: String) = {
    val url = s"$esURL/$index"
    WS.url(url).post(Json.obj())
  }

  def search(index: String, query: JsObject): Future[WSResponse] = {
    val url = s"$esURL/$index/_search"
    WS.url(url).post(query)
  }

  def deleteByQuery(index: String, t: String, queryStr: String): Future[WSResponse] = {
    val url = baseUrl(index, t, s"_query?q=$queryStr")
    WS.url(url).delete()
  }
}