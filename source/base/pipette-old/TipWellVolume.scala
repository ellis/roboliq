package roboliq.pipette

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
	override def toString = s"TipWell(${tip.id},${well.id})"
}

object TipWell {
	// Test all adjacent items for equidistance
	def equidistant(items: Seq[HasTip with HasWell]): Boolean = {
		val lWellInfo = items.map(_.well).toList
		val l = items zip lWellInfo
		equidistant2(l)
	}
		
	// Test all adjacent items for equidistance
	def equidistant2(tws: Seq[(HasTip, Well)]): Boolean = tws match {
		case Seq() => true
		case Seq(_) => true
		case Seq(a, b, rest @ _*) =>
			equidistant3(a, b) match {
				case false => false
				case true => equidistant2(Seq(b) ++ rest)
			}
	}
	
	// All tip/well pairs are equidistant or all tips are going to the same well
	// Assert that tips are spaced at equal distances to each other as the wells are to each other
	def equidistant3(a: Tuple2[HasTip, Well], b: Tuple2[HasTip, Well]): Boolean = {
		(b._1.tip.row - a._1.tip.row) == (b._2.row - a._2.row) &&
		(b._1.tip.col - a._1.tip.col) == (b._2.col - a._2.col) &&
		(b._2.plate == a._2.plate)
	}
}

case class TipWellVolume(
	tip: TipState,
	well: Well,
	volume: LiquidVolume
) extends HasTipWellVolume {
	override def toString = s"TipWellVolume(${tip.id},${well.id},$volume)"
}

case class TipWellPolicy(
	tip: TipState,
	well: Well,
	policy: PipettePolicy
) extends HasTipWellPolicy {
	override def toString = s"TipWellPolicy(${tip.id},${well.id},$policy)"
}

case class TipWellVolumePolicy(
	tip: TipState,
	well: Well,
	volume: LiquidVolume,
	policy: PipettePolicy
) extends HasTipWellVolumePolicy {
	override def toString = s"TipWellVolumePolicy(${tip.id},${well.id},$volume,$policy)"
}

case class TipWellMix(
	tip: TipState,
	well: Well,
	mixSpec: MixSpec
) extends HasTipWell with HasMixSpec {
	override def toString = "TipWellMix("+tip.id+","+well.id+","+mixSpec+")"
}
