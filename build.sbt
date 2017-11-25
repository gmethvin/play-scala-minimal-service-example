import Dependencies._

lazy val root = (project in file("."))
  .enablePlugins(PlayService)
  .settings(
    scalaVersion := "2.12.4",
    libraryDependencies ++= Seq(
      akkaHttpServer,
      logback,
      scalaTest % Test
    )
  )
