object jsontest {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(61); 
  println("Welcome to the Scala worksheet")

  import play.api.libs.json._
  import play.api.libs.json.Reads._
  import play.api.libs.functional.syntax._;$skip(173); 

  val js = Json.obj("_id" -> "dummy", "a" -> 123, "b" -> 345);System.out.println("""js  : play.api.libs.json.JsObject = """ + $show(js ));$skip(94); 

  def tConcat(a: String, b: String) = ((__ \ a).json.pick and
    (__ \ b).json.pick) reduce;System.out.println("""tConcat: (a: String, b: String)play.api.libs.json.Reads[play.api.libs.json.JsArray]""");$skip(51); 

  def tPrune(fld: String) = (__ \ fld).json.prune;System.out.println("""tPrune: (fld: String)play.api.libs.json.Reads[play.api.libs.json.JsObject]""");$skip(91); 

  val trans = (tPrune("_id") and
    (__ \ "ab").json.copyFrom(tConcat("a", "b"))) reduce;System.out.println("""trans  : play.api.libs.json.Reads[play.api.libs.json.JsObject] = """ + $show(trans ));$skip(27); val res$0 = 

  js.transform(trans).get;System.out.println("""res0: play.api.libs.json.JsObject = """ + $show(res$0))}
}
