package doapi.daos

import amlibs.core.daos.JsObjectDao

class TrafficSpeedDao extends JsObjectDao {
  val dbName = "trafficspeeds"
}

class TrafficLinkDao extends JsObjectDao {
  val dbName = "trafficlinks"
}

class CoordinateInfoDao extends JsObjectDao {
  val dbName = "coordinateinfos"
}