name := """get-started-scala"""
organization := "com.ponkotuy"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(
  guice,
  "org.skinny-framework" %% "skinny-orm" % "2.4.0",
  "org.scalikejdbc" %% "scalikejdbc-play-initializer" % "2.6.0",
  "mysql" % "mysql-connector-java" % "6.0.6",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0" % Test
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.ponkotuy.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.ponkotuy.binders._"
