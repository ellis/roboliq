package roboliq.test.example

import spray.json.JsonParser
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._
import roboliq.commands.pipette._
import roboliq.commands.CommandSpecBase
import roboliq.test.Config01

class Example002Spec extends CommandSpecBase {
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
					{ "id": "P_EW_water", "model": "Trough 100ml" },
					{ "id": "P_EW_dye", "model": "Trough 100ml" },
					{ "id": "P_SCRAP1", "model": "Ellis Nunc F96 MicroWell" }
				],
				"plateState": [
					{ "id": "P_EW_water", "location": "trough1" },
					{ "id": "P_EW_dye", "location": "trough2" },
					{ "id": "P_SCRAP1", "location": "pipette1" }
				],
				"vesselState": [
					{ "id": "P_EW_water(A01)", "content": { "S_water": "10ml" }, "isSource": true },
					{ "id": "P_EW_water(B01)", "content": { "S_water": "10ml" }, "isSource": true },
					{ "id": "P_EW_water(C01)", "content": { "S_water": "10ml" }, "isSource": true },
					{ "id": "P_EW_water(D01)", "content": { "S_water": "10ml" }, "isSource": true }
				],
				"cmd": [
					{ "cmd": "pipette.transfer", "source": ["S_water"], "destination": ["P_SCRAP1(A01xH01)"], "amount": ["50ul"], "pipettePolicy": "Water free dispense" }
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