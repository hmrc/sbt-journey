import sbt.*

object LibDependencies {
  val compile: Seq[ModuleID] = Seq(
    "com.typesafe"             % "config"       % "1.4.6",
    "net.sourceforge.plantuml" % "plantuml-asl" % "1.2026.2" % Optional
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"       %% "scalatest"       % "3.2.19",
    "org.scalacheck"      %% "scalacheck"      % "1.19.0",
    "org.scalatestplus"   %% "scalacheck-1-19" % "3.2.19.0",
    "com.vladsch.flexmark" % "flexmark-all"    % "0.64.8"
  ).map(_ % Test)
}
