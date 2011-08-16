package roboliq.devices

import scala.collection.mutable.HashSet
import roboliq.compiler._
import roboliq.level3._
import roboliq.devices.pipette.L2_PipetteItem
import roboliq.devices.pipette.L2_PipetteArgs
import roboliq.devices.pipette.L2_PipetteCommand

case class PipetteCommand(items: Seq[PipetteItem], mixSpec_? : Option[MixSpec] = None) extends Command

/*
 * TipSpec:
 * - type
 * - what to do with tips before aspirate (e.g., discard, clean, nothing...)
 */
//class TipSpec(kind: String = null)
case class MixSpec(val nVolume: Double, val nCount: Int)
sealed class PipetteItem(
		val src: WellOrPlateOrLiquid,
		val dest: WellOrPlate,
		val nVolume: Double)
