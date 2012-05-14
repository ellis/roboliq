package roboliq.robots.evoware

import scala.collection.mutable.HashMap
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.Stack

import roboliq.core.LiquidVolume
//import roboliq.core.WellState


class EvowareState {
	val mapLiquidToWellToAspirated = new LinkedHashMap[String, HashMap[String, LiquidVolume]]
	//val mapVesselToContents0 = new LinkedHashMap[String, WellState]
	//val mapVesselToContents1 = new LinkedHashMap[String, WellState]
}