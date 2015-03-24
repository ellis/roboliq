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
	val siteModel_cooledPcr = SiteModel("cooledPcr")
	val plateLocation_cooled1 = Site("cool1PCR")
	val plateLocation_cooled2 = Site("cool2PCR")
	// TODO: val plateLocation_15000 = Site("reagents15000", List(plateModel_15000), true)
	// TODO: val tubeModel_15000 = TubeModel("Tube 15000ul", None, None, LiquidVolume.ml(15))
	val plate_15000 = Plate("P_reagents15000")
	// TODO: val plateState_15000 = PlateState(plate_15000, Some(plateLocation_15000))

	val water = Substance.liquid("water", 55, TipCleanPolicy.TN, gramPerMole_? = Some(18))

	val plate_P1 = Plate("P_1")
	// TODO: val vessel_P1_A01 = Well("P_1(A01)", None)
	// TODO: val vessel_P1_B01 = Well("P_1(B01)", None)
	// TODO: val vessel_P1_C01 = Well("P_1(C01)", None)
	// TODO: val vessel_P1_D01 = Well("P_1(D01)", None)
	// TODO: val vessel_T1 = Well("T_1", Some(tubeModel_15000))
	// TODO: val tipState1 = TipState.createEmpty(tip1)
	// TODO: val tipState2 = TipState.createEmpty(tip2)
	// TODO: val vesselState_T1 = WellState(vessel_T1, WellContent.Empty)
	// TODO: val vesselState_P1_A01 = WellState(vessel_P1_A01, WellContent.fromVolume(water, LiquidVolume.ul(100)).getOrElse(null))
	// TODO: val vesselSituatedState_T1 = WellSituatedState(vesselState_T1, WellPosition(plateState_15000, 0))
	
	val eb = new EntityBase
	eb.addModel(siteModel_cooledPcr, "siteModel_cooledPcr")
	eb.addModel(plateModel_PCR, "plateModel_PCR")
	eb.addModel(plateModel_Nunc, "plateModel_Nunc")
	eb.addModel(plateModel_15000, "plateModel_15000")
	eb.addStackable(siteModel_cooledPcr, plateModel_PCR)
	eb.addSite(plateLocation_cooled1, "cooled1")
	eb.addSite(plateLocation_cooled2, "cooled2")
	eb.addLabware(plate_15000, "plate_15000")
	eb.addLabware(plate_P1, "P1")
	eb.setModel(plate_15000, plateModel_15000)
	eb.setModel(plate_P1, plateModel_PCR)
	eb.setLocation(plate_P1, plateLocation_cooled1)
	/*val state0 = {
		val state = new WorldStateBuilder
		state.
	}*/
}
