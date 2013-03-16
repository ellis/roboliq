package roboliq.commands.pipette.low

import spray.json.JsonParser
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._
import roboliq.commands.pipette._
import roboliq.commands.CommandSpecBase
import roboliq.test.Config01

class DispenseCmdSpec extends CommandSpecBase {
	describe("pipette.low.dispense") {
		describe("BSSE configuration") {
			implicit val p = makeProcessorBsse(
				Config01.database1Json,
				Config01.protocol1Json,
				JsonParser("""{
					"cmd": [
					  { "cmd": "pipette.low.aspirate", "items": [{"tip": "TIP1", "well": "P1(A01)", "volume": "50ul", "policy": { "id": "Wet", "pos": "WetContact" }}] },
					  { "cmd": "pipette.low.dispense", "items": [{"tip": "TIP1", "well": "P1(B01)", "volume": "50ul", "policy": { "id": "Wet", "pos": "WetContact" }}] }
					]
					}""").asJsObject
			)
			
			println("db:")
			println(p.db)
			
			it("should have no errors or warnings") {
				assert(p.getMessages === Nil)
			}
				
			it("should generated correct tokens") {
				val (_, token_l) = p.getTokenList.unzip
				val tipState_1 = getState[TipState]("TIP1", List(1))
				val tipState_2 = getState[TipState]("TIP1", List(2))
				val vss_P1_A01 = getState[VesselSituatedState]("P1(A01)", List(1))
				val vss_P1_B01 = getState[VesselSituatedState]("P1(B01)", List(2))
				assert(token_l === List(
					AspirateToken(List(new TipWellVolumePolicy(tipState_1, vss_P1_A01, LiquidVolume.ul(50), PipettePolicy("Wet", PipettePosition.WetContact)))),
					DispenseToken(List(new TipWellVolumePolicy(tipState_2, vss_P1_B01, LiquidVolume.ul(50), PipettePolicy("Wet", PipettePosition.WetContact))))
				))
			}
			
			it("should have correct final contents in the destination well") {
				assert(
					getState[VesselState]("P1(B01)", List(3)).content ===
					checkObj(VesselContent.fromVolume(Config01.water, LiquidVolume.ul(50)))
				)
			}
		}
	}
}