package roboliq.robots.evoware

import scala.collection.mutable
import roboliq.core._
import roboliq.entity._


class EvowareConfig(
	val carrier: EvowareCarrierData,
	val table: EvowareTableData,
	val config: EvowareConfigData
) {
	val idToSite_m: Map[String, CarrierSite] = {
		val gridToCarrier_m = table.mapCarrierToGrid.map(pair => pair._2 -> pair._1)
		
		// Get or create an ID for each site on the table
		table.mapCarrierToGrid.toList.flatMap(pair => {
			val (carrier, grid_i) = pair
			(0 until carrier.nSites).map(site_i => {
				val gridSite = (grid_i, site_i)
				val id0 = f"G${grid_i}%03dS${site_i+1}"
				val id = config.siteIds.getOrElse(id0, id0)
				id -> CarrierSite(carrier, site_i)
			})
		}).toMap
	}
}
