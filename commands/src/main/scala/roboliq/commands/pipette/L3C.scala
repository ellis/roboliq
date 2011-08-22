package roboliq.commands.pipette

import roboliq.common._


case class L3C_Pipette(items: Seq[L3A_PipetteItem], mixSpec_? : Option[MixSpec] = None) extends Command

case class L3C_Mix(dests: Seq[WellOrPlate], mixSpec: MixSpec) extends Command


sealed class L3A_PipetteItem(
		val src: WellOrPlateOrLiquid,
		val dest: WellOrPlate,
		val nVolume: Double)
