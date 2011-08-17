package roboliq.common

class Contamination(val bCells: Boolean, val bDna: Boolean, val bOtherContaminant: Boolean) {
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
