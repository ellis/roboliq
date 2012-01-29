package roboliq.robots.evoware

import roboliq.compiler.RoboliqSystem

/*class EvowarePartModels(
) {
}

class EvowareTable(
) {
	val mapSites: Map[String, SiteObj] = sites.map(site => site.sName -> site).toMap
}*/

class EvowareConfig(
	val tableFile: EvowareTableFile,
	val mapLabelToSite: Map[String, CarrierSite]//,
	//val mapLabelToLabware: Map[String, LabwareObject]//,
	//val mapWashProgramArgs: Map[Int, WashProgramArgs]
) {
}