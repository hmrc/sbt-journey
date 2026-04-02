import sbt._

lazy val plugin = Project("sbt-journey", file("."))
  .enablePlugins(SbtPlugin)
  .settings(CodeCoverageSettings.settings*)
  .settings(
    majorVersion     := 0,
    scalaVersion     := "2.12.20",
    isPublicArtefact := true,
    libraryDependencies ++= LibDependencies.compile ++ LibDependencies.test,
    addSbtPlugin("org.playframework" %% "sbt-plugin"  % "3.0.9"),
    addSbtPlugin("com.github.sbt"     % "sbt2-compat" % "0.1.0"),
    scriptedLaunchOpts ++= {
      Seq(
        "-Xmx1024M",
        "-Dplugin.version=" + version.value
      )
    },
    scriptedBufferLog := false,
    // This config is in preparation for sbt2 cross-building, but this won't work
    // until Play Framework publishes sbt2 versions of their plugins.
    crossScalaVersions += "3.8.1",
    (pluginCrossBuild / sbtVersion) := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.5.8"
        case _      => "2.0.0-RC9"
      }
    },
    scriptedSbt := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.11.6"
        case _      => "2.0.0-RC9"
      }
    }
  )
