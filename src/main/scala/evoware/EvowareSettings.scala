package evoware

import roboliq.parts.Part

/*sealed class EvowareTipWashSpec(
	val iWasteGrid: Int, val iWasteSite: Int, val nWasteDelay: Int,
	val iCleanerGrid: Int, val iCleanerSite: Int, val nCleanerDelay: Int,
	val nAirgapVolume: Int,
	val nAirgapSpeed: Int,
	val nRetractSpeed: Int,
	val bFastWash: Boolean
)*/

sealed class EvowareSetupFixed(
	val tipKinds: Traversable[EvowareTipKind]
)

sealed class EvowareSetupVariable(
	val 
)

sealed class EvowareSettings(
	val grids: Map[Part, Int],
	val tipKinds: Traversable[EvowareTipKind],
	/** For each tip, select its kind */
	val kindOfTips: Seq[EvowareTipKind],
	val insideWashSpecs: Tuple3[EvowareTipWashSpec, EvowareTipWashSpec, EvowareTipWashSpec],
	val outsideWashSpecs: Tuple3[EvowareTipWashSpec, EvowareTipWashSpec, EvowareTipWashSpec]
) {
	//val nTips = 
}
object EvowareSettings {
	def load(): EvowareSettings = {
		val config = {
		val nTips = settings.kindOfTips.size
		val tips = (0 until nTips).map(new Tip(_))
		val tipKindsPowerset = powerSet(settings.tipKinds.toSet).filter(!_.isEmpty)
		val kindOfTipsIndexed = settings.kindOfTips.zipWithIndex
		val tipGroups = tipKindsPowerset.map(kinds => kindOfTipsIndexed.filter((kind, iTip) => kinds.contains(kind)).map(_.2).toArray).toArray
		new RobotConfig(tips, tipGroups)
	}

}
