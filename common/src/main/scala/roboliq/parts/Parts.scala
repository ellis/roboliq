package roboliq.parts

class ObjectL1

sealed class AspirateStrategy(val sName: String)
sealed class DispenseStrategy(val sName: String, val bEnter: Boolean)

class Tip(val index: Int) extends Ordered[Tip] {
	override def compare(that: Tip): Int = this.index - that.index
}

class Contamination(val bCells: Boolean, val bDna: Boolean, val bOtherContaminant: Boolean) extends ObjectL1 {
	def +(other: Contamination): Contamination = {
		new Contamination(
			bCells | other.bCells,
			bDna | other.bDna,
			bOtherContaminant | other.bOtherContaminant)
	}
}
object Contamination {
	val empty = new Contamination(false, false, false)
}

object PipettePosition extends Enumeration {
	val Free, WetContact, DryContact = Value
}

// TODO: add various speeds, such as entry, pump, exit
//case class PipettePolicy(val pos: PipettePosition.Value) // FIXME: remove this
case class PipettePolicy(sName: String, pos: PipettePosition.Value)

class Liquid(
	val sName: String,
	val bWaterFreeDispense: Boolean,
	val bRequireDecontamBeforeAspirate: Boolean,
	bCells: Boolean,
	bDna: Boolean,
	bOtherContaminant: Boolean
)
extends Contamination(bCells, bDna, bOtherContaminant)
{
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
			new Liquid(
				sName3,
				bWaterFreeDispense | other.bWaterFreeDispense,
				bRequireDecontamBeforeAspirate | other.bRequireDecontamBeforeAspirate,
				bCells | other.bCells,
				bDna | other.bDna,
				bOtherContaminant | other.bOtherContaminant
			)
		}
	}
}
object Liquid {
	val empty = new Liquid("", false, false, false, false, false)
}

class Part extends ObjectL1

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

class Well(val holder: WellHolder, val index: Int) extends Part with Ordered[Well] {
	override def compare(that: Well): Int = {
		val d1 = holder.index - that.holder.index
		if (d1 == 0) index - that.index
		else d1
	}
}

class Plate(val nRows: Int, val nCols: Int) extends Part with WellHolder {
	val nWells = nRows * nCols
	val wells: Array[Well] = {
		(0 until nWells).map(i => new Well(this, i)).toArray
	}
}

class Carrier extends Part
