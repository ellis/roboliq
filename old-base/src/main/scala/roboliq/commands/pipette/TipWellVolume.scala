package roboliq.commands.pipette

import roboliq.core._


trait HasTip { val tip: Tip }
trait HasWell { val well: Well }
trait HasVolume { val volume: LiquidVolume }
trait HasPolicy { val policy: PipettePolicy }
trait HasMixSpec { val mixSpec: MixSpec }

trait HasTipWell extends HasTip with HasWell
trait HasTipWellVolume extends HasTipWell with HasVolume
trait HasTipWellPolicy extends HasTipWell with HasPolicy
trait HasTipWellVolumePolicy extends HasTipWellVolume with HasPolicy

//class TW(val tip: Tip, val well: Well) extends HasTip with HasWell
//class TWV(val tip: Tip, val well: Well, val volume: LiquidVolume) extends HasTip with HasWell with HasVolume
//class TWVP(val tip: Tip, val well: Well, val volume: LiquidVolume, val policy: String) extends HasTip with HasWell with HasVolume with HasPolicy

case class TipWell(
	tip: Tip,
	well: Well
) extends HasTipWell {
	override def toString = s"TipWell(${tip.id},${well.id})"
}

case class TipWellVolume(
	tip: Tip,
	well: Well,
	volume: LiquidVolume
) extends HasTipWellVolume {
	override def toString = s"TipWellVolume(${tip.id},${well.id},$volume)"
}

case class TipWellPolicy(
	tip: Tip,
	well: Well,
	policy: PipettePolicy
) extends HasTipWellPolicy {
	override def toString = s"TipWellPolicy(${tip.id},${well.id},$policy)"
}

case class TipWellVolumePolicy(
	tip: Tip,
	well: Well,
	volume: LiquidVolume,
	policy: PipettePolicy
) extends HasTipWellVolumePolicy {
	override def toString = s"TipWellVolumePolicy(${tip.id},${well.id},$volume,$policy)"
}

case class TipWellMix(
	tip: Tip,
	well: Well,
	mixSpec: MixSpec
) extends HasTipWell with HasMixSpec {
	override def toString = "TipWellMix("+tip.id+","+well.id+","+mixSpec+")"
}
