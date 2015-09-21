scalaVersion := "2.10.0"
 
resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
 
libraryDependencies ++= Seq(
	"org.scalaz" % "scalaz-core_2.10" % "7.0.0-M7"
)
 
scalacOptions += "-feature"
 
initialCommands in console := "import scalaz._, Scalaz._"
