package roboliq.devices.pipette

import roboliq.common._


object CleanDegree extends Enumeration {
	val None, Light, Thorough, Decontaminate = Value
}

case class Site(val parent: Obj, val index: Int)

object PipettePosition extends Enumeration {
	val Free, WetContact, DryContact = Value
}

// TODO: add various speeds, such as entry, pump, exit
//case class PipettePolicy(val pos: PipettePosition.Value) // FIXME: remove this
case class PipettePolicy(sName: String, pos: PipettePosition.Value)

class Tip(val index: Int) extends Ordered[Tip] {
	override def compare(that: Tip): Int = this.index - that.index
}
class WellState(val well: Well, val liquid: Liquid, val nVolume: Double) {
	def add(liquid2: Liquid, nVolume2: Double) = new WellState(well, liquid + liquid2, nVolume + nVolume2)
	def remove(nVolume2: Double) = new WellState(well, liquid, nVolume - nVolume2)
}

case class TipState(
	val tip: Tip, 
	val liquid: Liquid, 
	val nVolume: Double, 
	val contamInside: Contamination, 
	val nContamInsideVolume: Double,
	val destsEntered: List[Liquid],
	val cleanDegree: CleanDegree.Value
) {
	def aspirate(liquid2: Liquid, nVolume2: Double): TipState = {
		val nVolumeNew = nVolume + nVolume2
		new TipState(
			tip,
			liquid + liquid2,
			nVolumeNew,
			contamInside + liquid2,
			math.max(nContamInsideVolume, nVolumeNew),
			destsEntered,
			CleanDegree.None
		)
	}
	def dispense(nVolumeDisp: Double, liquidDest: Liquid, pos: PipettePosition.Value): TipState = {
		pos match {
			case PipettePosition.WetContact => dispenseIn(nVolumeDisp, liquidDest)
			case _ => dispenseFree(nVolumeDisp)
		}
	}
	def dispenseFree(nVolume2: Double): TipState =
		this.copy(nVolume = nVolume - nVolume2, cleanDegree = CleanDegree.None)
	def dispenseIn(nVolume2: Double, liquid2: Liquid): TipState =
		this.copy(nVolume = nVolume - nVolume2, destsEntered = liquid2 :: destsEntered, cleanDegree = CleanDegree.None)
	def clean(cleanDegree: CleanDegree.Value) = TipState(tip).copy(cleanDegree = cleanDegree)
}

object TipState {
	def apply(tip: Tip) = new TipState(tip, Liquid.empty, 0, Contamination.empty, 0, Nil, CleanDegree.None)
}
