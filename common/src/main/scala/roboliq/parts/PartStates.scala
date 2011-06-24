// REFACTOR: Consider moving this to robot package, since that's where it's needed and not here

package roboliq.parts

object CleanDegree extends Enumeration {
	val None, Light, Thorough, Decontaminate = Value
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
	def dispenseFree(nVolume2: Double): TipState =
		this.copy(nVolume = nVolume - nVolume2, cleanDegree = CleanDegree.None)
	def dispenseIn(liquid2: Liquid, nVolume2: Double): TipState =
		this.copy(nVolume = nVolume - nVolume2, destsEntered = liquid2 :: destsEntered, cleanDegree = CleanDegree.None)
	def clean(cleanDegree: CleanDegree.Value) = TipState(tip).copy(cleanDegree = cleanDegree)
}

object TipState {
	def apply(tip: Tip) = new TipState(tip, Liquid.empty, 0, Contamination.empty, 0, Nil, CleanDegree.None)
}
