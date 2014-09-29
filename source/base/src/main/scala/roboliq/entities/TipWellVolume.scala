package roboliq.entities

import roboliq.core._
import scala.annotation.tailrec


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
	override def toString = s"TipWell(${tip.key},${well.key})"
}

object TipWell {
	// Test all adjacent items for equidistance
	def equidistant(items: Seq[HasTip with HasWell], state: WorldState): Boolean = {
		val lWellInfo = items.map(_.well).toList
		val l = items zip lWellInfo
		equidistant2(l, state, 1) || equidistant2(l, state, 2)
	}
	
	// Test all adjacent items for equidistance
	@tailrec
	def equidistant2(tws: Seq[(HasTip, Well)], state: WorldState, tipSpreadRow: Int): Boolean = tws match {
		case Seq() => true
		case Seq(_) => true
		case Seq(a, b, rest @ _*) =>
			equidistant2sub(a, b, state, tipSpreadRow) match {
				case false => false
				case true => equidistant2(Seq(b) ++ rest, state, tipSpreadRow)
			}
	}
	
	// All tip/well pairs are equidistant or all tips are going to the same well
	// Assert that tips are spaced at equal distances to each other as the wells are to each other
	private def equidistant2sub(a: Tuple2[HasTip, Well], b: Tuple2[HasTip, Well], state: WorldState, tipSpreadRow: Int): Boolean = {
		(state.getWellPosition(a._2), state.getWellPosition(b._2)) match {
			case (RsSuccess(posA, _), RsSuccess(posB, _)) =>
				equidistantItems((a._1, posA), (b._1, posB), tipSpreadRow)
			case _ =>
				// REFACTOR: consider returning an error from this function instead
				false
		}
	}
	
	// Test all adjacent items for equidistance
	def equidistant(tws: Seq[(HasTip, WellPosition)]): Boolean = {
		equidistant(tws, 1) || equidistant(tws, 2)
	}
	
	@tailrec
	private def equidistant(tws: Seq[(HasTip, WellPosition)], tipSpreadRow: Int): Boolean = tws match {
		case Seq() => true
		case Seq(_) => true
		case Seq(a, b, rest @ _*) =>
			equidistantItems(a, b, tipSpreadRow) match {
				case false => false
				case true => equidistant(Seq(b) ++ rest, tipSpreadRow)
			}
	}
	
	// All tip/well pairs are equidistant or all tips are going to the same well
	// Assert that tips are spaced at equal distances to each other as the wells are to each other
	private def equidistantItems(a: Tuple2[HasTip, WellPosition], b: Tuple2[HasTip, WellPosition], tipSpreadRow: Int): Boolean = {
		val posA = a._2
		val posB = b._2
		(posB.parent == posA.parent) && // wells are are same parent
		(b._1.tip.row - a._1.tip.row) * tipSpreadRow == (posB.row - posA.row) && // tip rows and plate rows are equidistant
		(b._1.tip.col - a._1.tip.col) == (posB.col - posA.col) // tip columns and plate columns are equidistant
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
