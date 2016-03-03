package roboliq.evoware.translator

import scala.collection.mutable
import roboliq.core._
import roboliq.evoware.parser._


class EvowareConfig(
	val carrierData: EvowareCarrierData,
	val tableData: EvowareTableData,
	val configData: EvowareConfigData
) {
	val idToSite_m: Map[String, CarrierGridSiteIndex] = {
		// Get or create an ID for each site on the table
		tableData.carrierIdToGrids_m.toList.flatMap({ case (carrierId, gridIndex_l) =>
			val carrier = carrierData.mapIdToCarrier(carrierId)
			gridIndex_l.flatMap { gridIndex =>
				(0 until carrier.nSites).map(siteIndex => {
					val id0 = f"G${gridIndex}%03dS${siteIndex+1}"
					val id = configData.siteIds.getOrElse(id0, id0)
					id -> CarrierGridSiteIndex(carrierId, gridIndex, siteIndex)
				})
			}
		}).toMap
	}
}
