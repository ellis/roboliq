package roboliq.devices.pipette

import roboliq.builder.parts._
import roboliq.devices.MixSpec


case class L2_PipetteItem(
		val srcs: Set[Well],
		val dest: Well,
		val nVolume: Double,
		val mixSpec_? : Option[MixSpec] = None,
		val sAspirateClass_? : Option[String] = None,
		val sDispenseClass_? : Option[String] = None,
		val sMixClass_? : Option[String] = None
		)

case class L2_PipetteCommand(items: Seq[L2_PipetteItem])
