package roboliq.commands.pipette.low

import spray.json.JsonParser
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._
import roboliq.commands.pipette._
import roboliq.commands.CommandSpecBase
import roboliq.test.Config01

class AspirateCmdSpec extends CommandSpecBase {
	describe("pipette.low.aspirate") {
		describe("BSSE configuration") {
			implicit val p = makeProcessorBsse(
				Config01.database1Json,
				Config01.protocol1Json,
				JsonParser("""{
					"cmd": [
					  { "cmd": "pipette.low.aspirate", "items": [{"tip": "TIP1", "well": "P1(A01)", "volume": "50ul", "policy": { "id": "Wet", "pos": "WetContact" }}] }
					]
					}""").asJsObject
			)
			
			it("should have no errors or warnings") {
				assert(p.getMessages === Nil)
			}
			
			it("should generate correct tokens") {
				val (_, token_l) = p.getTokenList.unzip
				val tipState_1 = getState[TipState]("TIP1", List(1))
				val vss_P1_A01_1 = getState[VesselSituatedState]("P1(A01)", List(1))
				assert(token_l === List(
					AspirateToken(List(new TipWellVolumePolicy(tipState_1, vss_P1_A01_1, LiquidVolume.ul(50), PipettePolicy("Wet", PipettePosition.WetContact))))
				))
			}
			
			it("should have correct TipStates") {
				val tipState_1 = getState[TipState]("TIP1", List(1))
				val tipState_1_expected = TipState.createEmpty(Config01.tip1)
				assert(tipState_1 === tipState_1_expected)

				val tipState_2 = getState[TipState]("TIP1", List(2))
				val tipState_2_content_expected = checkObj(VesselContent.fromVolume(Config01.water, LiquidVolume.ul(50)))
				assert(tipState_2.content === tipState_2_content_expected)
			}

			it("should have correct VesselState for source well") {
				val vesselState_P1_A01_2 = getState[VesselState]("P1(A01)", List(2))
				val vesselContent_P1_A01_content_expected = checkObj(VesselContent.fromVolume(Config01.water, LiquidVolume.ul(50)))
				assert(vesselState_P1_A01_2.content === vesselContent_P1_A01_content_expected)
			}
		}
	}
}