name := "utils0"

version := "1.0"

scalaVersion := "2.10.0-M5"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

initialCommands in console := """
  import roboliq.core._
"""

libraryDependencies ++= Seq(
	"org.scala-lang" % "scala-compiler" % "2.10.0-M5",
	"org.scala-lang" % "scala-reflect" % "2.10.0-M5",
	"commons-io" % "commons-io" % "2.2",
	"org.scalatest" % "scalatest_2.10.0-M5" % "1.9-2.10.0-M5-B2",
	"org.scalaz" % "scalaz-core_2.10.0-M5" % "6.0.4",
	"org.yaml" % "snakeyaml" % "1.10"
)
