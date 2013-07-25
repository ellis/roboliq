package roboliq.pipette.planners

import roboliq.core._
import roboliq.entities._
import roboliq.pipette.PipettePolicy
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
	def equidistant(items: Seq[HasTip with HasVessel]): Boolean = {
		val lVesselInfo = items.map(_.well).toList
		val l = items zip lVesselInfo
		equidistant2(l)
	}
		
	// Test all adjacent items for equidistance
	def equidistant2(tws: Seq[(HasTip, Vessel)]): Boolean = tws match {
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
	def equidistant3(a: Tuple2[HasTip, Vessel], b: Tuple2[HasTip, Vessel]): Boolean = {
		(b._1.tip.row - a._1.tip.row) == (b._2.row - a._2.row) &&
		(b._1.tip.col - a._1.tip.col) == (b._2.col - a._2.col) &&
		(b._2.plate == a._2.plate)
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
