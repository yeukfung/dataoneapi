name := """dataonehk"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.5"

lazy val njRepo = Seq(
    "Mandubian repository snapshots" at "https://github.com/mandubian/mandubian-mvn/raw/master/snapshots/",
    "Mandubian repository releases" at "https://github.com/mandubian/mandubian-mvn/raw/master/releases/",
    "Mandubian bintray repository releases" at "http://dl.bintray.com/mandubian/maven",
    "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    "sonarepo release" at "https://oss.sonatype.org/content/repositories/releases/")

resolvers in ThisBuild ++= njRepo
    
libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "com.mandubian"     %% "play-json-zipper"  % "1.2"                      ,
  "com.typesafe.play" %% "play-json"         % "2.3.7"                     ,
  "org.specs2"        %% "specs2"            % "2.3.12"         % "test"  ,
  "com.typesafe.akka" %% "akka-testkit" % "2.3.7" % "test",
  "com.google.inject" % "guice" % "4.0-beta5",
  "ws.securesocial" %% "securesocial" % "3.0-M1"
)

libraryDependencies += "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23"

libraryDependencies += "org.apache.poi" % "poi" % "3.9"

libraryDependencies += "org.seleniumhq.selenium" % "selenium-java" % "2.39.0"

libraryDependencies += "io.spray" %%  "spray-json" % "1.3.1"