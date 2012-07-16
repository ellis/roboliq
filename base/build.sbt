name := "base"

version := "1.0"

scalaVersion := "2.10.0-M4"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

initialCommands in console := """
  import roboliq.core._
"""

libraryDependencies += "commons-io" % "commons-io" % "2.2"

//libraryDependencies += "org.scalatest" %% "scalatest" % "1.8" % "test"
libraryDependencies += "org.scalatest" % "scalatest_2.10.0-M4" % "1.9-2.10.0-M4-B2"

//libraryDependencies += "org.scalaz" %% "scalaz-core" % "6.0.4"
libraryDependencies += "org.scalaz" % "scalaz-core_2.10.0-M4" % "6.0.4"

libraryDependencies += "org.yaml" % "snakeyaml" % "1.10"

libraryDependencies += "com.googlecode.efficient-java-matrix-library" % "ejml" % "0.16"
