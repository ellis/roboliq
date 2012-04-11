package roboliq.commands.pipette

import roboliq.core._

trait HasTip { val tip: Tip }
trait HasWell { val well: Well2 }
trait HasVolume { val nVolume: LiquidVolume }
trait HasPolicy { val policy: PipettePolicy }
trait HasMixSpec { val mixSpec: MixSpec }
trait HasTipWell extends HasTip with HasWell
trait HasTipWellVolume extends HasTipWell with HasVolume
trait HasTipWellVolumePolicy extends HasTipWellVolume with HasPolicy

sealed class TipWell(val tip: Tip, val well: Well2) extends HasTipWell {
	override def toString = "TipWell("+(tip.index+1)+","+well+")" 
}

sealed class TipWellVolume(
		tip: Tip, well: Well2,
		val nVolume: LiquidVolume
	) extends TipWell(tip, well) {
	override def toString = "TipWellVolume("+tip.index+","+well.id+","+nVolume+")" 
}

sealed class TipWellVolumePolicy(tip: Tip, well: Well2, nVolume: LiquidVolume,
		val policy: PipettePolicy
	) extends TipWellVolume(tip, well, nVolume) with HasTipWellVolumePolicy {
	override def toString = "TipWellVolumePolicy("+tip.index+","+well.id+","+nVolume+","+policy+")" 
}

sealed class TipWellMix(tip: Tip, well: Well2,
		val mixSpec: MixSpec
	) extends TipWell(tip, well) with HasMixSpec {
	override def toString = "TipWellMix("+tip.index+","+well.id+","+mixSpec+")" 
}
