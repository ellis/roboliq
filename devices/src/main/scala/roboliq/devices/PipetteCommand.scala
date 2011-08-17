package roboliq.devices

import roboliq.common._
import roboliq.level3._

case class PipetteCommand(items: Seq[PipetteItem], mixSpec_? : Option[MixSpec] = None) extends Command

sealed class PipetteItem(
		val src: WellOrPlateOrLiquid,
		val dest: WellOrPlate,
		val nVolume: Double)

case class MixSpec(val nVolume: Double, val nCount: Int)
