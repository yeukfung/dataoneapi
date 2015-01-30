object jsontest {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(61); 
  println("Welcome to the Scala worksheet")
  import play.api.libs.json._

  object JsFlattener {

    val removeId = (__ \ "_id").json.prune

    def apply(js: JsValue): JsValue = flatten(js).foldLeft(JsObject(Nil))(_ ++ _.as[JsObject])

    def flatten(js: JsValue, prefix: String = ""): Seq[JsValue] = {
      js.as[JsObject].fieldSet.toSeq.flatMap {
        case (key, values) =>
          values match {
            case JsBoolean(x) => Seq(Json.obj(concat(prefix, key) -> x))
            case JsNumber(x)  => Seq(Json.obj(concat(prefix, key) -> x))
            case JsString(x)  => Seq(Json.obj(concat(prefix, key) -> x))
            case JsArray(seq) => Seq(Json.obj(concat(prefix, key) -> JsArray(seq)))
            case x: JsObject  => flatten(x, concat(prefix, key))
            case _            => Seq(Json.obj(concat(prefix, key) -> JsNull))
          }
      }
    }

    def concat(prefix: String, key: String): String = if (prefix.nonEmpty) s"${prefix}${key.capitalize}" else key

  };$skip(1146); 

  val json = Json.parse(
    """
    {"meta":{"startLongLat":[114.187422872,22.314083818],"endLongLat":[114.184084134,22.309177485],"regionName":"Kowloon","linkName":"34372-3450"}}
  """);System.out.println("""json  : play.api.libs.json.JsValue = """ + $show(json ));$skip(21); val res$0 = 

  JsFlattener(json)


import java.text.SimpleDateFormat
import java.util.{Date, TimeZone};System.out.println("""res0: play.api.libs.json.JsValue = """ + $show(res$0));$skip(107); 

val localTz = TimeZone.getDefault();System.out.println("""localTz  : java.util.TimeZone = """ + $show(localTz ));$skip(64); 
val currentOffset = localTz.getOffset(System.currentTimeMillis);System.out.println("""currentOffset  : Int = """ + $show(currentOffset ));$skip(75); 
val fmtFromLocal = new SimpleDateFormat("hh:mm a z");System.out.println("""fmtFromLocal  : java.text.SimpleDateFormat = """ + $show(fmtFromLocal ));$skip(47);  // z parses time zone
val fmtToGmt = new SimpleDateFormat("hh:mm a");System.out.println("""fmtToGmt  : java.text.SimpleDateFormat = """ + $show(fmtToGmt ));$skip(154); 
def toGmt(t: String): String = {
  val time = fmtFromLocal.parse(t).getTime()
  val timeUtc = time + currentOffset
  fmtToGmt.format(new Date(timeUtc))
};System.out.println("""toGmt: (t: String)String""");$skip(22); 

val now = new Date();System.out.println("""now  : java.util.Date = """ + $show(now ));$skip(20); val res$1 = 
toGmt(now.toString);System.out.println("""res1: String = """ + $show(res$1))}
}
