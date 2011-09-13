package roboliq.commands.pipette

import scala.collection.immutable.SortedSet

import roboliq.common._


case class L4C_Pipette(args: L4A_PipetteArgs) extends CommandL4 {
	type L3Type = L3C_Pipette

	def addKnowledge(kb: KnowledgeBase) {
		for (item <- args.items) {
			item.src match {
				case WPL_Well(o) => kb.addWell(o, true)
				case WPL_Plate(o) => kb.addPlate(o, true)
				case WPL_Liquid(o) => kb.addReagent(o)
			}
			item.dest match {
				case WP_Well(o) => kb.addWell(o, false)
				case WP_Plate(o) => kb.addPlate(o, false)
			}
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
		}
	}
	
	def toL3(states: RobotState): Result[L3Type] = {
		args.toL3(states) match {
			case Error(lsErrors) => Error(lsErrors)
			case Success(args3) => Success(new L3C_Pipette(args3))
		}
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
	def toL3(states: RobotState): Result[L3A_PipetteArgs] = for(
		llItem3 <- Result.sequence(items.map(_.toL3(states)))
		) yield
		new L3A_PipetteArgs(
			llItem3.flatten,
			mixSpec_? = mixSpec_?,
			tipOverrides_? = tipOverrides_?,
			pipettePolicy_? = pipettePolicy_?,
			tipModel_? = tipModel_?
		)
}

class L3A_PipetteArgs(
	val items: Seq[L3A_PipetteItem],
	val mixSpec_? : Option[MixSpec] = None,
	val tipOverrides_? : Option[TipHandlingOverrides] = None,
	val pipettePolicy_? : Option[PipettePolicy] = None,
	val tipModel_? : Option[TipModel] = None
)

case class L4A_PipetteItem(
	val src: WellOrPlateOrLiquid,
	val dest: WellOrPlate,
	val nVolume: Double
) {
	def toL3(states: RobotState): Result[Seq[L3A_PipetteItem]] = {
		val srcs3 = PipetteHelperL4.getWells1(states, src)
		if (srcs3.isEmpty) {
			src match {
				case WPL_Liquid(reagent) =>
					val liquid = reagent.state(states).conf.liquid
					return Error(Seq("Liquid \""+liquid.getName()+"\" must be assigned to one or more wells"))
				case _ => return Error(Seq("INTERNAL: no config found for pipette source "+src))
			}
		}
		val dests3 = PipetteHelperL4.getWells1(states, dest).toSeq
		//println("dests3: "+dests3)
		def createItemsL3() = Success(dests3.map(dest3 => new L3A_PipetteItem(srcs3, dest3, nVolume)))
						
		src match {
			case WPL_Well(_) =>
				createItemsL3()
			case WPL_Plate(plate1) =>
				dest match {
					case WP_Well(_) =>
						Error(Seq("when source is a plate, destination must also be a plate"))
					case WP_Plate(plate2) =>
						if (plate1.state(states).conf.nWells != plate1.state(states).conf.nWells)
							Error(Seq("source and destination plates must have the same number of wells"))
						else {
							val items = (srcs3.toSeq zip dests3.toSeq).map(pair => new L3A_PipetteItem(SortedSet(pair._1), pair._2, nVolume))
							Success(items)
						}
				}
			case WPL_Liquid(liquid1) =>
				createItemsL3()
		}
	}
}

case class L3A_PipetteItem(
		val srcs: SortedSet[WellConfigL2],
		val dest: WellConfigL2,
		val nVolume: Double
		)
