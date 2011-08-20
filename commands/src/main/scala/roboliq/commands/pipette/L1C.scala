package roboliq.commands.pipette

import roboliq.common._


trait HasTip {
	val tip: TipConfigL1
}

sealed class TipWell(val tip: TipConfigL1, val well: WellConfigL1) extends HasTip {
	override def toString = "TipWell("+tip.index+","+well.holder.hashCode()+":"+well.index+")" 
}

sealed class TipWellVolume(
		tip: TipConfigL1, well: WellConfigL1,
		val nVolume: Double
	) extends TipWell(tip, well) {
	override def toString = "TipWellVolume("+tip.index+","+well.holder.hashCode()+":"+well.index+","+nVolume+")" 
}

sealed class TipWellVolumePolicy(tip: TipConfigL1, well: WellConfigL1, nVolume: Double,
		val policy: PipettePolicy
	) extends TipWellVolume(tip, well, nVolume) {
	override def toString = "TipWellVolumePolicy("+tip.index+","+well.holder.hashCode()+":"+well.index+","+nVolume+","+policy+")" 
}

sealed class TipWellVolumePolicyCount(tip: TipConfigL1, well: WellConfigL1, nVolume: Double, policy: PipettePolicy,
		val nCount: Int
	) extends TipWellVolumePolicy(tip, well, nVolume, policy) {
	override def toString = "TipWellVolumePolicyCount("+tip.index+","+well.holder.hashCode()+":"+well.index+","+nVolume+","+policy+","+nCount+")" 
}

object ContaminationSeverity extends Enumeration {
	val None, Minor, Medium, Major = Value
}

case class L1C_Aspirate(twvs: Seq[TipWellVolumePolicy]) extends Command
case class L1C_Dispense(twvs: Seq[TipWellVolumePolicy]) extends Command
//case class L1C_Clean(tips: Seq[Tip], degree: CleanDegree.Value) extends Command
case class L1C_Mix(twvpcs: Seq[TipWellVolumePolicyCount]) extends Command
case class L1C_Wash(tips: Set[TipConfigL1], degree: CleanDegree.Value, iWashProgram: Int) extends Command
case class L1C_TipsDrop(tips: Set[Tip])
case class L1C_TipsGet(tips: Set[Tip]) // FIXME: add tip kind
/*case class T0_Wash(
	mTips: Int,
	iWasteGrid: Int, iWasteSite: Int,
	iCleanerGrid: Int, iCleanerSite: Int,
	nWasteVolume: Double,
	nWasteDelay: Int,
	nCleanerVolume: Double,
	nCleanerDelay: Int,
	nAirgapVolume: Int,
	nAirgapSpeed: Int,
	nRetractSpeed: Int,
	bFastWash: Boolean
) extends T0_Token("wash") {
	override def toString = {
		val fmt = new java.text.DecimalFormat("#.##")
		Array(
			mTips,
			iWasteGrid, iWasteSite,
			iCleanerGrid, iCleanerSite,
			'"'+fmt.format(nWasteVolume)+'"',
			nWasteDelay,
			'"'+fmt.format(nCleanerVolume)+'"',
			nCleanerDelay,
			nAirgapVolume,
			nAirgapSpeed,
			nRetractSpeed,
			(if (bFastWash) 1 else 0),
			0,1000,0
		).mkString("Wash(", ",", ")")
	}
}
*/

case class L1C_SetTipStateClean(tips: Seq[Tip], degree: CleanDegree.Value) extends Command
