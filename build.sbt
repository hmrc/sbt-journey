import sbt._

lazy val library = Project("sbt-gen-journey", file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    majorVersion     := 0,
    scalaVersion     := "2.12.18",
    isPublicArtefact := true,
    libraryDependencies ++= LibDependencies.compile ++ LibDependencies.test
  )
