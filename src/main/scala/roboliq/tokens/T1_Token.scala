package roboliq.tokens

import roboliq.parts._


sealed class TipWellVolume(val tip: Tip, val well: Well, val nVolume: Double)

object ContaminationSeverity extends Enumeration {
	val None, Minor, Medium, Major = Value
}
sealed class TipCleanSpec(val tip: Tip,
		val nInsideVolume: Double, val insideSeverity: ContaminationSeverity.Value,
		val nOutsideVolume: Double, val outsideSeverity: ContaminationSeverity.Value)

sealed abstract class T1_Token
case class T1_Aspirate(twvs: Seq[TipWellVolume]) extends T1_Token
case class T1_Dispense(twvs: Seq[TipWellVolume]) extends T1_Token
case class T1_Clean(specs: Traversable[TipCleanSpec]) extends T1_Token
