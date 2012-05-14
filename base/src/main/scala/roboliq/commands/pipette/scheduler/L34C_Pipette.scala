package roboliq.commands.pipette.scheduler

import scala.collection.immutable.SortedSet

import roboliq.core._


case class L3C_Pipette(args: L3A_PipetteArgs) {
	def toDebugString = {
		val srcs = args.items.groupBy(_.srcs).keys
		val sDests = Printer.getWellsDebugString(args.items.map(_.dest))
		val sVolumes = Printer.getSeqDebugString(args.items.map(_.nVolume))
		if (srcs.size == 1) {
			val sSrcs = Printer.getWellsDebugString(srcs.head)
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
		else if (args.items.forall(_.srcs.size == 1)) {
			val sSrcs = Printer.getWellsDebugString(args.items.map(_.srcs.head))
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
		else {
			val lsSrcs = args.items.map(item => Printer.getWellsDebugString(item.srcs))
			val sSrcs = Printer.getSeqDebugString(lsSrcs)
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
	}
	
	def toDocString(ob: ObjBase, states: RobotState): String = {
		def getWellsString(l: Iterable[Well2]): String =
			WellSpecParser.toString(l.toList, ob, ", ")
			
		val srcs = args.items.groupBy(_.srcs).keys
		val sSrcs = {
			if (srcs.size == 1)
				getWellsString(srcs.head)
			else if (args.items.forall(_.srcs.size == 1))
				getWellsString(args.items.map(_.srcs.head))
			else {
				val lsSrcs = args.items.map(item => getWellsString(item.srcs))
				Printer.getSeqDebugString(lsSrcs)
			}
			
		}
		
		val sLiquids = {
			val lsLiquid = srcs.toList.flatMap(_.map(_.wellState(states).map(_.liquid.sName).getOrElse("ERROR")))
			val lsLiquid2 = lsLiquid.distinct
			if (lsLiquid2.isEmpty)
				""
			else if (lsLiquid2.size == 1)
				" of "+lsLiquid2.head
			else
				" of "+Printer.getSeqDebugString(lsLiquid2)
		}
		
		val sDests = getWellsString(args.items.map(_.dest))
				//Printer.getWellsDebugString(args.items.map(_.dest))
		val sVolumes = Printer.getSeqDebugString(args.items.map(_.nVolume))
		
		"Pipette "+sVolumes+sLiquids+" from "+sSrcs+" to "+sDests
	}
}

class L3A_PipetteArgs(
	val items: Seq[Item],
	val mixSpec_? : Option[MixSpec] = None,
	val tipOverrides_? : Option[TipHandlingOverrides] = None,
	val pipettePolicy_? : Option[PipettePolicy] = None,
	val tipModel_? : Option[TipModel] = None
)

case class Item(
	val srcs: SortedSet[Well2],
	val dest: Well2,
	val nVolume: LiquidVolume,
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
