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
		carrier.mapNameToLabwareModel.toList.map(pair => {
			val (id, labware) = pair
			PlateModel(id, labware.nRows, labware.nCols, LiquidVolume.ul(labware.ul))
		})
	}
}
