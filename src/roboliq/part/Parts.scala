package roboliq.part

sealed class AspirateStrategy(val sName: String)
sealed class DispenseStrategy(val sName: String, val bEnter: Boolean)

class Tip(val index: Int, val nVolumeMin: Double, val nVolumeMax: Double)

class Liquid(val sName: String, val bContaminates: Boolean)
object Liquid {
	val empty = new Liquid(null, false)
}

class Thing {
	var parent: Option[Thing] = None
}

trait WellHolder {
	val index: Int
	val nRows: Int
	val nCols: Int
	val wells: Array[Well]
}

class Well(val holder: WellHolder, val index: Int) extends Thing {
	var liquid: Liquid = null
	var nVolume = 0.0
}
object Well {
	def fill(wells: Seq[Well], liquid: Liquid, nVolume: Double) {
		for (well <- wells) {
			well.liquid = liquid
			well.nVolume = nVolume
		}
	}
}

class WellState(val well: Well) {
	var liquid: Liquid = Liquid.empty
	var nVolume = 0.0
}

class Plate(val index: Int, val nRows: Int, val nCols: Int) extends Thing with WellHolder {
	val nWells = nRows * nCols
	val wells: Array[Well] = {
		(0 until nWells).map(i => new Well(this, i)).toArray
	}
}
