package roboliq.commands.pipette

import scala.collection.immutable.SortedSet

import roboliq.common._


case class L4C_Pipette(args: L4A_PipetteArgs) extends CommandL4 {
	type L3Type = L3C_Pipette

	def addKnowledge(kb: KnowledgeBase) {
		for (item <- args.items) {
			item.src.getWells(kb).map(_.foreach(o => kb.addWell(o, true)))
			item.src.getPlatesL4.map(_.foreach(o => kb.addPlate(o)))
			item.src.getReagentsL4.map(_.foreach(o => kb.addReagent(o)))

			item.dest.getWells(kb).map(_.foreach(o => kb.addWell(o, false)))
			item.dest.getPlatesL4.map(_.foreach(o => kb.addPlate(o)))
			item.dest.getReagentsL4.map(_.foreach(o => kb.addReagent(o)))

			/*
			(item.src, item.dest) match {
				case (WPL_Plate(plate1), WP_Plate(plate2)) =>
					val setup1 = kb.getPlateSetup(plate1)
					val setup2 = kb.getPlateSetup(plate2)
					(setup1.dim_?, setup2.dim_?) match {
						case (Some(dim1), None) =>
							new PlateProxy(kb, plate2).setDimension(dim1.nRows, dim1.nCols)
						case (None, Some(dim2)) =>
							new PlateProxy(kb, plate1).setDimension(dim2.nRows, dim2.nCols)
						case _ =>
					}
				case _ =>
			}
			*/
		}
	}
	
	def toL3(states: RobotState): Result[L3Type] = {
		for { args3 <- args.toL3(states) }
		yield L3C_Pipette(args3)
	}
}

case class L3C_Pipette(args: L3A_PipetteArgs) extends CommandL3 {
	override def toDebugString = {
		val srcs = args.items.groupBy(_.srcs).keys
		if (srcs.size == 1) {
			val sSrcs = getWellsDebugString(srcs.head)
			val sDests = getWellsDebugString(args.items.map(_.dest))
			val sVolumes = getSeqDebugString(args.items.map(_.nVolume))
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
		else if (args.items.forall(_.srcs.size == 1)) {
			val sSrcs = getWellsDebugString(args.items.map(_.srcs.head))
			val sDests = getWellsDebugString(args.items.map(_.dest))
			val sVolumes = getSeqDebugString(args.items.map(_.nVolume))
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
		else {
			val lsSrcs = args.items.map(item => getWellsDebugString(item.srcs))
			val sSrcs = getSeqDebugString(lsSrcs)
			val sDests = getWellsDebugString(args.items.map(_.dest))
			val sVolumes = getSeqDebugString(args.items.map(_.nVolume))
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
	}
}

class L4A_PipetteArgs(
	val items: Seq[L4A_PipetteItem],
	val mixSpec_? : Option[MixSpec] = None,
	val tipOverrides_? : Option[TipHandlingOverrides] = None,
	val pipettePolicy_? : Option[PipettePolicy] = None,
	val tipModel_? : Option[TipModel] = None
) {
	def toL3(states: RobotState): Result[L3A_PipetteArgs] = {
		for { llItem3 <- Result.sequence(items.map(_.toL3(states))) }
		yield new L3A_PipetteArgs(
			llItem3.flatten,
			mixSpec_? = mixSpec_?,
			tipOverrides_? = tipOverrides_?,
			pipettePolicy_? = pipettePolicy_?,
			tipModel_? = tipModel_?
		)
	}
}

class L3A_PipetteArgs(
	val items: Seq[L3A_PipetteItem],
	val mixSpec_? : Option[MixSpec] = None,
	val tipOverrides_? : Option[TipHandlingOverrides] = None,
	val pipettePolicy_? : Option[PipettePolicy] = None,
	val tipModel_? : Option[TipModel] = None
)

case class L4A_PipetteItem(
	val src: WellPointer,
	val dest: WellPointer,
	val lnVolume: Seq[Double]
) {
	def toL3(states: RobotState): Result[Seq[L3A_PipetteItem]] = {
		for {
			srcs <- src.getWells(states)
			dests <- dest.getWells(states)
			val lLiquid = srcs.map(_.state(states).liquid).toSet
			_ <- Result.assert(lLiquid.size == 1 || srcs.size == dests.size, "you must specify an equal number of source and destination wells: "+srcs+" vs "+dests)
			_ <- Result.assert(lnVolume.size == 1 || dests.size == lnVolume.size, "you must specify an equal number of destinations and volumes: "+dests+" vs "+lnVolume)
		} yield {
			val mapDestToVolume = {
				if (lnVolume.size == 1) dests.map(_ -> lnVolume.head).toMap
				else (dests zip lnVolume).toMap
			}
			if (lLiquid.size == 1)
				dests.map(dest => new L3A_PipetteItem(SortedSet(srcs : _*), dest, mapDestToVolume(dest)))
			else
				(srcs.toSeq zip dests.toSeq).map(pair => new L3A_PipetteItem(SortedSet(pair._1), pair._2, mapDestToVolume(pair._2)))
		}
	}
}

case class L3A_PipetteItem(
	val srcs: SortedSet[WellConfigL2],
	val dest: WellConfigL2,
	val nVolume: Double
)

object L3A_PipetteItem {
	def toDebugString(items: Seq[L3A_PipetteItem]) = {
		val srcs = items.groupBy(_.srcs).keys
		if (srcs.size == 1) {
			val sSrcs = Command.getWellsDebugString(srcs.head)
			val sDests = Command.getWellsDebugString(items.map(_.dest))
			val sVolumes = Command.getSeqDebugString(items.map(_.nVolume))
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
		else if (items.forall(_.srcs.size == 1)) {
			val sSrcs = Command.getWellsDebugString(items.map(_.srcs.head))
			val sDests = Command.getWellsDebugString(items.map(_.dest))
			val sVolumes = Command.getSeqDebugString(items.map(_.nVolume))
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
		else {
			val lsSrcs = items.map(item => Command.getWellsDebugString(item.srcs))
			val sSrcs = Command.getSeqDebugString(lsSrcs)
			val sDests = Command.getWellsDebugString(items.map(_.dest))
			val sVolumes = Command.getSeqDebugString(items.map(_.nVolume))
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
	}
}