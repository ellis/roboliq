package roboliq.devices.pipette

import roboliq.common._
import roboliq.devices.MixSpec


class L2_PipetteItem(
		val srcs: Set[WellConfigL1],
		val dest: WellConfigL1,
		val nVolume: Double
		)

class L2_PipetteArgs(
		val items: Seq[L2_PipetteItem],
		val mixSpec_? : Option[MixSpec] = None,
		val sAspirateClass_? : Option[String] = None,
		val sDispenseClass_? : Option[String] = None,
		val sMixClass_? : Option[String] = None,
		val sTipKind_? : Option[String] = None,
		val fnClean_? : Option[Unit => Unit] = None
		)

case class L2_PipetteCommand(args: L2_PipetteArgs) extends Command
