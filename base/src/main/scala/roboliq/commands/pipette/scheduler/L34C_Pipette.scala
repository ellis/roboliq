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
	
	def toDocString(ob: ObjBase, states: RobotState): Tuple2[String, String] = {
		def getWellsString(l: Iterable[Well2]): String =
			WellSpecParser.toString(l.toList, ob, ", ")
		
		// All lists of sources
		val llSrc = args.items.map(_.srcs)
		// First source of each item
		val lSrc0 = llSrc.map(_.head)
		
		val src_? : Option[Well2] = {
			val lSrcDistinct = lSrc0.distinct
			if (lSrcDistinct.size == 1)
				Some(lSrcDistinct.head)
			else
				None
		}
		
		//val bShortSrcs
		val sSrcs = src_? match {
			case Some(src) => src.id
			case None =>
				def step(llSrc: Seq[SortedSet[Well2]], accR: List[String]): String = {
					if (llSrc.isEmpty)
						accR.reverse.mkString(", ")
					else if (llSrc.head.size > 1)
						step(llSrc.tail, getWellsString(llSrc.head) :: accR)
					else {
						val (x, rest) = llSrc.span(_.size == 1)
						val lSrc = x.map(_.head).toList
						step(rest, getWellsString(lSrc) :: accR)
					}
				}
				step(llSrc, Nil)
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
		
		("Pipette "+sVolumes+sLiquids+" from "+sSrcs+" to "+sDests, null)
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
