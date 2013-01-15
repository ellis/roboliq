name := "utils0"

version := "1.0"

scalaVersion := "2.10.0"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

initialCommands in console := "import scalaz._, Scalaz._"

//resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
resolvers += "io.spray repo" at "http://repo.spray.io"

libraryDependencies ++= Seq(
	"org.scala-lang" % "scala-compiler" % "2.10.0",
	"org.scala-lang" % "scala-reflect" % "2.10.0",
	"commons-io" % "commons-io" % "2.2",
	"org.scalatest" % "scalatest_2.10" % "1.9.1",
	"org.scalaz" % "scalaz-core_2.10" % "7.0.0-M7",
	"com.google.code.gson" % "gson" % "2.2.1",
	"org.yaml" % "snakeyaml" % "1.10",
	"io.spray" % "spray-json_2.10" % "1.2.3" //cross CrossVersion.full
)
