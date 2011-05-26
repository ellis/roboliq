package roboliq.parts

sealed class AspirateStrategy(val sName: String)
sealed class DispenseStrategy(val sName: String, val bEnter: Boolean)

class Tip(val index: Int, val nVolumeMin: Double, val nVolumeMax: Double)

class Liquid(val sName: String, val bContaminates: Boolean)
object Liquid {
	val empty = new Liquid(null, false)
}

class Part {
}

sealed class Site(val parent: Part, val index: Int)

trait WellHolder extends Part {
	val nRows: Int
	val nCols: Int
	val nWells: Int
	val wells: Array[Well]
	val index = WellHolder.nextIndex
	
	WellHolder.nextIndex += 1
}
object WellHolder {
	var nextIndex = 0
}

class Well(val holder: WellHolder, val index: Int) extends Part {
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

class Plate(val nRows: Int, val nCols: Int) extends Part with WellHolder {
	val nWells = nRows * nCols
	val wells: Array[Well] = {
		(0 until nWells).map(i => new Well(this, i)).toArray
	}
}

class Carrier extends Part
