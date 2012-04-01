package roboliq.commands.pipette.scheduler

import scala.collection.immutable.SortedSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.Queue

import roboliq.core._
import roboliq.commands2._
import roboliq.commands2.pipette._
//import roboliq.compiler._
//import roboliq.devices.pipette._


case class Item(
	val srcs: SortedSet[Well],
	val dest: Well,
	val nVolume: Double,
	val premix_? : Option[MixSpec],
	val postmix_? : Option[MixSpec]
)

object Item {
	def toDebugString(items: Seq[Item]): String = {
		val srcs = items.groupBy(_.srcs).keys
		if (srcs.size == 1) {
			val sSrcs = Printer.getWellsDebugString(srcs.head)
			val sDests = Printer.getWellsDebugString(items.map(_.dest))
			val sVolumes = Printer.getSeqDebugString(items.map(_.nVolume))
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
		else if (items.forall(_.srcs.size == 1)) {
			val sSrcs = Printer.getWellsDebugString(items.map(_.srcs.head))
			val sDests = Printer.getWellsDebugString(items.map(_.dest))
			val sVolumes = Printer.getSeqDebugString(items.map(_.nVolume))
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
		else {
			val lsSrcs = items.map(item => Printer.getWellsDebugString(item.srcs))
			val sSrcs = Printer.getSeqDebugString(lsSrcs)
			val sDests = Printer.getWellsDebugString(items.map(_.dest))
			val sVolumes = Printer.getSeqDebugString(items.map(_.nVolume))
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
	}

	def toDebugString(item: Item): String = toDebugString(Seq(item))
}

case class ItemState(item: Item, srcLiquid: Liquid, destState0: WellState, destState1: WellState)

private class TipState(val tip: Tip) {
	var liquid: Liquid = null
	var nVolume: Double = 0
}

case class LM(liquid: Liquid, tipModel: TipModel) {
	override def toString: String = {
		"LM("+liquid.getName()+", "+tipModel.id+")"
	}
}
case class LMData(nTips: Int, nVolumeTotal: Double, nVolumeCurrent: Double)

sealed abstract class CleanSpec2 { val tip: Tip }
case class ReplaceSpec2(tip: Tip, model: TipModel) extends CleanSpec2
case class WashSpec2(tip: Tip, spec: WashSpec) extends CleanSpec2 {
	override def toString: String = {
		"WashSpec2("+tip+","+spec.washIntensity+")"
	}
}
case class DropSpec2(tip: Tip) extends CleanSpec2
