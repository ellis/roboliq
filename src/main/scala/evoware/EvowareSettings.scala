package evoware

import roboliq.parts.Part

//sealed case class Loc(iGrid: Int, iSite: Int)
sealed class EvowareTipKind(val id: Int, val mapLiquidClasses: Map[String, String])

sealed class EvowareSettings(
	val grids: Map[Part, Int],
	val mapTipIndexToKind: Map[Int, EvowareTipKind]
)
