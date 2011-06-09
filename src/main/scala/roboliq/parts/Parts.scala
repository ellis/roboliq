package roboliq.parts

sealed class AspirateStrategy(val sName: String)
sealed class DispenseStrategy(val sName: String, val bEnter: Boolean)

class Tip(val index: Int)

class Liquid(val sName: String, val bWaterFreeDispense: Boolean, val bCells: Boolean, val bDna: Boolean, val bOtherContaminant: Boolean) {
	def contaminates = bCells || bDna || bOtherContaminant
	
	def +(other: Liquid): Liquid = {
		if (this eq other)
			this
		else if (this eq Liquid.empty)
			other
		else if (other eq Liquid.empty)
			this
		else {
			assert(sName != other.sName)
			val sName3 = sName+":"+other.sName
			val bContaminates3 = bContaminates | other.bContaminates
			new Liquid(sName3, bContaminates3)
		}
	}
}
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
}

class WellState(val well: Well, val liquid: Liquid, val nVolume: Double) {
	def add(liquid2: Liquid, nVolume2: Double) = new WellState(well, liquid + liquid2, nVolume + nVolume2)
	def remove(nVolume2: Double) = new WellState(well, liquid, nVolume - nVolume2)
}
/*object WellState {
	def fill(wells: Seq[Well], liquid: Liquid, nVolume: Double) {
		for (well <- wells) {
			well.liquid = liquid
			well.nVolume = nVolume
		}
	}
}*/

class Plate(val nRows: Int, val nCols: Int) extends Part with WellHolder {
	val nWells = nRows * nCols
	val wells: Array[Well] = {
		(0 until nWells).map(i => new Well(this, i)).toArray
	}
}

class Carrier extends Part
