lazy val commonSettings = Seq(
  version := "0.2.0",
  scalaVersion := "2.12.0",
  crossScalaVersions := Seq("2.11.8", "2.12.0"),
  organization := "blah",
  organizationName := "bleh",
  licenses := Seq("MIT License" -> url("https://opensource.org/licenses/MIT")),
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-api" % "1.7.25",
    "ch.qos.logback" % "logback-core" % "1.2.2",
    "ch.qos.logback" % "logback-classic" % "1.2.2",
    "ch.qos.logback" % "logback-access" % "1.2.2",
    "net.logstash.logback" % "logstash-logback-encoder" % "4.11"
  ),
  publishMavenStyle := true
)

lazy val root = project.in(file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "blah"
  )
