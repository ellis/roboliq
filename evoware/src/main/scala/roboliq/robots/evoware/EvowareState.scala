package roboliq.robots.evoware

import scala.collection.mutable.HashMap
import scala.collection.mutable.Stack

import roboliq.core.LiquidVolume


class EvowareState {
	val mapWellToVolumeChanges = new HashMap[String, Stack[LiquidVolume]]
}