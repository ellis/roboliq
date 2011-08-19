package roboliq.devices.pipette

import roboliq.common._
import roboliq.level3._

case class L3C_Pipette(items: Seq[L3A_PipetteItem], mixSpec_? : Option[MixSpec] = None) extends Command

sealed class L3A_PipetteItem(
		val src: WellOrPlateOrLiquid,
		val dest: WellOrPlate,
		val nVolume: Double)

case class MixSpec(val nVolume: Double, val nCount: Int)
