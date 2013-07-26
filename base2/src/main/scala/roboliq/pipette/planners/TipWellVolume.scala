package roboliq.pipette.planners

import roboliq.entities.LiquidVolume
import roboliq.entities.Tip
import roboliq.entities.Vessel
import roboliq.entities.WorldState
import roboliq.pipette.MixSpec


trait HasTip { val tip: Tip }
trait HasVessel { val well: Vessel }
trait HasVolume { val volume: LiquidVolume }
trait HasPolicy { val policy: PipettePolicy }
trait HasMixSpec { val mixSpec: MixSpec }

trait HasTipWell extends HasTip with HasVessel
trait HasTipWellVolume extends HasTipWell with HasVolume
trait HasTipWellPolicy extends HasTipWell with HasPolicy
trait HasTipWellVolumePolicy extends HasTipWellVolume with HasPolicy

//class TW(val tip: Tip, val well: Vessel) extends HasTip with HasVessel
//class TWV(val tip: Tip, val well: Vessel, val volume: LiquidVolume) extends HasTip with HasVessel with HasVolume
//class TWVP(val tip: Tip, val well: Vessel, val volume: LiquidVolume, val policy: String) extends HasTip with HasVessel with HasVolume with HasPolicy

case class TipWell(
	tip: Tip,
	well: Vessel
) extends HasTipWell {
	override def toString = s"TipWell(${tip.key},${well.key})"
}

object TipWell {
	// Test all adjacent items for equidistance
	def equidistant(items: Seq[HasTip with HasVessel], state: WorldState): Boolean = {
		val lVesselInfo = items.map(_.well).toList
		val l = items zip lVesselInfo
		equidistant2(l, state)
	}
		
	// Test all adjacent items for equidistance
	def equidistant2(tws: Seq[(HasTip, Vessel)], state: WorldState): Boolean = tws match {
		case Seq() => true
		case Seq(_) => true
		case Seq(a, b, rest @ _*) =>
			equidistant3(a, b, state) match {
				case false => false
				case true => equidistant2(Seq(b) ++ rest, state)
			}
	}
	
	// All tip/well pairs are equidistant or all tips are going to the same well
	// Assert that tips are spaced at equal distances to each other as the wells are to each other
	def equidistant3(a: Tuple2[HasTip, Vessel], b: Tuple2[HasTip, Vessel], state: WorldState): Boolean = {
		(state.getWellPosition(a._2), state.getWellPosition(b._2)) match {
			case (Some(posA), Some(posB)) =>
				(b._1.tip.row - a._1.tip.row) == (posB.row - posA.row) &&
				(b._1.tip.col - a._1.tip.col) == (posB.col - posA.col) &&
				(posB.parent == posA.parent)
			case _ =>
				// REFACTOR: consider returning an error from this function instead
				false
		}
	}
}

case class TipWellVolume(
	tip: Tip,
	well: Vessel,
	volume: LiquidVolume
) extends HasTipWellVolume {
	override def toString = s"TipWellVolume(${tip.key},${well.key},$volume)"
}

case class TipWellPolicy(
	tip: Tip,
	well: Vessel,
	policy: PipettePolicy
) extends HasTipWellPolicy {
	override def toString = s"TipWellPolicy(${tip.key},${well.key},$policy)"
}

case class TipWellVolumePolicy(
	tip: Tip,
	well: Vessel,
	volume: LiquidVolume,
	policy: PipettePolicy
) extends HasTipWellVolumePolicy {
	override def toString = s"TipWellVolumePolicy(${tip.key},${well.key},$volume,$policy)"
}

/*
case class TipWellVolumePolicy0(
	tip: Tip,
	well: Vessel,
	volume: LiquidVolume,
	policy: PipettePolicy
) extends HasTipWellVolumePolicy {
	override def toString = s"TipWellVolumePolicy(${tip.key},${well.key},$volume,$policy)"
}
*/

case class TipWellMix(
	tip: Tip,
	well: Vessel,
	mixSpec: MixSpec
) extends HasTipWell with HasMixSpec {
	override def toString = "TipWellMix("+tip.key+","+well.key+","+mixSpec+")"
}
