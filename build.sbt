name := "bestaro-locator"

version := "0.9.2"

scalaVersion := "2.12.4"

lazy val commonSettings = Seq(
  organization := "bestaro-locator",
  version := "0.9.2",
  scalaVersion := "2.12.4",
  resolvers += "jitpack" at "https://jitpack.io"
)

lazy val root = (project in file("."))
  .settings(
    commonSettings,
    name := "bestaro-locator",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.6.3",
      "com.beachape" %% "enumeratum" % "1.5.12",
      "com.beachape" %% "enumeratum-play-json" % "1.5.12-2.6.0-M7",
      "org.carrot2" % "morfologik-polish" % "2.1.3",
      "com.google.guava" % "guava" % "23.0",
      "com.google.maps" % "google-maps-services" % "0.2.1",
      "com.typesafe.slick" %% "slick" % "3.2.1",
      "com.github.tototoshi" %% "scala-csv" % "1.3.4",
      "org.xerial" % "sqlite-jdbc" % "3.20.1",
      "org.scalactic" %% "scalactic" % "3.0.1",
      "org.scalatest" %% "scalatest" % "3.0.1" % "test",
      "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % Test
    )
  )

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

scmInfo := Some(ScmInfo(
  url("https://github.com/alchrabas/bestaro-locator.git"),
  "scm:git:git@github.com:alchrabas/bestaro-locator.git",
  Some("scm:git:git@github.com:alchrabas/bestaro-locator.git")))


pomExtra :=
  <developers>
    <developer>
      <id>alchrabas</id>
      <name>Aleksander Chrabaszcz</name>
      <url>https://github.com/alchrabas/</url>
    </developer>
  </developers>
