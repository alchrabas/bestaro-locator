name := "bestaro-locator"

lazy val root = (project in file("."))
  .settings(
    name := "bestaro-locator",
    organization := "bestaro-locator",
    version := "0.9.6",
    scalaVersion := "2.12.4",
    resolvers += "jitpack" at "https://jitpack.io",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.6.3",
      "com.beachape" %% "enumeratum" % "1.5.12",
      "com.beachape" %% "enumeratum-play-json" % "1.5.12-2.6.0-M7",
      "org.carrot2" % "morfologik-polish" % "2.1.8",
      "com.google.guava" % "guava" % "23.0",
      "com.google.maps" % "google-maps-services" % "0.2.1",
      "com.typesafe.slick" %% "slick" % "3.3.3",
      "com.opencsv" % "opencsv" % "5.6",
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
