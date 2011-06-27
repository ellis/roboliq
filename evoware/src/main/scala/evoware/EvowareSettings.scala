package evoware

import roboliq.parts._

/*sealed class EvowareTipWashSpec(
	val iWasteGrid: Int, val iWasteSite: Int, val nWasteDelay: Int,
	val iCleanerGrid: Int, val iCleanerSite: Int, val nCleanerDelay: Int,
	val nAirgapVolume: Int,
	val nAirgapSpeed: Int,
	val nRetractSpeed: Int,
	val bFastWash: Boolean
)*/

/*
sealed class EvowareSetupFixed(
	val tipKinds: Traversable[EvowareTipKind],
	val nTips: Int
	///** Lists of indexes of tips which can be operated in parallel */
	//val aaiTipBatches: List[List[Int]]
)
*/

/*
sealed class EvowareSetupState(
	//val tipTipKinds: IndexedSeq[EvowareTipKind],
	val mapPartToGrid: Map[Part, Int]
) {
	val mapGridToPart = mapPartToGrid.map(pair => pair._2 -> pair._1)
	def getPartAt(iGrid: Int): Option[Part] = mapGridToPart.get(iGrid) 
}
*/

/*
sealed class EvwareSetup(val fixed: EvowareSetupFixed, state0: EvowareSetupState) {
	var state = state0
}
object EvowareSetup {
	def load(): EvowareSetup = {
		val fixed = new EvowareSetupFixed(
			tipKinds = Vector()
		)
		val config = {
			val tips = (0 until fixed.nTips).map(new Tip(_))
			val tipKindsPowerset = powerSet(settings.tipKinds.toSet).filter(!_.isEmpty)
			val tipTipKindsIndexed = state.tipTipKinds.zipWithIndex
			val tipGroups = tipKindsPowerset.map(kinds => tipTipKindsIndexed.filter((kind, iTip) => kinds.contains(kind)).map(_.2).toArray).toArray
			new RobotConfig(tips, tipGroups)
		}
	}

}
*/
