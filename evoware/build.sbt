name := "roboliq-evoware"

version := "1.0"

scalaVersion := "2.9.0-1"

scalacOptions ++= Seq("-unchecked", "-deprecation")

initialCommands in console := """
  import roboliq.core._
  import roboliq.robots.evoware._
"""
