import sbt._

object MyBuild extends Build {
	lazy val projCommon = Project("common", file("common"))

	lazy val projCommands = Project("commands", file("commands")) dependsOn(projCommon)

	lazy val projCompiler = Project("compiler", file("compiler")) dependsOn(projCommon, projCommands)

	lazy val projDevices = Project("devices", file("devices")) dependsOn(projCommon, projCommands, projCompiler)

	lazy val projRoboease = Project("roboease", file("roboease")) dependsOn(projCommon, projCommands, projCompiler, projDevices)

	lazy val projEvoware = Project("evoware", file("evoware")) dependsOn(projCommon, projCommands)

	lazy val projBsse = Project("bsse", file("bsse")) dependsOn(projCommon, projCommands, projEvoware)

	//lazy val projLevel2 = Project("level2", file("level2")) dependsOn(projCommon)

	//lazy val projTestBsseLevel2 = Project("test-bsse-level2", file("test-bsse-level2")) dependsOn(projBsse, projLevel2)

	//scalacOptions <++= Seq("-unchecked")
}

