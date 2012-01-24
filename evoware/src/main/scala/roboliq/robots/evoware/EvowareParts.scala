package roboliq.robots.evoware

import roboliq.common

/*sealed class EvowareTipKind(
	val sName: String,
	val nAspirateVolumeMin: Double,
	val nHoldVolumeMax: Double,
	val nWashVolumeExtra: Double
)
*/

/*
sealed abstract class PartModel(val sName: String)
class CarrierModel(sName: String, val nSites: Int, val bCooled: Boolean) extends PartModel(sName)

class RackModel(sName: String, val nRows: Int, val nCols: Int) extends PartModel(sName)
class TroughModel(sName: String, val nRows: Int, val nCols: Int) extends PartModel(sName)
class LabwareModel(sName: String) extends PartModel(sName)

// REFACTOR: choose a better name, perhaps Location, LocationSpec, SiteSpec
class SiteObj(val sName: String, val carrier: CarrierObj, val iSite: Int, val liRoma: Seq[Int]) {
	def iGrid = carrier.iGrid
}

sealed abstract class EvowarePart

class CarrierObj(val sLabel: String, val model: CarrierModel, val iGrid: Int) extends EvowarePart {
	def createSites(s1: String, liRoma: Seq[Int]) = new SiteObj(s1, this, 0, liRoma)
	def createSites(s1: String, s2: String, liRoma: Seq[Int]) = (new SiteObj(s1, this, 0, liRoma), new SiteObj(s2, this, 1, liRoma))
	def createSites(s1: String, s2: String, s3: String, liRoma: Seq[Int]) = (new SiteObj(s1, this, 0, liRoma), new SiteObj(s2, this, 1, liRoma), new SiteObj(s3, this, 2, liRoma))
}
class RackObj(val sLabel: String, val model: RackModel, val site: SiteObj) extends EvowarePart
class TroughObj(val sLabel: String, val model: TroughModel, val site: SiteObj) extends EvowarePart
class PlateObj(val sLabel: String, val model: common.PlateModel, val site: SiteObj) extends EvowarePart {
	private var m_common: common.Plate = null
	
	def commonObj = {
		if (m_common == null) {
			val plate = new common.Plate
			val setup = plate.setup
			setup.model_? = Some(model)
			setup.sLabel_? = Some(sLabel)
			setup.location_? = Some(site.sName)
			setup.setDimension(model.nRows, model.nCols)
			m_common = plate
		}
		m_common
	}
}
*/