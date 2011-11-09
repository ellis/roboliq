package roboliq.devices.pipette.scheduler

import scala.collection.immutable.SortedSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.Queue

import roboliq.common._
import roboliq.commands._
import roboliq.commands.pipette._
import roboliq.commands.pipette.{L3A_PipetteItem => Item}
import roboliq.compiler._
import roboliq.devices.pipette._


private class TipState(val tip: TipConfigL2) {
	var liquid: Liquid = null
	var nVolume: Double = 0
}

case class LM(liquid: Liquid, tipModel: TipModel) {
	override def toString: String = {
		"LM("+liquid.getName()+", "+tipModel.id+")"
	}
}
case class LMData(nTips: Int, nVolumeTotal: Double, nVolumeCurrent: Double)

sealed abstract class CleanSpec2 { val tip: TipConfigL2 }
case class ReplaceSpec2(tip: TipConfigL2, model: TipModel) extends CleanSpec2
case class WashSpec2(tip: TipConfigL2, spec: WashSpec) extends CleanSpec2
case class DropSpec2(tip: TipConfigL2) extends CleanSpec2
