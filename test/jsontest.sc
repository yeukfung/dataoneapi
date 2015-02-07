object jsontest {
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet

  import play.api.libs.json._
  import play.api.libs.json.Reads._
  import play.api.libs.functional.syntax._

  val js = Json.obj("_id" -> "dummy", "a" -> 123, "b" -> 345)
                                                  //> js  : play.api.libs.json.JsObject = {"_id":"dummy","a":123,"b":345}

  def tConcat(a: String, b: String) = ((__ \ a).json.pick and
    (__ \ b).json.pick) reduce                    //> tConcat: (a: String, b: String)play.api.libs.json.Reads[play.api.libs.json.J
                                                  //| sArray]

  def tPrune(fld: String) = (__ \ fld).json.prune //> tPrune: (fld: String)play.api.libs.json.Reads[play.api.libs.json.JsObject]

  val trans = (tPrune("_id") and
    (__ \ "ab").json.copyFrom(tConcat("a", "b"))) reduce
                                                  //> trans  : play.api.libs.json.Reads[play.api.libs.json.JsObject] = play.api.li
                                                  //| bs.json.Reads$$anon$8@48e4ec18

  js.transform(trans).get                         //> res0: play.api.libs.json.JsObject = {"a":123,"b":345,"ab":[123,345]}
}