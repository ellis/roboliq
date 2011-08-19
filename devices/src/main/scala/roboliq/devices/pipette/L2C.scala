package roboliq.devices.pipette

import roboliq.common._


class L2A_PipetteItem(
		val srcs: Set[WellConfigL1],
		val dest: WellConfigL1,
		val nVolume: Double
		)

class L2A_PipetteArgs(
		val items: Seq[L2A_PipetteItem],
		val mixSpec_? : Option[MixSpec] = None,
		val sAspirateClass_? : Option[String] = None,
		val sDispenseClass_? : Option[String] = None,
		val sMixClass_? : Option[String] = None,
		val sTipKind_? : Option[String] = None,
		val fnClean_? : Option[Unit => Unit] = None
		)

case class L2C_Pipette(args: L2A_PipetteArgs) extends Command
