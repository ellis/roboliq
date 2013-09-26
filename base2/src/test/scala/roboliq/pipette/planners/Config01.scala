package roboliq.pipette.planners

import roboliq.core._
import roboliq.entities._


object Config01 {
	val tipModel1000 = TipModel("Standard 1000ul", None, None, LiquidVolume.ul(950), LiquidVolume.ul(4))
	val tip1 = Tip("TIP1", None, None, 0, 0, 0, Some(tipModel1000))
	val tip2 = Tip("TIP2", None, None, 1, 1, 0, Some(tipModel1000))
	val tip3 = Tip("TIP3", None, None, 2, 2, 0, Some(tipModel1000))
	val tip4 = Tip("TIP4", None, None, 3, 3, 0, Some(tipModel1000))
	val plateModel_PCR = PlateModel("D-BSSE 96 Well PCR Plate", None, None, 8, 12, LiquidVolume.ul(200))
	val plateModel_Nunc = PlateModel("Ellis Nunc F96 MicroWell", None, None, 8, 12, LiquidVolume.ul(400))
	val plateModel_15000 = PlateModel("Reagent Cooled 8*15ml", None, None, 8, 1, LiquidVolume.ml(15))
	val plateLocation_cooled1 = Site("cool1PCR", List(plateModel_PCR), true)
	val plateLocation_cooled2 = Site("cool2PCR", List(plateModel_PCR), true)
	val plateLocation_15000 = Site("reagents15000", List(plateModel_15000), true)
	val tubeModel_15000 = TubeModel("Tube 15000ul", None, None, LiquidVolume.ml(15))
	val plate_15000 = Plate("P_reagents15000", plateModel_15000, None)
	val plateState_15000 = PlateState(plate_15000, Some(plateLocation_15000))

	val water = Substance.liquid("water", 55, TipCleanPolicy.TN, gramPerMole_? = Some(18))

	val plate_P1 = Plate("P_1", plateModel_PCR, None)
	val vessel_P1_A01 = Well("P_1(A01)", None)
	val vessel_P1_B01 = Well("P_1(B01)", None)
	val vessel_P1_C01 = Well("P_1(C01)", None)
	val vessel_P1_D01 = Well("P_1(D01)", None)
	val vessel_T1 = Well("T_1", Some(tubeModel_15000))
	val tipState1 = TipState.createEmpty(tip1)
	val tipState2 = TipState.createEmpty(tip2)
	val plateState_P1 = PlateState(plate_P1, Some(plateLocation_cooled1))
	val vesselState_T1 = WellState(vessel_T1, WellContent.Empty)
	val vesselState_P1_A01 = WellState(vessel_P1_A01, WellContent.fromVolume(water, LiquidVolume.ul(100)).getOrElse(null))
	val vesselSituatedState_T1 = WellSituatedState(vesselState_T1, WellPosition(plateState_15000, 0))
}
