package roboliq.commands.pipette

import roboliq.common._


sealed trait WellOrPlate
case class WP_Well(well: Well) extends WellOrPlate
case class WP_Plate(plate: Plate) extends WellOrPlate

sealed trait WellOrPlateOrLiquid
case class WPL_Well(well: Well) extends WellOrPlateOrLiquid
case class WPL_Plate(plate: Plate) extends WellOrPlateOrLiquid
case class WPL_Liquid(liquid: Liquid) extends WellOrPlateOrLiquid

case class MixSpec(
		val nVolume: Double,
		val nCount: Int,
		val policy_? : Option[PipettePolicy] = None
		)

//class TipWellL2(val tip: TipStateL1, val well: WellL2) {
	//override def toString = "TipWell("+tip.conf.index+","+well.holder.conf.sLabel+":"+well.conf.index+")" 
//}
