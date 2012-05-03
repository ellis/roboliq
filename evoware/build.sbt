name := "roboliq-evoware"

version := "1.0"

scalaVersion := "2.9.1"

scalacOptions ++= Seq("-unchecked", "-deprecation")

initialCommands in console := """
  import roboliq.core._
  import roboliq.robots.evoware._
"""

libraryDependencies += "commons-io" % "commons-io" % "2.2"

libraryDependencies += "org.scalatest" % "scalatest_2.9.0" % "1.6.1" % "test"

libraryDependencies += "org.scalaz" %% "scalaz-core" % "6.0.4"

libraryDependencies += "org.yaml" % "snakeyaml" % "1.10"
