package roboliq.test.example

import spray.json.JsonParser
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._
import roboliq.commands.pipette._
import roboliq.commands.CommandSpecBase
import roboliq.test.Config01

class Example001Spec extends CommandSpecBase {
	describe("BSSE configuration") {
		implicit val p = makeProcessorBsse(
			//Config01.database1Json,
			JsonParser("""{
				"substance": [
					{ "id": "S_water", "kind": "Liquid", "tipCleanPolicy": "ThoroughNone", "molarity": 55, "gramPerMole": 18 }
				]
				}""").asJsObject,
			JsonParser("""{
				"plate": [
					{ "id": "P_water", "model": "Trough 100ml" },
					{ "id": "P_dye", "model": "Trough 100ml" },
					{ "id": "P_1", "model": "D-BSSE 96 Well PCR Plate" }
				],
				"plateState": [
					{ "id": "P_water", "location": "trough1" },
					{ "id": "P_dye", "location": "trough2" },
					{ "id": "P_1", "location": "cool1PCR" }
				],
				"vesselState": [
					{ "id": "P_water(A01)", "content": { "S_water": "10ml" }, "isSource": true },
					{ "id": "P_water(B01)", "content": { "S_water": "10ml" }, "isSource": true },
					{ "id": "P_water(C01)", "content": { "S_water": "10ml" }, "isSource": true },
					{ "id": "P_water(D01)", "content": { "S_water": "10ml" }, "isSource": true }
				],
				"cmd": [
				  { "cmd": "pipette.transfer", "source": ["S_water"], "destination": ["P_1(A01dD01)"], "amount": ["50ul"], "pipettePolicy": "Water free dispense" }
				]
				}""").asJsObject
		)
			
		val policy = getObj[PipettePolicy]("Water free dispense")
		
		it("should have no errors or warnings") {
			assert(p.getMessages === Nil)
		}
		
		it("should generate correct tokens") {
			val (time_l, token_l) = p.getTokenList.unzip
			val tipState_1_A = getState[TipState]("TIP1", time_l(0))
			val tipState_1_B = getState[TipState]("TIP1", time_l(1))
			val tipState_1_C = getState[TipState]("TIP1", time_l(2))
			val vss_P1_A01_1 = getState[VesselSituatedState]("P_1(A01)", List(1))
			val vss_P1_B01_1 = getState[VesselSituatedState]("P_1(B01)", List(1))
			assert(token_l === List(
				low.WashTipsToken("Thorough", List(tipState_1_A)),
				low.AspirateToken(List(
					TipWellVolumePolicy(tipState_1_B, vss_P1_A01_1, LiquidVolume.ul(50), policy)
				)),
				low.DispenseToken(List(
					TipWellVolumePolicy(tipState_1_C, vss_P1_B01_1, LiquidVolume.ul(50), policy)
				))
			))
		}
	}
}