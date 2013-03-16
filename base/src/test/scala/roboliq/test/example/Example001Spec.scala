package roboliq.test.example

import spray.json.JsonParser
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._
import roboliq.commands.pipette._
import roboliq.commands.CommandSpecBase
import roboliq.test.Config01

class Example001Spec extends CommandSpecBase {
	describe("BSSE configuration") {
		implicit val p = makeProcessorBsse(
			Config01.database1Json,
			JsonParser("""{
				"plate": [
					{ "id": "Pwater", "model": "Trough 100ml" },
					{ "id": "Pdye", "model": "Trough 100ml" },
					{ "id": "P1", "model": "D-BSSE 96 Well PCR Plate" },
					{ "id": "P2", "model": "D-BSSE 96 Well PCR Plate" }
				],
				"plateState": [
					{ "id": "Pwater", "location": "trough1" },
					{ "id": "Pdye", "location": "trough2" },
					{ "id": "P1", "location": "cooled1hi" },
					{ "id": "P2", "location": "cooled2hi" }
				],
				"vesselState": [
					{ "id": "Pwater(A01)", "content": { "water": "10ml" }, "isSource": true },
					{ "id": "Pwater(B01)", "content": { "water": "10ml" }, "isSource": true },
					{ "id": "Pwater(C01)", "content": { "water": "10ml" }, "isSource": true },
					{ "id": "Pwater(D01)", "content": { "water": "10ml" }, "isSource": true }
				],
				"cmd": [
				  { "cmd": "pipette.transfer", "source": ["water"], "destination": ["P1(A01dD01)"], "amount": ["50ul"], "pipettePolicy": "Water free dispense" }
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
			val vss_P1_A01_1 = getState[VesselSituatedState]("P1(A01)", List(1))
			val vss_P1_B01_1 = getState[VesselSituatedState]("P1(B01)", List(1))
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