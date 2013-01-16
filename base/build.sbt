name := "base"

version := "1.0"

scalaVersion := "2.10.0"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

initialCommands in console := """
  import scalaz._, Scalaz._, roboliq.core._
"""

resolvers += "io.spray repo" at "http://repo.spray.io"

libraryDependencies ++= Seq(
	"org.scala-lang" % "scala-reflect" % "2.10.0",
	"commons-io" % "commons-io" % "2.2",
	"org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
	"org.scalaz" % "scalaz-core_2.10" % "7.0.0-M7",
	"org.yaml" % "snakeyaml" % "1.10",
	"com.googlecode.efficient-java-matrix-library" % "ejml" % "0.16",
	"io.spray" % "spray-json_2.10" % "1.2.3"
)
