package roboliq.commands.pipette

import roboliq.common._



trait HasTip {
	val tip: TipConfigL2
}

sealed class TipWell(val tip: TipConfigL2, val well: WellConfigL2) extends HasTip {
	override def toString = "TipWell("+tip.index+","+well.holder.sLabel+":"+well.index+")" 
}

sealed class TipWellVolume(
		tip: TipConfigL2, well: WellConfigL2,
		val nVolume: Double
	) extends TipWell(tip, well) {
	override def toString = "TipWellVolume("+tip.index+","+well.holder.hashCode()+":"+well.index+","+nVolume+")" 
}

sealed class TipWellVolumePolicy(tip: TipConfigL2, well: WellConfigL2, nVolume: Double,
		val policy: PipettePolicy
	) extends TipWellVolume(tip, well, nVolume) {
	override def toString = "TipWellVolumePolicy("+tip.index+","+well.holder.hashCode()+":"+well.index+","+nVolume+","+policy+")" 
}

sealed class TipWellVolumePolicyCount(tip: TipConfigL2, well: WellConfigL2, nVolume: Double, liquid: Liquid, policy: PipettePolicy,
		val nCount: Int
	) extends L2A_AspirateItem(tip, well, liquid, nVolume, policy) {
	override def toString = "TipWellVolumePolicyCount("+tip.index+","+well.holder.hashCode()+":"+well.index+","+nVolume+","+policy+","+nCount+")" 
}

sealed class L2A_AspirateItem(
		tip: TipConfigL2, well: WellConfigL2, val liquidWell: Liquid, nVolume: Double, policy: PipettePolicy
	) extends TipWellVolumePolicy(tip, well, nVolume, policy)
sealed class L2A_DispenseItem(
		tip: TipConfigL2, val liquidTip: Liquid, well: WellConfigL2, val liquidWell: Liquid, nVolume: Double, policy: PipettePolicy
	) extends TipWellVolumePolicy(tip, well, nVolume, policy)
sealed class L2A_MixItem(
		tip: TipConfigL2, well: WellConfigL2, liquidWell: Liquid, nVolume: Double, val nCount: Int, policy: PipettePolicy
	) extends L2A_AspirateItem(tip, well, liquidWell, nVolume, policy)


case class L2C_Aspirate(items: Seq[L2A_AspirateItem]) extends Command
case class L2C_Dispense(items: Seq[L2A_DispenseItem]) extends Command
//case class L2C_Clean(tips: Seq[Tip], degree: CleanDegree.Value) extends Command
case class L2C_Mix(items: Seq[L2A_MixItem]) extends Command
case class L2C_Wash(tips: Set[TipConfigL2], degree: CleanDegree.Value, iWashProgram: Int) extends Command
case class L2C_TipsDrop(tips: Set[Tip])
case class L2C_TipsGet(tips: Set[Tip]) // FIXME: add tip kind
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

case class L2C_SetTipStateClean(tips: Seq[TipConfigL2], degree: CleanDegree.Value) extends Command
