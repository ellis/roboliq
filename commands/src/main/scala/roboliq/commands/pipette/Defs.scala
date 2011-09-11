package roboliq.commands.pipette

import roboliq.common._


trait HasTip { val tip: TipConfigL2 }
trait HasWell { val well: WellConfigL2 }
trait HasVolume { val nVolume: Double }
trait HasPolicy { val policy: PipettePolicy }
trait HasTipWell extends HasTip with HasWell
trait HasTipWellVolume extends HasTipWell with HasVolume
trait HasTipWellVolumePolicy extends HasTipWellVolume with HasPolicy

sealed trait WellOrPlate
case class WP_Well(well: Well) extends WellOrPlate
case class WP_Plate(plate: Plate) extends WellOrPlate

sealed trait WellOrPlateOrLiquid
case class WPL_Well(well: Well) extends WellOrPlateOrLiquid
case class WPL_Plate(plate: Plate) extends WellOrPlateOrLiquid
case class WPL_Liquid(liquid: Reagent) extends WellOrPlateOrLiquid

case class MixSpec(
	val nVolume: Double,
	val nCount: Int,
	val mixPolicy_? : Option[PipettePolicy] = None
)

case class TipModel(
	val id: String,
	val nVolume: Double, 
	val nVolumeAspirateMin: Double, 
	val nVolumeWashExtra: Double,
	val nVolumeDeconExtra: Double
)

sealed class TipWell(val tip: TipConfigL2, val well: WellConfigL2) extends HasTip {
	override def toString = "TipWell("+(tip.index+1)+","+well+")" 
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
/*
sealed class TipWellVolumePolicyCount(tip: TipConfigL2, well: WellConfigL2, nVolume: Double, liquid: Liquid, policy: PipettePolicy,
		val nCount: Int
	) extends L2A_AspirateItem(tip, well, liquid, nVolume, policy) {
	override def toString = "TipWellVolumePolicyCount("+tip.index+","+well.holder.hashCode()+":"+well.index+","+nVolume+","+policy+","+nCount+")" 
}
*/

object PipettePosition extends Enumeration {
	val Free, WetContact, DryContact = Value
}

//case class PipetteSpec(sName: String, aspirate: PipettePosition.Value, dispense: PipettePosition.Value, mix: PipettePosition.Value)

case class PipettePolicy(id: String, pos: PipettePosition.Value)

object TipReplacementPolicy extends Enumeration { // FIXME: Replace this with TipReplacementPolicy following Roboease
	val ReplaceAlways, KeepBetween, KeepAlways = Value
}

class TipHandlingOverrides(
	val replacement_? : Option[TipReplacementPolicy.Value],
	//val washProgram_? : Option[Int],
	val washIntensity_? : Option[WashIntensity.Value],
	val contamInside_? : Option[Set[Contaminant.Value]],
	val contamOutside_? : Option[Set[Contaminant.Value]]
)

object TipHandlingOverrides {
	def apply() = new TipHandlingOverrides(None, None, None, None)
}

class WashSpec(
	val washIntensity: WashIntensity.Value,
	val contamInside: Set[Contaminant.Value],
	val contamOutside: Set[Contaminant.Value]
)

class CleanSpec(
	val replacement: Option[TipReplacementPolicy.Value],
	val washIntensity: WashIntensity.Value,
	val contamInside: Set[Contaminant.Value],
	val contamOutside: Set[Contaminant.Value]
)
