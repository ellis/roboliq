package roboliq.commands.pipette

import roboliq.core._


trait HasTip { val tip: Tip }
trait HasWell { val well: Well }
trait HasVolume { val volume: LiquidVolume }
trait HasPolicy { val policy: PipettePolicy }
trait HasMixSpec { val mixSpec: MixSpec }

//class TW(val tip: Tip, val well: Well) extends HasTip with HasWell
//class TWV(val tip: Tip, val well: Well, val volume: LiquidVolume) extends HasTip with HasWell with HasVolume
//class TWVP(val tip: Tip, val well: Well, val volume: LiquidVolume, val policy: String) extends HasTip with HasWell with HasVolume with HasPolicy

//trait HasTipWellPolicy extends HasTip with HasWell with HasPolicy
//trait HasTipWell extends HasTip with HasWell
//trait HasTipWellVolume extends HasTipWell with HasVolume
//trait HasTipWellVolumePolicy extends HasTipWellVolume with HasPolicy

sealed class TipWell(val tip: Tip, val well: Well) extends HasTip with HasWell {
	override def toString = "TipWell("+(tip.index+1)+","+well+")" 
}

sealed class TipWellVolume(
		tip: Tip, well: Well,
		val volume: LiquidVolume
	) extends TipWell(tip, well) with HasVolume {
	override def toString = "TipWellVolume("+tip.index+","+well.id+","+volume+")" 
}

sealed class TipWellPolicy(tip: Tip, well: Well,
		val policy: PipettePolicy
	) extends TipWell(tip, well) with HasPolicy {
	override def toString = "TipWellPolicy("+tip.index+","+well.id+","+policy+")" 
}

sealed class TipWellVolumePolicy(tip: Tip, well: Well, volume: LiquidVolume,
		val policy: PipettePolicy
	) extends TipWellVolume(tip, well, volume) with HasPolicy {
	override def toString = "TipWellVolumePolicy("+tip.index+","+well.id+","+volume+","+policy+")" 
}

sealed class TipWellMix(tip: Tip, well: Well,
		val mixSpec: MixSpec
	) extends TipWell(tip, well) with HasMixSpec {
	override def toString = "TipWellMix("+tip.index+","+well.id+","+mixSpec+")" 
}
