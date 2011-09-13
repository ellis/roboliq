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
	//val carrierModels: Iterable[CarrierModel]
	//val labwareModels: Iterable[PartModel]
	val sites: Iterable[SiteObj],
	//val labwares: Iterable[EvowarePart]
	val mapWashProgramArgs: Map[Int, WashProgramArgs]
) {
	//val mapCarrierModels = carrierModels.map(m => m.sName -> m).toMap
	//val mapLabwareModels = labwareModels.map(m => m.sName -> m).toMap
	val mapSites: Map[String, SiteObj] = sites.map(site => site.sName -> site).toMap
}