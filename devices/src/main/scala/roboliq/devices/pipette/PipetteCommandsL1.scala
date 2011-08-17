package roboliq.devices.pipette

import roboliq.common._


trait HasTip {
	val tip: Tip
}

sealed class TipWell(val tip: Tip, val well: WellConfigL1) extends HasTip {
	override def toString = "TipWell("+tip.index+","+well.holder.hashCode()+":"+well.index+")" 
}

sealed class TipWellVolume(
		tip: Tip, well: WellConfigL1,
		val nVolume: Double
	) extends TipWell(tip, well) {
	override def toString = "TipWellVolume("+tip.index+","+well.holder.hashCode()+":"+well.index+","+nVolume+")" 
}

sealed class TipWellVolumePolicy(tip: Tip, well: WellConfigL1, nVolume: Double,
		val policy: PipettePolicy
	) extends TipWellVolume(tip, well, nVolume) {
	override def toString = "TipWellVolumePolicy("+tip.index+","+well.holder.hashCode()+":"+well.index+","+nVolume+","+policy+")" 
}

object ContaminationSeverity extends Enumeration {
	val None, Minor, Medium, Major = Value
}

case class L1_Aspirate(twvs: Seq[TipWellVolumePolicy]) extends Command
case class L1_Dispense(twvs: Seq[TipWellVolumePolicy]) extends Command
case class L1_Clean(tips: Seq[Tip], degree: CleanDegree.Value) extends Command
