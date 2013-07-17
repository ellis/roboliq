package roboliq.pipette

import roboliq.core._

object Config01 {
	val tipModel1000 = TipModel("Standard 1000ul", LiquidVolume.ul(950), LiquidVolume.ul(4))
	val tip1 = Tip("TIP1", "LiHa", 0, 0, 0, Some(tipModel1000))
	val tip2 = Tip("TIP2", "LiHa", 1, 1, 0, Some(tipModel1000))
	val tip3 = Tip("TIP3", "LiHa", 2, 2, 0, Some(tipModel1000))
	val tip4 = Tip("TIP4", "LiHa", 3, 3, 0, Some(tipModel1000))
	val plateModel_PCR = PlateModel("D-BSSE 96 Well PCR Plate", 8, 12, LiquidVolume.ul(200))
	val plateModel_Nunc = PlateModel("Ellis Nunc F96 MicroWell", 8, 12, LiquidVolume.ul(400))
	val plateModel_15000 = PlateModel("Reagent Cooled 8*15ml", 8, 1, LiquidVolume.ml(15))
	val plateLocation_cooled1 = PlateLocation("cool1PCR", List(plateModel_PCR), true)
	val plateLocation_cooled2 = PlateLocation("cool2PCR", List(plateModel_PCR), true)
	val plateLocation_15000 = PlateLocation("reagents15000", List(plateModel_15000), true)
	val tubeModel_15000 = TubeModel("Tube 15000ul", LiquidVolume.ml(15))
	val plate_15000 = Plate("P_reagents15000", plateModel_15000, None)
	val plateState_15000 = PlateState(plate_15000, Some(plateLocation_15000))

	val water = Substance.liquid("water", 55, TipCleanPolicy.TN, gramPerMole_? = Some(18))

	val plate_P1 = Plate("P_1", plateModel_PCR, None)
	val vessel_P1_A01 = Vessel("P_1(A01)", None)
	val vessel_P1_B01 = Vessel("P_1(B01)", None)
	val vessel_P1_C01 = Vessel("P_1(C01)", None)
	val vessel_P1_D01 = Vessel("P_1(D01)", None)
	val vessel_T1 = Vessel("T_1", Some(tubeModel_15000))
	val tipState1 = TipState.createEmpty(tip1)
	val tipState2 = TipState.createEmpty(tip2)
	val plateState_P1 = PlateState(plate_P1, Some(plateLocation_cooled1))
	val vesselState_T1 = VesselState(vessel_T1, VesselContent.Empty)
	val vesselState_P1_A01 = VesselState(vessel_P1_A01, VesselContent.fromVolume(water, LiquidVolume.ul(100)).getOrElse(null))
	val vesselSituatedState_T1 = VesselSituatedState(vesselState_T1, VesselPosition(plateState_15000, 0))
}
