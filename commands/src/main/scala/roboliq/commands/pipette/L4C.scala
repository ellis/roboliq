package roboliq.commands.pipette

import roboliq.common._


case class L4C_Pipette(items: Seq[L4A_PipetteItem], mixSpec_? : Option[MixSpec] = None) extends Command

case class L4C_Mix(dests: Seq[WellOrPlate], mixSpec: MixSpec) extends Command


sealed class L4A_PipetteItem(
		val src: WellOrPlateOrLiquid,
		val dest: WellOrPlate,
		val nVolume: Double)
