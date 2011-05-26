package evoware

import roboliq.parts.Part

sealed case class Loc(iGrid: Int, iSite: Int)
sealed class EvowareSettings(val grids: Map[Part, Int])
