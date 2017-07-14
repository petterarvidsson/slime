import ReleaseTransformations._

organization := "se.petterarvidsson"

scalaVersion := "2.12.0"

crossScalaVersions := Seq("2.11.8", "2.12.0")

licenses := Seq("MIT License" -> url("https://opensource.org/licenses/MIT"))

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-core" % "1.2.2",
  "ch.qos.logback" % "logback-classic" % "1.2.2",
  "ch.qos.logback" % "logback-access" % "1.2.2",
  "net.logstash.logback" % "logstash-logback-encoder" % "4.11"
)

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion,
  pushChanges
)
