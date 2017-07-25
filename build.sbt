import ReleaseTransformations._

val commonSettings = Seq(
  version := "0.2.0",
  scalaVersion := "2.12.2",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
  crossScalaVersions := Seq("2.12.2"),
  organization := "com.unstablebuild",
  licenses := Seq("MIT License" -> url("https://opensource.org/licenses/MIT")),
  libraryDependencies ++= Seq(
    "org.scalameta" %% "scalameta" % "1.8.0",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "ch.qos.logback" % "logback-classic" % "1.2.3" % "provided,test",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test"
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

lazy val macros = project
  .in(file("macros"))
  .settings(commonSettings: _*)
  .settings(name := "slime-macros")

lazy val root = project
  .in(file("."))
  .settings(commonSettings: _*)
  .dependsOn(macros)
  .settings(name := "slime")
