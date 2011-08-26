package roboliq.common

object Contaminant extends Enumeration {
	val Cell, Dna, Other = Value
}

object ContaminationLevel extends Enumeration {
	val None, Minor, Major = Value
}

object TipReplacementAction extends Enumeration {
	val None, Drop, Replace = Value
}

class ContaminationMap(map: Map[Contaminant.Value, ContaminationLevel.Value]) {
	
}

class TipHandlingOverrides(
	val replacement_? : Option[TipReplacementAction.Value],
	val contaminantLevels: Map[Contaminant.Value, ContaminationLevel.Value]
)

class X() {
	def chooseDispenseTipHandlingAction(overrides: TipHandlingOverrides, /*policy: PipettePolicy,*/ liquidInWell: Liquid2, contaminantsOnTip: Map[Contaminant.Value, ContaminationLevel.Value]): TipReplacementAction.Value = {
		overrides.replacement_? match {
			case Some(action) =>
				action
			case None =>
				if (liquidInWell.bWaterFreeDispense) // FIXME: use 'policy' instead?
					TipReplacementAction.None
				else if (liquidInWell.bRequireDecontamBeforeAspirate)
					TipReplacementAction.Replace
				else if (contaminantsOnTip.keys.exists(liquidInWell.prohibitedTipContaminants.contains))
					TipReplacementAction.Replace
				else
					TipReplacementAction.None
		}
	}
	
	def chooseDispenseWashAction(overrides: TipHandlingOverrides, /*policy: PipettePolicy,*/ liquidInWell: Liquid2, contaminantsOnTip: Map[Contaminant.Value, ContaminationLevel.Value]): TipReplacementAction.Value = {
		
	}
}

class Liquid2(
	val sName: String,
	val bWaterFreeDispense: Boolean,
	val bRequireDecontamBeforeAspirate: Boolean,
	/** Contaminants in this liquid */
	val contaminantLevels: Map[Contaminant.Value, ContaminationLevel.Value],
	/** Contaminants which must be cleaned from tips before entering this liquid */
	val prohibitedTipContaminants: Seq[Contaminant.Value]
)

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
