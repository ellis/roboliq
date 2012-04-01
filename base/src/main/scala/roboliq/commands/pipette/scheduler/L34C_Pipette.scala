package roboliq.commands.pipette.scheduler

import scala.collection.immutable.SortedSet

import roboliq.core._


case class L3C_Pipette(args: L3A_PipetteArgs) {
	override def toDebugString = {
		val srcs = args.items.groupBy(_.srcs).keys
		if (srcs.size == 1) {
			val sSrcs = Printer.getWellsDebugString(srcs.head)
			val sDests = Printer.getWellsDebugString(args.items.map(_.dest))
			val sVolumes = Printer.getSeqDebugString(args.items.map(_.nVolume))
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
		else if (args.items.forall(_.srcs.size == 1)) {
			val sSrcs = Printer.getWellsDebugString(args.items.map(_.srcs.head))
			val sDests = Printer.getWellsDebugString(args.items.map(_.dest))
			val sVolumes = Printer.getSeqDebugString(args.items.map(_.nVolume))
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
		else {
			val lsSrcs = args.items.map(item => Printer.getWellsDebugString(item.srcs))
			val sSrcs = Printer.getSeqDebugString(lsSrcs)
			val sDests = Printer.getWellsDebugString(args.items.map(_.dest))
			val sVolumes = Printer.getSeqDebugString(args.items.map(_.nVolume))
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
	}
}

class L3A_PipetteArgs(
	val items: Seq[L3A_PipetteItem],
	val mixSpec_? : Option[MixSpec] = None,
	val tipOverrides_? : Option[TipHandlingOverrides] = None,
	val pipettePolicy_? : Option[PipettePolicy] = None,
	val tipModel_? : Option[TipModel] = None
)

case class L3A_PipetteItem(
	val srcs: SortedSet[Well],
	val dest: Well,
	val nVolume: Double,
	val premix_? : Option[MixSpec],
	val postmix_? : Option[MixSpec]
)

object L3A_PipetteItem {
	def toDebugString(items: Seq[L3A_PipetteItem]): String = {
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

	def toDebugString(item: L3A_PipetteItem): String = toDebugString(Seq(item))
}
