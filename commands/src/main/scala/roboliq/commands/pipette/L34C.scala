package roboliq.commands.pipette

import roboliq.common._


case class L3C_Mix(args: L3A_MixArgs) extends Command
case class L4C_Mix(dests: Seq[WellOrPlate], mixSpec: MixSpec) extends Command

case class L3C_Clean(tips: Set[TipConfigL2], degree: CleanDegree.Value) extends Command

sealed class L3A_MixArgs(
		val wells: Set[WellConfigL2],
		val mixSpec: MixSpec
		)
sealed class L4A_MixArgs(
		val dest: WellOrPlateOrLiquid,
		val mixSpec: MixSpec
		)
