package roboliq.entities

import roboliq.core._


trait HasTip { val tip: TipState }
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
	tip: TipState,
	well: Well
) extends HasTipWell {
	override def toString = s"TipWell(${tip.id},${well.key})"
}

object TipWell {
	// Test all adjacent items for equidistance
	def equidistant(items: Seq[HasTip with HasWell], state: WorldState): Boolean = {
		val lWellInfo = items.map(_.well).toList
		val l = items zip lWellInfo
		equidistant2(l, state)
	}
		
	// Test all adjacent items for equidistance
	def equidistant2(tws: Seq[(HasTip, Well)], state: WorldState): Boolean = tws match {
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
	def equidistant3(a: Tuple2[HasTip, Well], b: Tuple2[HasTip, Well], state: WorldState): Boolean = {
		(state.getWellPosition(a._2), state.getWellPosition(b._2)) match {
			case (Some(posA), Some(posB)) =>
				(posB.parent == posA.parent) && // wells are are same parent
				(b._1.tip.row - a._1.tip.row) == (posB.row - posA.row) && // tip rows and plate rows are equidistant
				(b._1.tip.col - a._1.tip.col) == (posB.col - posA.col) // tip columns and plate columns are equidistant
			case _ =>
				// REFACTOR: consider returning an error from this function instead
				false
		}
	}
}

case class TipWellVolume(
	tip: Tip,
	well: Well,
	volume: LiquidVolume
) extends HasTipWellVolume {
	override def toString = s"TipWellVolume(${tip.key},${well.key},$volume)"
}

case class TipWellPolicy(
	tip: Tip,
	well: Well,
	policy: PipettePolicy
) extends HasTipWellPolicy {
	override def toString = s"TipWellPolicy(${tip.key},${well.key},$policy)"
}

case class TipWellVolumePolicy(
	tip: Tip,
	well: Well,
	volume: LiquidVolume,
	policy: PipettePolicy
) extends HasTipWellVolumePolicy {
	override def toString = s"TipWellVolumePolicy(${tip.key},${well.key},$volume,$policy)"
}

/*
case class TipWellVolumePolicy0(
	tip: Tip,
	well: Well,
	volume: LiquidVolume,
	policy: PipettePolicy
) extends HasTipWellVolumePolicy {
	override def toString = s"TipWellVolumePolicy(${tip.key},${well.key},$volume,$policy)"
}
*/

case class TipWellMix(
	tip: Tip,
	well: Well,
	mixSpec: MixSpec
) extends HasTipWell with HasMixSpec {
	override def toString = "TipWellMix("+tip.key+","+well.key+","+mixSpec+")"
}
