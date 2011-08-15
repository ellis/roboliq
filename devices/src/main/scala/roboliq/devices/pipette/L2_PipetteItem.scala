package roboliq.devices.pipette

import roboliq.compiler.Command
import roboliq.parts._
import roboliq.devices.MixSpec

class L2_PipetteItem(
		val srcs: Set[Well],
		val dest: Well,
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
