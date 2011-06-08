package evoware

import roboliq.parts.Part

sealed class EvowareTipWashSettings(
	val iWasteGrid: Int, val iWasteSite: Int, val nWasteDelay: Int,
	val iCleanerGrid: Int, val iCleanerSite: Int, val nCleanerDelay: Int,
	val nAirgapVolume: Int,
	val nAirgapSpeed: Int,
	val nRetractSpeed: Int,
	val bFastWash: Boolean
)

//sealed case class Loc(iGrid: Int, iSite: Int)
sealed class EvowareTipKind(
	val id: Int,
	val mapLiquidClasses: Map[String, String]
)

sealed class EvowareSettings(
	val grids: Map[Part, Int],
	val tipKinds: Traversable[EvowareTipKind],
	/** For each tip, select its kind */
	val kindOfTips: Seq[EvowareTipKind]
)
