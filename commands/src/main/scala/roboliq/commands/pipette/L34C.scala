package roboliq.commands.pipette

import roboliq.common._


//-------------------------------
// Mix
//-------------------------------

case class L3C_Mix(args: L3A_MixArgs) extends Command
case class L4C_Mix(dests: Seq[WellOrPlate], mixSpec: MixSpec) extends Command

class L4A_MixArgs(
	val items: Seq[L4A_MixItem],
	val mixSpec: MixSpec,
	val sMixClass_? : Option[String] = None,
	val sTipKind_? : Option[String] = None,
	val fnClean_? : Option[Unit => Unit] = None
) {
	def toL3(states: RobotState): Either[Seq[String], L3A_MixArgs] = {
		val items3_? = items.map(_.toL3(states))
		if (items3_?.exists(_.isLeft)) {
			val lsErrors = items3_?.filter(_.isLeft).flatMap(_.left.get)
			return Left(lsErrors)
		}
		
		val items3 = items3_?.flatMap(_.right.get)
		Right(new L3A_MixArgs(
			items3,
			mixSpec = mixSpec,
			sMixClass_? = sMixClass_?,
			sTipKind_? = sTipKind_?,
			fnClean_? = fnClean_?
		))
	}
}

class L3A_MixArgs(
	val items: Seq[L3A_MixItem],
	val mixSpec: MixSpec,
	val sMixClass_? : Option[String] = None,
	val sTipKind_? : Option[String] = None,
	val fnClean_? : Option[Unit => Unit] = None
)

sealed class L4A_MixItem(
	val target: WellOrPlateOrLiquid,
	val nVolume: Double
) {
	def toL3(states: RobotState): Either[Seq[String], Seq[L3A_MixItem]] = {
		val targets3 = PipetteHelperL4.getWells1(states, target)
		Right(targets3.map(target3 => new L3A_MixItem(target3, nVolume)).toSeq)
	}
}

case class L3A_MixItem(
	val well: WellConfigL2,
	val nVolume: Double
)

//-------------------------------
// Clean
//-------------------------------

case class L3C_Clean(tips: Set[TipConfigL2], degree: CleanDegree.Value) extends Command
