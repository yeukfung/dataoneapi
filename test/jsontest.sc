object jsontest {
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet
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

  }

  val json = Json.parse(
    """
    {"meta":{"startLongLat":[114.187422872,22.314083818],"endLongLat":[114.184084134,22.309177485],"regionName":"Kowloon","linkName":"34372-3450"}}
  """)                                            //> json  : play.api.libs.json.JsValue = {"meta":{"startLongLat":[114.187422872
                                                  //| ,22.314083818],"endLongLat":[114.184084134,22.309177485],"regionName":"Kowl
                                                  //| oon","linkName":"34372-3450"}}

  JsFlattener(json)                               //> res0: play.api.libs.json.JsValue = {"metaStartLongLat":[114.187422872,22.31
                                                  //| 4083818],"metaEndLongLat":[114.184084134,22.309177485],"metaRegionName":"Ko
                                                  //| wloon","metaLinkName":"34372-3450"}


import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

val localTz = TimeZone.getDefault()               //> localTz  : java.util.TimeZone = sun.util.calendar.ZoneInfo[id="Asia/Hong_Ko
                                                  //| ng",offset=28800000,dstSavings=0,useDaylight=false,transitions=71,lastRule=
                                                  //| null]
val currentOffset = localTz.getOffset(System.currentTimeMillis)
                                                  //> currentOffset  : Int = 28800000
val fmtFromLocal = new SimpleDateFormat("hh:mm a z") // z parses time zone
                                                  //> fmtFromLocal  : java.text.SimpleDateFormat = java.text.SimpleDateFormat@2b8
                                                  //| 0f9b5
val fmtToGmt = new SimpleDateFormat("hh:mm a")    //> fmtToGmt  : java.text.SimpleDateFormat = java.text.SimpleDateFormat@3264901
                                                  //| b
def toGmt(t: String): String = {
  val time = fmtFromLocal.parse(t).getTime()
  val timeUtc = time + currentOffset
  fmtToGmt.format(new Date(timeUtc))
}                                                 //> toGmt: (t: String)String

val now = new Date()                              //> now  : java.util.Date = Fri Jan 30 18:02:11 HKT 2015
toGmt(now.toString)                               //> java.text.ParseException: Unparseable date: "Fri Jan 30 18:02:11 HKT 2015"
                                                  //| 	at java.text.DateFormat.parse(DateFormat.java:357)
                                                  //| 	at jsontest$$anonfun$main$1.toGmt$1(jsontest.scala:45)
                                                  //| 	at jsontest$$anonfun$main$1.apply$mcV$sp(jsontest.scala:51)
                                                  //| 	at org.scalaide.worksheet.runtime.library.WorksheetSupport$$anonfun$$exe
                                                  //| cute$1.apply$mcV$sp(WorksheetSupport.scala:76)
                                                  //| 	at org.scalaide.worksheet.runtime.library.WorksheetSupport$.redirected(W
                                                  //| orksheetSupport.scala:65)
                                                  //| 	at org.scalaide.worksheet.runtime.library.WorksheetSupport$.$execute(Wor
                                                  //| ksheetSupport.scala:75)
                                                  //| 	at jsontest$.main(jsontest.scala:1)
                                                  //| 	at jsontest.main(jsontest.scala)
}