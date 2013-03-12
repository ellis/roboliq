package roboliq.commands.pipette

import spray.json.JsonParser
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._
import roboliq.commands.pipette._
import roboliq.commands.CommandSpecBase
import roboliq.test.Config01

class TransferCmdSpec extends CommandSpecBase {
	describe("pipette.transfer") {
		describe("BSSE configuration") {
			implicit val p = makeProcessorBsse(
				Config01.protocol1Json,
				//{ "cmd": "pipette.tips", "cleanIntensity": "Thorough", "items": [{"tip": "TIP1"}] }
				JsonParser("""{
					"cmd": [
					  { "cmd": "pipette.transfer", "source": ["P1(A01)"], "destination": ["P1(B01)"], "amount": ["50ul"], "pipettePolicy": "POLICY" }
					]
					}""").asJsObject
			)
			
			it("should have no errors or warnings") {
				assert(p.getMessages === Nil)
			}
			
			it("should generate correct tokens") {
				val (time_l, token_l) = p.getTokenList.unzip
				val tipState_A = getState[TipState]("TIP1", time_l(0))
				val tipState_B = getState[TipState]("TIP1", time_l(1))
				val vss_P1_A01_1 = getState[VesselSituatedState]("P1(A01)", List(1))
				val vss_P1_B01_1 = getState[VesselSituatedState]("P1(B01)", List(1))
				assert(token_l === List(
					low.AspirateToken(List(
						TipWellVolumePolicy(tipState_A, vss_P1_A01_1, LiquidVolume.ul(50), PipettePolicy("POLICY", PipettePosition.WetContact))
					)),
					low.DispenseToken(List(
						TipWellVolumePolicy(tipState_B, vss_P1_B01_1, LiquidVolume.ul(50), PipettePolicy("POLICY", PipettePosition.WetContact))
					))
				))
			}
			
			ignore("should have correct TipStates") {
				val tipState_1 = getState[TipState]("TIP1", List(1))
				val tipState_1_expected = TipState.createEmpty(Config01.tip1)
				assert(tipState_1 === tipState_1_expected)

				val tipState_2 = getState[TipState]("TIP1", List(2))
				val tipState_2_expected = tipState_1.copy(
					cleanDegree = CleanIntensity.Thorough,
					cleanDegreePrev = CleanIntensity.Thorough,
					cleanDegreePending = CleanIntensity.None
				)
				assert(tipState_2 === tipState_2_expected)
			}
		}
	}
}