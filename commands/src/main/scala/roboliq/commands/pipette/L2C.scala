package roboliq.commands.pipette

import roboliq.common._

case class L2C_Pipette(args: L2A_PipetteArgs) extends Command

case class L2C_Clean(tips: Set[TipStateL1], degree: CleanDegree.Value) extends Command

case class L2C_Mix(args: L2A_MixArgs) extends Command

class L2A_PipetteItem(
		val srcs: Set[WellL2],
		val dest: WellL2,
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

class L2A_MixArgs(
		val wells: Set[WellL2],
		val mixSpec: MixSpec
		)
