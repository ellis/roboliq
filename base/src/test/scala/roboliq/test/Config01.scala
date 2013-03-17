package roboliq.test

import spray.json.JsonParser
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._

object Config01 {
	val tipModel1000 = TipModel("Standard 1000ul", LiquidVolume.ul(950), LiquidVolume.ul(4))
	val tip1 = Tip("TIP1", "LiHa", 0, 0, 0, Some(tipModel1000))
	val tip2 = Tip("TIP2", "LiHa", 1, 1, 0, Some(tipModel1000))
	val tip3 = Tip("TIP3", "LiHa", 2, 2, 0, Some(tipModel1000))
	val tip4 = Tip("TIP4", "LiHa", 3, 3, 0, Some(tipModel1000))
	val plateModel_PCR = PlateModel("D-BSSE 96 Well PCR Plate", 8, 12, LiquidVolume.ul(200))
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

	val benchJson = JsonParser(
"""{
"tipModel": [
	{ "id": "Standard 50ul", "volume": "45ul", "volumeMin": "0.01ul" },
	{ "id": "Standard 1000ul", "volume": "950ul", "volumeMin": "4ul" }
],
"tip": [
	{ "id": "TIP1", "deviceId": "LiHa", "index": 0, "row": 0, "col": 0, "permanent": "Standard 1000ul" },
	{ "id": "TIP2", "deviceId": "LiHa", "index": 1, "row": 1, "col": 0, "permanent": "Standard 1000ul" },
	{ "id": "TIP3", "deviceId": "LiHa", "index": 2, "row": 2, "col": 0, "permanent": "Standard 1000ul" },
	{ "id": "TIP4", "deviceId": "LiHa", "index": 3, "row": 3, "col": 0, "permanent": "Standard 1000ul" },
	{ "id": "TIP5", "deviceId": "LiHa", "index": 4, "row": 4, "col": 0, "permanent": "Standard 50ul" },
	{ "id": "TIP6", "deviceId": "LiHa", "index": 5, "row": 5, "col": 0, "permanent": "Standard 50ul" },
	{ "id": "TIP7", "deviceId": "LiHa", "index": 6, "row": 6, "col": 0, "permanent": "Standard 50ul" },
	{ "id": "TIP8", "deviceId": "LiHa", "index": 7, "row": 7, "col": 0, "permanent": "Standard 50ul" }
],
"pipettePolicy": [
	{ "id": "Water free dispense", "pos": "WetContact" }
],
"plateModel": [
	{ "id": "Reagent Cooled 8*50ml", "rows": 8, "cols": 1, "wellVolume": "50ml" },
	{ "id": "Reagent Cooled 8*15ml", "rows": 8, "cols": 1, "wellVolume": "15ml" },
	{ "id": "Block 20Pos 1.5 ml Eppendorf", "rows": 4, "cols": 5, "wellVolume": "1.5ml" },
	{ "id": "D-BSSE 96 Well PCR Plate", "rows": 8, "cols": 12, "wellVolume": "200ul" },
	{ "id": "D-BSSE 96 Well Costar Plate", "rows": 8, "cols": 12, "wellVolume": "350ul" },
	{ "id": "D-BSSE 96 Well DWP", "rows": 8, "cols": 12, "wellVolume": "1000ul" },
	{ "id": "Trough 100ml", "rows": 8, "cols": 1, "wellVolume": "100ml" },
	{ "id": "Ellis Nunc F96 MicroWell", "rows": 8, "cols": 12, "wellVolume": "400ul" }
],
"plateLocation": [
	{ "id": "trough1", "plateModels": ["Trough 100ml"], "cooled": false },
	{ "id": "trough2", "plateModels": ["Trough 100ml"], "cooled": false },
	{ "id": "trough3", "plateModels": ["Trough 100ml"], "cooled": false },
	{ "id": "reagents15000", "plateModels": ["Reagent Cooled 8*15ml"], "cooled": true },
	{ "id": "uncooled2_low", "plateModels": ["D-BSSE 96 Well DWP", "Ellis Nunc F96 MicroWell"] },
	{ "id": "uncooled2_high", "plateModels": ["D-BSSE 96 Well Costar Plate"] },
	{ "id": "shaker", "plateModels": ["D-BSSE 96 Well Costar Plate", "D-BSSE 96 Well DWP"] },
	{ "id": "cool1PCR", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cool2PCR", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cool3PCR", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cool4PCR", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cool5PCR", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "regrip", "plateModels": ["D-BSSE 96 Well PCR Plate", "D-BSSE 96 Well Costar Plate"] },
	{ "id": "reader", "plateModels": ["D-BSSE 96 Well Costar Plate"] }
],
"tubeModel": [
	{ "id": "Tube 50000ul", "volume": "50000ul" },
	{ "id": "Tube 15000ul", "volume": "15000ul" },
	{ "id": "Tube 1500ul", "volume": "1500ul" }
],
"tubeLocation": [
	{ "id": "reagents50000", "tubeModels": ["Tube 50000ul"], "rackModel": "Reagent Cooled 8*50ml" },
	{ "id": "reagents15000", "tubeModels": ["Tube 15000ul"], "rackModel": "Reagent Cooled 8*15ml" },
	{ "id": "reagents1500", "tubeModels": ["Tube 1500ul"], "rackModel": "Block 20Pos 1.5 ml Eppendorf" }
],


"washProgram": [
	{ "id": "Thorough", "intensity": "Thorough", "tips": ["TIP1", "TIP2", "TIP3", "TIP4"] }
],

"plate": [
	{ "id": "P_reagents50000", "model": "Reagent Cooled 8*50ml", "location": "reagents50000" },
	{ "id": "P_reagents15000", "model": "Reagent Cooled 8*15ml", "location": "reagents15000" },
	{ "id": "P_reagents1500", "model": "Block 20Pos 1.5 ml Eppendorf", "location": "reagents1500" }
],
"plateState": [
	{ "id": "P_reagents15000", "location": "reagents15000" }
]
}""").asJsObject
	
	val database1Json = JsonParser("""{
"substance": [
	{ "id": "water", "kind": "Liquid", "tipCleanPolicy": "ThoroughNone", "molarity": 55, "gramPerMole": 18 }
]
}""").asJsObject

	val protocol1Json = JsonParser(
"""{
"plate": [
	{ "id": "P_1", "model": "D-BSSE 96 Well PCR Plate" }
],
"vessel": [
	{ "id": "P_1(A01)" },
	{ "id": "T_1", "tubeModel": "Tube 15000ul" }
],
"plateState": [
	{ "id": "P_1", "location": "cool1PCR" }
],
"vesselState": [
	{ "id": "T_1", "content": {} },
	{ "id": "P_1(A01)", "content": { "water": "100ul" } }
],
"vesselSituatedState": [
	{ "id": "T_1", "position": { "plate": "P_reagents15000", "index": 0 } },
	{ "id": "P_1(A01)", "position": { "plate": "P_1", "index": 0 } }
]
}""").asJsObject
}
