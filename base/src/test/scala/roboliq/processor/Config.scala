package roboliq.processor

import spray.json.JsonParser

object Config {
	val config01 = JsonParser(
"""{
"tipModel": [
	{ "id": "Standard 50ul", "volume": "45ul", "volumeMin": "0.01ul" },
	{ "id": "Standard 1000ul", "volume": "950ul", "volumeMin": "4ul" }
],
"tip": [
	{ "id": "TIP1", "index": 0, "permanent": "Standard 1000ul" },
	{ "id": "TIP2", "index": 1, "permanent": "Standard 1000ul" },
	{ "id": "TIP3", "index": 2, "permanent": "Standard 1000ul" },
	{ "id": "TIP4", "index": 3, "permanent": "Standard 1000ul" },
	{ "id": "TIP5", "index": 4, "permanent": "Standard 50ul" },
	{ "id": "TIP6", "index": 5, "permanent": "Standard 50ul" },
	{ "id": "TIP7", "index": 6, "permanent": "Standard 50ul" },
	{ "id": "TIP8", "index": 7, "permanent": "Standard 50ul" }
],
"plateModel": [
	{ "id": "Reagent Cooled 8*50ml", "rows": 8, "cols": 1, "wellVolume": "50ml" },
	{ "id": "Reagent Cooled 8*15ml", "rows": 8, "cols": 1, "wellVolume": "15ml" },
	{ "id": "Block 20Pos 1.5 ml Eppendorf", "rows": 4, "cols": 5, "wellVolume": "1.5ml" },
	{ "id": "D-BSSE 96 Well PCR Plate", "rows": 8, "cols": 12, "wellVolume": "200ul" },
	{ "id": "D-BSSE 96 Well Costar Plate", "rows": 8, "cols": 12, "wellVolume": "350ul" },
	{ "id": "D-BSSE 96 Well DWP", "rows": 8, "cols": 12, "wellVolume": "1000ul" },
	{ "id": "Trough 100ml", "rows": 8, "cols": 1, "wellVolume": "100ul" },
	{ "id": "Ellis Nunc F96 MicroWell", "rows": 8, "cols": 12, "wellVolume": "400ul" }
],
"plateLocation": [
	{ "id": "trough1", "plateModels": ["Trough 100ml"] },
	{ "id": "trough2", "plateModels": ["Trough 100ml"] },
	{ "id": "trough3", "plateModels": ["Trough 100ml"] },
	{ "id": "reagents15000", "plateModels": ["Reagent Cooled 8*15ml"], "cooled": true },
	{ "id": "uncooled2_low", "plateModels": ["D-BSSE 96 Well DWP", "Ellis Nunc F96 MicroWell"] },
	{ "id": "uncooled2_high", "plateModels": ["D-BSSE 96 Well Costar Plate"] },
	{ "id": "shaker", "plateModels": ["D-BSSE 96 Well Costar Plate", "D-BSSE 96 Well DWP"] },
	{ "id": "cooled1", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cooled2", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cooled3", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cooled4", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cooled5", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "regrip", "plateModels": ["D-BSSE 96 Well PCR Plate", "D-BSSE 96 Well Costar Plate"] },
	{ "id": "reader", "plateModels": ["D-BSSE 96 Well Costar Plate"] }
],
"tubeModel": [
	{ "id": "Tube 50000ul", "volume": "50000ul" },
	{ "id": "Tube 15000ul", "volume": "15000ul" },
	{ "id": "Tube 1500ul", "volume": "1500ul" }
],
"tubeLocation": [
	{ "id": "reagents50", "tubeModels": ["Tube 50000ul"], "rackModel": "Reagent Cooled 8*50ml" },
	{ "id": "reagents15000", "tubeModels": ["Tube 15000ul"], "rackModel": "Reagent Cooled 8*15ml" },
	{ "id": "reagents1.5", "tubeModels": ["Tube 1500ul"], "rackModel": "Block 20Pos 1.5 ml Eppendorf" }
],
"plate": [
	{ "id": "reagents50", "model": "Reagent Cooled 8*50ml", "location": "reagents50" },
	{ "id": "reagents15000", "model": "Reagent Cooled 8*15ml", "location": "reagents15000" },
	{ "id": "reagents1.5", "model": "Block 20Pos 1.5 ml Eppendorf", "location": "reagents1.5" },
	{ "id": "P1", "model": "D-BSSE 96 Well PCR Plate" }
],
"vessel": [
	{ "id": "T1", "tubeModel": "Tube 15000ul" },
	{ "id": "P1(A01)" }
],

"plateState": [
	{ "id": "reagents15000", "location": "reagents15000" },
	{ "id": "P1", "location": "cooled1" }
],
"vesselState": [
	{ "id": "T1", "content": { "idVessel": "T1" } },
	{ "id": "P1(A01)", "content": { "idVessel": "T1", "solventToVolume": { "water": "100ul" } } }
],
"vesselSituatedState": [
	{ "id": "T1", "position": { "plate": "reagents15000", "index": 0 } },
	{ "id": "P1(A01)", "position": { "plate": "P1", "index": 0 } }
]
}""").asJsObject
}
