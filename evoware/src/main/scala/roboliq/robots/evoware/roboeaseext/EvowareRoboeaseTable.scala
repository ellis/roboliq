package roboliq.robots.evoware.roboeaseext

import roboliq.robots.evoware
import roboliq.roboease

trait EvowareRoboeaseTable {
	val roboeaseTable: roboease.Table
	val sEvowareHeader: String
	val evowareSites: Seq[evoware.LabwareObject]
}
/*
object Converter {
	def convertTable(id: String, tE: evoware.EvowareTable): roboease.Table = {
		val racksR = tE.sites.map(site => {
			new roboease.Rack(site.sName, site.carrier.model.)
		name: String,
		nRows: Int,
		nCols: Int,
		grid: Int, site: Int, nVolumeMax: Double, carrierType: String
		})
	}
}*/