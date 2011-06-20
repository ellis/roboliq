import sbt._

object MyBuild extends Build {
	lazy val projCommon = Project("common", file("common"))

	lazy val projEvoware = Project("evoware", file("evoware")) dependsOn(projCommon)

	lazy val projBsse = Project("bsse", file("bsse")) dependsOn(projCommon) dependsOn(projEvoware)

	lazy val projLevel2 = Project("level2", file("level2")) dependsOn(projCommon)
}

