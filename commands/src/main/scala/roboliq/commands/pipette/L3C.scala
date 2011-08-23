package roboliq.commands.pipette

import roboliq.common._

case class L3C_Pipette(args: L3A_PipetteArgs) extends Command

case class L3C_Clean(tips: Set[TipConfigL2], degree: CleanDegree.Value) extends Command

case class L3C_Mix(args: L3A_MixArgs) extends Command

class L3A_PipetteItem(
		val srcs: Set[WellConfigL2],
		val dest: WellConfigL2,
		val nVolume: Double
		)

class L3A_PipetteArgs(
		val items: Seq[L3A_PipetteItem],
		val mixSpec_? : Option[MixSpec] = None,
		val sAspirateClass_? : Option[String] = None,
		val sDispenseClass_? : Option[String] = None,
		val sMixClass_? : Option[String] = None,
		val sTipKind_? : Option[String] = None,
		val fnClean_? : Option[Unit => Unit] = None
		)

class L3A_MixArgs(
		val wells: Set[WellConfigL2],
		val mixSpec: MixSpec
		)
