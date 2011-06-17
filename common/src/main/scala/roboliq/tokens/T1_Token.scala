package roboliq.tokens

import roboliq.parts._

trait HasTip {
	val tip: Tip
}

sealed class TipWellVolume(val tip: Tip, val well: Well, val nVolume: Double) extends HasTip

object ContaminationSeverity extends Enumeration {
	val None, Minor, Medium, Major = Value
}
/*
sealed class TipCleanInfo(val tip: Tip,
		val nInsideVolume: Double, val insideSeverity: ContaminationSeverity.Value,
		val nOutsideVolume: Double, val outsideSeverity: ContaminationSeverity.Value) extends HasTip
*/
 
sealed abstract class T1_Token
case class T1_Aspirate(twvs: Seq[TipWellVolume]) extends T1_Token
case class T1_Dispense(twvs: Seq[TipWellVolume]) extends T1_Token
case class T1_Clean(tips: Seq[Tip], degree: CleanDegree.Value) extends T1_Token
