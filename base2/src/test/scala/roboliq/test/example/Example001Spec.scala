package roboliq.test.example

import spray.json.JsonParser
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._
import roboliq.commands.pipette._
import roboliq.commands.CommandSpecBase
import roboliq.test.Config01

class Example001Spec extends CommandSpecBase {
	ignore("BSSE configuration") {
		implicit val p = makeProcessorBsse(
			//Config01.database1Json,
			JsonParser("""{
				"substance": [
					{ "id": "water", "kind": "Liquid", "tipCleanPolicy": "ThoroughNone", "molarity": 55, "gramPerMole": 18 }
				]
				}""").asJsObject,
			JsonParser("""{
				"cmd": [
					{ "cmd": "resource.liquid", "id": "L_water", "liquid": { "water": 1 }, "vessels": ["P_EW_water(A01)", "P_EW_water(B01)", "P_EW_water(C01)", "P_EW_water(D01)"] },
					{ "cmd": "resource.plate", "id": "P_SCRAP1", "model": "Ellis Nunc F96 MicroWell", "location": "pipette1" },
					{ "cmd": "resource.plate", "id": "P_EW_water", "model": "Trough 100ml", "location": "trough1" },
					{ "cmd": "pipette.transfer", "source": ["L_water"], "destination": ["P_SCRAP1"], "amount": ["50ul"], "pipettePolicy": "Water free dispense" }
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