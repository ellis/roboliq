import sbt._

object MyBuild extends Build {
	lazy val projBase = Project("base", file("base"))

	/*
	lazy val projCommon = Project("common", file("common"))

	lazy val projCommands = Project("commands", file("commands")) dependsOn(projCommon)

	lazy val projCompiler = Project("compiler", file("compiler")) dependsOn(projCommon, projCommands)

	lazy val projDevices = Project("devices", file("devices")) dependsOn(projCommon, projCommands, projCompiler)

	lazy val projRoboease = Project("roboease", file("roboease")) dependsOn(projCommon, projCommands, projCompiler, projDevices)

	//lazy val projEvoware = Project("evoware", file("evoware")) dependsOn(projCommon, projCommands, projCompiler, projDevices, projRoboease)
	*/

	lazy val projEvoware = Project("evoware", file("evoware")) dependsOn(projBase)

	//lazy val projRoboeaseEvoware = Project("roboease-evoware", file("roboease-evoware")) dependsOn(projCommon, projCommands, projCompiler, projDevices, projRoboease, projEvoware)

	//lazy val projBsse = Project("bsse", file("bsse")) dependsOn(projCommon, projCommands, projCompiler, projDevices, projEvoware)
	lazy val projBsse = Project("bsse", file("bsse")) dependsOn(projBase, projEvoware)
	
	//lazy val projTest = Project("test", file("test")) dependsOn(projCommon, projCommands, projCompiler, projDevices, projRoboease, projEvoware, projBsse, projRoboeaseEvoware)

	lazy val projUtils0 = Project("utils0", file("utils0"))
	
	//scalacOptions <++= Seq("-unchecked")
}

