name := "base"

version := "1.0"

scalaVersion := "2.9.1"

scalacOptions ++= Seq("-unchecked", "-deprecation")

initialCommands in console := """
  import roboliq.core._
"""

libraryDependencies += "commons-io" % "commons-io" % "2.2"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.8" % "test"

libraryDependencies += "org.scalaz" %% "scalaz-core" % "6.0.4"

libraryDependencies += "org.yaml" % "snakeyaml" % "1.10"

//libraryDependencies += "org.scalala" %% "scalala" % "1.0.0.RC3-SNAPSHOT"

libraryDependencies += "com.googlecode.efficient-java-matrix-library" % "ejml" % "0.16"
