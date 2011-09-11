package roboliq.commands.pipette

import scala.collection.immutable.SortedSet

import roboliq.common._


case class L4C_Mix(args: L4A_MixArgs) extends CommandL4 {
	type L3Type = L3C_Mix

	def addKnowledge(kb: KnowledgeBase) {
		for (target <- args.targets) {
			target match {
				case WPL_Well(o) => kb.addWell(o, true)
				case WPL_Plate(o) => kb.addPlate(o, true)
				case WPL_Liquid(o) => kb.addReagent(o)
			}
		}
	}
	
	def toL3(states: RobotState): Either[Seq[String], L3Type] = {
		args.toL3(states) match {
			case Left(lsErrors) => Left(lsErrors)
			case Right(args3) => Right(new L3C_Mix(args3))
		}
	}

}

case class L3C_Mix(args: L3A_MixArgs) extends CommandL3

class L4A_MixArgs(
	val targets: Iterable[WellOrPlateOrLiquid],
	val mixSpec: MixSpec,
	val tipOverrides_? : Option[TipHandlingOverrides] = None,
	val tipModel_? : Option[TipModel] = None
) {
	def toL3(states: RobotState): Either[Seq[String], L3A_MixArgs] = {
		val wells3 = targets.foldLeft(SortedSet(): SortedSet[WellConfigL2]) {(acc, target) => acc ++ PipetteHelperL4.getWells1(states, target) }
		Right(new L3A_MixArgs(
			wells3,
			mixSpec = mixSpec,
			tipOverrides_? = tipOverrides_?,
			tipModel_? = tipModel_?
		))
	}
}

class L3A_MixArgs(
	val wells: SortedSet[WellConfigL2],
	val mixSpec: MixSpec,
	val tipOverrides_? : Option[TipHandlingOverrides] = None,
	val tipModel_? : Option[TipModel] = None
)

/*
sealed class L4A_MixItem(
	val target: WellOrPlateOrLiquid
) {
	def toL3(states: RobotState): Either[Seq[String], Set[WellConfigL2]] = {
		Right(PipetteHelperL4.getWells1(states, target))
	}
}

case class L3A_MixItem(
	val well: WellConfigL2,
	val nVolume: Double
)
*/