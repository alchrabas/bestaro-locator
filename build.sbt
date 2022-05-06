name := "bestaro-locator"

version := "0.9.4"

scalaVersion := "2.12.4"

lazy val commonSettings = Seq(
  organization := "bestaro-locator",
  version := "0.9.4",
  scalaVersion := "2.12.4",
  resolvers += "jitpack" at "https://jitpack.io"
)

lazy val root = (project in file("."))
  .settings(
    commonSettings,
    name := "bestaro-locator",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.9.2",
      "com.beachape" %% "enumeratum" % "1.7.0",
      "com.beachape" %% "enumeratum-play-json" % "1.7.0",
      "org.carrot2" % "morfologik-polish" % "2.1.8",
      "com.google.guava" % "guava" % "23.0",
      "com.google.maps" % "google-maps-services" % "0.2.1",
      "com.typesafe.slick" %% "slick" % "3.3.3",
      "com.github.tototoshi" %% "scala-csv" % "1.3.9",
      "org.xerial" % "sqlite-jdbc" % "3.8.9",
      "org.scalactic" %% "scalactic" % "3.2.9",
      "org.scalatest" %% "scalatest" % "3.2.9" % "test",
      "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % Test
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
