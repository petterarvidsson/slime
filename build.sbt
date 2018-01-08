val commonSettings = Seq(
  version := "0.2.1",
  scalaVersion := "2.12.3",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
  crossScalaVersions := Seq("2.12.3"),
  organization := "com.unstablebuild",
  homepage := Some(url("https://github.com/petterarvidsson/slime")),
  organizationHomepage := Some(url("https://github.com/petterarvidsson/slime")),
  licenses := Seq("MIT License" -> url("https://opensource.org/licenses/MIT")),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
  libraryDependencies ++= Seq(
    "com.chuusai" %% "shapeless" % "2.3.2",
    "ch.qos.logback" % "logback-classic" % "1.2.3" % "provided,test",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "com.typesafe.play" %% "play-json" % "2.6.0" % "test"
  ),
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  pomIncludeRepository := (_ => false),
  pomExtra :=
    <scm>
      <url>git@github.com:petterarvidsson/slime.git</url>
      <connection>scm:git:git@github.com:petterarvidsson/slime.git</connection>
    </scm>
    <developers>
      <developer>
        <id>petterarvidsson</id>
        <name>Petter Arvidsson</name>
        <url>https://github.com/petterarvidsson</url>
      </developer>
      <developer>
        <id>hansjoergschurr</id>
        <name>Hans-Jörg Schurr</name>
        <url>https://github.com/hansjoergschurr</url>
      </developer>
      <developer>
        <id>lucastorri</id>
        <name>Lucas Torri</name>
        <url>http://unstablebuild.com</url>
      </developer>
      <developer>
        <id>hcwilhelm</id>
        <name>Hans Christian Wilhelm</name>
        <url>https://github.com/hcwilhelm</url>
      </developer>
    </developers>
)

lazy val macros = project
  .in(file("macros"))
  .settings(commonSettings: _*)
  .settings(
    name := "slime-macros",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scalameta" %% "scalameta" % "1.8.0"
    )
  )

lazy val root = project
  .in(file("."))
  .settings(commonSettings: _*)
  .dependsOn(macros)
  .settings(name := "slime")
