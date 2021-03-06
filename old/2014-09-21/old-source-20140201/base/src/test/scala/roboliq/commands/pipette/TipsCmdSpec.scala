package roboliq.commands.pipette

import spray.json.JsonParser
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._
import roboliq.commands.pipette._
import roboliq.commands.CommandSpecBase
import roboliq.test.Config01

class TipCmdSpec extends CommandSpecBase {
	describe("pipette.tips") {
		describe("BSSE configuration") {
			implicit val p = makeProcessorBsse(
				Config01.database1Json,
				Config01.protocol1Json,
				JsonParser("""{
					"cmd": [
					  { "cmd": "pipette.tips", "cleanIntensity": "Thorough", "tips": ["TIP1"] }
					]
					}""").asJsObject
			)
			
			it("should have no errors or warnings") {
				assert(p.getMessages === Nil)
			}
			
			it("should generate correct tokens") {
				val (_, token_l) = p.getTokenList.unzip
				val tipState_1 = getState[TipState]("TIP1", List(1))
				val vss_P1_A01_1 = getState[VesselSituatedState]("P_1(A01)", List(1))
				assert(token_l === List(
					low.WashTipsToken("Thorough", List(tipState_1))
				))
			}
			
			it("should have correct TipStates") {
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