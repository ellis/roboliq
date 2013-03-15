package roboliq.robots.evoware

import roboliq.core._
import roboliq.entity._


class EvowareConfig(
	val tableFile: EvowareTableData,
	val mapLabelToSite: Map[String, CarrierSite]
) {
	def toCoreEntities: List[Entity] = {
		val carrier = tableFile.configFile
		val table = tableFile
		
		val plateModel_m = carrier.mapNameToLabwareModel.map(pair => {
			val (id, labware) = pair
			id -> PlateModel(id, labware.nRows, labware.nCols, LiquidVolume.ul(labware.ul))
		})
		val plateModel_l = plateModel_m.values.toList.sortBy(_.id)
		
		val plateLocation_l = mapLabelToSite.toList.map(pair => {
			val (id, carrierSite) = pair
			val sitepair = (carrierSite.carrier.id, carrierSite.iSite)
			val labware_l = carrier.mapNameToLabwareModel.values.filter(labware => labware.sites.contains(sitepair)).toList
			val plateModel_l = labware_l.map(labware => plateModel_m(labware.sName))
			PlateLocation(id, plateModel_l, false)
		})
		
		plateModel_l ++ plateLocation_l
	}
}
