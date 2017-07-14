import ReleaseTransformations._

lazy val commonSettings = Seq(
  version := "0.2.0",
  scalaVersion := "2.12.2",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
  crossScalaVersions := Seq("2.11.8", "2.12.2"),
  organization := "com.unstablebuild",
  licenses := Seq("MIT License" -> url("https://opensource.org/licenses/MIT")),
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-api" % "1.7.25",
    "ch.qos.logback" % "logback-core" % "1.2.2",
    "ch.qos.logback" % "logback-classic" % "1.2.2",
    "ch.qos.logback" % "logback-access" % "1.2.2",
    "net.logstash.logback" % "logstash-logback-encoder" % "4.11",
    "com.chuusai" %% "shapeless" % "2.3.2",
    "org.scalameta" %% "scalameta" % "1.8.0",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value
  ),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
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
)

lazy val macros = project.in(file("macros"))
  .settings(commonSettings: _*)
  .settings(
    name := "macros"
  )

lazy val root = project.in(file("."))
  .settings(commonSettings: _*)
  .dependsOn(macros)
  .settings(
    name := "blah"
  )
