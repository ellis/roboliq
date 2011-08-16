package roboliq.devices.pipette

import roboliq.compiler.Command
import roboliq.parts._


trait HasTip {
	val tip: Tip
}

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

case class L1_Aspirate(twvs: Seq[TipWellVolumePolicy]) extends Command
case class L1_Dispense(twvs: Seq[TipWellVolumePolicy]) extends Command
case class L1_Clean(tips: Seq[Tip], degree: CleanDegree.Value) extends Command
