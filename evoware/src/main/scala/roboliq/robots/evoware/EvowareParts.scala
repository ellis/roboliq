package roboliq.robots.evoware

/*sealed class EvowareTipKind(
	val sName: String,
	val nAspirateVolumeMin: Double,
	val nHoldVolumeMax: Double,
	val nWashVolumeExtra: Double
)
*/

sealed abstract class PartModel(val sName: String)
class CarrierModel(sName: String, val nSites: Int, val bCooled: Boolean) extends PartModel(sName)

class RackModel(sName: String, val nRows: Int, val nCols: Int) extends PartModel(sName)
class TroughModel(sName: String, val nRows: Int, val nCols: Int) extends PartModel(sName)
class LabwareModel(sName: String) extends PartModel(sName)
class PlateModel(sName: String, val nRows: Int, val nCols: Int, val nVolume: Double) extends LabwareModel(sName)
//class TubeModel(sName: String, val nVolume: Double) extends LabwareModel(sName)

// REFACTOR: choose a better name, perhaps Location, LocationSpec, SiteSpec
class SiteObj(val sName: String, val carrier: CarrierObj, val iSite: Int) {
	def iGrid = carrier.iGrid
}

sealed abstract class EvowarePart

class CarrierObj(val sLabel: String, val model: CarrierModel, val iGrid: Int) extends EvowarePart {
	def createSites(s1: String) = new SiteObj(s1, this, 0)
	def createSites(s1: String, s2: String) = (new SiteObj(s1, this, 0), new SiteObj(s2, this, 1))
	def createSites(s1: String, s2: String, s3: String) = (new SiteObj(s1, this, 0), new SiteObj(s2, this, 1), new SiteObj(s3, this, 2))
}
class RackObj(val sLabel: String, val model: RackModel, val site: SiteObj) extends EvowarePart
class TroughObj(val sLabel: String, val model: TroughModel, val site: SiteObj) extends EvowarePart
class PlateObj(val sLabel: String, val model: PlateModel, val site: SiteObj) extends EvowarePart
//class TubeObj(sLabel: String, model: TubeModel, rack: RackObj, iRow: Int, iCol: Int)
