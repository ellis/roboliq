import sbt._

object MyBuild extends Build {
	lazy val projCommon = Project("common", file("common"))

	lazy val projEvoware = Project("evoware", file("evoware")) dependsOn(projCommon, projCommands)

	lazy val projBsse = Project("bsse", file("bsse")) dependsOn(projCommon, projCommands, projEvoware)

	lazy val projCommands = Project("commands", file("commands")) dependsOn(projCommon)

	lazy val projLevel2 = Project("level2", file("level2")) dependsOn(projCommon)

	lazy val projLevel3 = Project("level3", file("level3")) dependsOn(projCommon, projCommands)

	lazy val projDevices = Project("devices", file("devices")) dependsOn(projCommon, projCommands, projLevel3)

	lazy val projRoboease = Project("roboease", file("roboease")) dependsOn(projCommon, projLevel3)

	lazy val projTestBsseLevel2 = Project("test-bsse-level2", file("test-bsse-level2")) dependsOn(projBsse, projLevel2)

	//scalacOptions <++= Seq("-unchecked")
}

