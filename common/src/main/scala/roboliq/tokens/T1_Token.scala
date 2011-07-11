package roboliq.tokens

import roboliq.parts._

trait HasTip {
	val tip: Tip
}

object PipettePosition extends Enumeration {
	val Free, WetContact, DryContact = Value
}

object PipetteSpeed extends Enumeration {
	val Normal, Slow, Fast = Value
}

// TODO: add various speeds, such as entry, pump, exid
case class PipettePolicy(val pos: PipettePosition.Value)

sealed class TipWell(val tip: Tip, val well: Well) extends HasTip {
	override def toString = "TipWell("+tip.index+","+well.holder.index+":"+well.index+")" 
}

sealed class TipWellVolume(
		tip: Tip, well: Well,
		val nVolume: Double
	) extends TipWell(tip, well) {
	override def toString = "TipWellVolume("+tip.index+","+well.holder.index+":"+well.index+","+nVolume+")" 
}

sealed class TipWellVolumePolicy(tip: Tip, well: Well, nVolume: Double,
		val policy: PipettePolicy
	) extends TipWellVolume(tip, well, nVolume) {
	override def toString = "TipWellVolumePolicy("+tip.index+","+well.holder.index+":"+well.index+","+nVolume+","+policy+")" 
}

object ContaminationSeverity extends Enumeration {
	val None, Minor, Medium, Major = Value
}
 
case class T1_Aspirate(twvs: Seq[TipWellVolumePolicy]) extends T1_Token("aspirate")
case class T1_Dispense(twvs: Seq[TipWellVolumePolicy]) extends T1_Token("dispense")
case class T1_Clean(tips: Seq[Tip], degree: CleanDegree.Value) extends T1_Token("clean")
case class T1_Mix(twvs: Seq[TipWell], policy: PipettePolicy, nVolume: Double, nCount: Int) extends T1_Token("mix")
