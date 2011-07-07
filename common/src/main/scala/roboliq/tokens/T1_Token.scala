package roboliq.tokens

import roboliq.parts._

trait HasTip {
	val tip: Tip
}

object DispenseKind extends Enumeration {
	val Free, WetContact, DryContact = Value
}

sealed class TipWell(val tip: Tip, val well: Well) extends HasTip {
	override def toString = "TipWell("+tip.index+","+well.holder.index+":"+well.index+")" 
}

sealed class TipWellVolume(
		tip: Tip, well: Well,
		val nVolume: Double
	) extends TipWell(tip, well) {
	override def toString = "TipWell("+tip.index+","+well.holder.index+":"+well.index+","+nVolume+")" 
}

sealed class TipWellVolumeDispense(
		tip: Tip, well: Well, nVolume: Double,
		val dispenseKind: DispenseKind.Value
	) extends TipWellVolume(tip, well, nVolume) {
	override def toString = "TipWellVolumeDispense("+tip.index+","+well.holder.index+":"+well.index+","+nVolume+","+dispenseKind+")" 
}

object ContaminationSeverity extends Enumeration {
	val None, Minor, Medium, Major = Value
}
/*
sealed class TipCleanInfo(val tip: Tip,
		val nInsideVolume: Double, val insideSeverity: ContaminationSeverity.Value,
		val nOutsideVolume: Double, val outsideSeverity: ContaminationSeverity.Value) extends HasTip
*/
 
case class T1_Aspirate(twvs: Seq[TipWellVolume]) extends T1_Token("aspirate")
case class T1_Dispense(twvs: Seq[TipWellVolumeDispense]) extends T1_Token("dispense")
case class T1_Clean(tips: Seq[Tip], degree: CleanDegree.Value) extends T1_Token("clean")
