package roboliq.commands.pipette

import spray.json.JsonParser
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._
import roboliq.commands.pipette._
import roboliq.commands.CommandSpecBase
import roboliq.test.Config01

class TransferCmdSpec extends CommandSpecBase {
	describe("pipette.transfer") {
		describe("BSSE configuration") {
			describe("single item") {
				implicit val p = makeProcessorBsse(
					Config01.database1Json,
					Config01.protocol1Json,
					JsonParser("""{
						"cmd": [
						  { "cmd": "pipette.transfer", "source": ["P1(A01)"], "destination": ["P1(B01)"], "amount": ["50ul"], "pipettePolicy": "Water free dispense" }
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

			describe("two items, source wells adjacent and dest wells adjacent") {
				implicit val p = makeProcessorBsse(
					Config01.database1Json,
					Config01.protocol1Json,
					JsonParser("""{
						"vesselState": [
							{ "id": "P1(A01)", "content": { "water": "100ul" } },
							{ "id": "P1(B01)", "content": { "water": "100ul" } }
						],
						"cmd": [
						  { "cmd": "pipette.transfer", "source": ["P1(A01)", "P1(B01)"], "destination": ["P1(C01)", "P1(D01)"], "amount": ["50ul", "50ul"], "pipettePolicy": "Water free dispense" }
						]
						}""").asJsObject
				)
				val policy = getObj[PipettePolicy]("Water free dispense")
				
				it("should have no errors or warnings") {
					assert(p.getMessages === Nil)
				}
				
				it("should generate correct tokens") {
					val (time_l, token_l) = p.getTokenList.unzip
					val tipState_TIP1_A = getState[TipState]("TIP1", time_l(0))
					val tipState_TIP2_A = getState[TipState]("TIP2", time_l(0))
					val tipState_TIP1_B = getState[TipState]("TIP1", time_l(1))
					val tipState_TIP2_B = getState[TipState]("TIP2", time_l(1))
					val tipState_TIP1_C = getState[TipState]("TIP1", time_l(2))
					val tipState_TIP2_C = getState[TipState]("TIP2", time_l(2))
					val vss_P1_A01_B = getState[VesselSituatedState]("P1(A01)", time_l(1))
					val vss_P1_B01_B = getState[VesselSituatedState]("P1(B01)", time_l(1))
					val vss_P1_C01_C = getState[VesselSituatedState]("P1(C01)", time_l(2))
					val vss_P1_D01_C = getState[VesselSituatedState]("P1(D01)", time_l(2))
					assert(token_l === List(
						low.WashTipsToken("Thorough", List(tipState_TIP1_A, tipState_TIP2_A)),
						low.AspirateToken(List(
							TipWellVolumePolicy(tipState_TIP1_B, vss_P1_A01_B, LiquidVolume.ul(50), policy),
							TipWellVolumePolicy(tipState_TIP2_B, vss_P1_B01_B, LiquidVolume.ul(50), policy)
						)),
						low.DispenseToken(List(
							TipWellVolumePolicy(tipState_TIP1_C, vss_P1_C01_C, LiquidVolume.ul(50), policy),
							TipWellVolumePolicy(tipState_TIP2_C, vss_P1_D01_C, LiquidVolume.ul(50), policy)
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

			describe("two items, source wells adjacent but dest wells not adjacent") {
				implicit val p = makeProcessorBsse(
					Config01.database1Json,
					Config01.protocol1Json,
					JsonParser("""{
						"vesselState": [
							{ "id": "P1(A01)", "content": { "water": "100ul" } },
							{ "id": "P1(B01)", "content": { "water": "100ul" } }
						],
						"cmd": [
						  { "cmd": "pipette.transfer", "source": ["P1(A01)", "P1(B01)"], "destination": ["P1(C01)", "P1(C02)"], "amount": ["50ul", "50ul"], "pipettePolicy": "Water free dispense" }
						]
						}""").asJsObject
				)
				val policy = getObj[PipettePolicy]("Water free dispense")
				
				it("should have no errors or warnings") {
					assert(p.getMessages === Nil)
				}
				
				it("should generate correct tokens") {
					val (time_l, token_l) = p.getTokenList.unzip
					val tipState_TIP1_A = getState[TipState]("TIP1", time_l(0))
					val tipState_TIP2_A = getState[TipState]("TIP2", time_l(0))
					val tipState_TIP1_B = getState[TipState]("TIP1", time_l(1))
					val tipState_TIP2_B = getState[TipState]("TIP2", time_l(1))
					val tipState_TIP1_C = getState[TipState]("TIP1", time_l(2))
					val tipState_TIP2_D = getState[TipState]("TIP2", time_l(3))
					val vss_P1_A01_B = getState[VesselSituatedState]("P1(A01)", time_l(1))
					val vss_P1_B01_B = getState[VesselSituatedState]("P1(B01)", time_l(1))
					val vss_P1_C01_C = getState[VesselSituatedState]("P1(C01)", time_l(2))
					val vss_P1_C02_D = getState[VesselSituatedState]("P1(C02)", time_l(3))
					assert(token_l === List(
						low.WashTipsToken("Thorough", List(tipState_TIP1_A, tipState_TIP2_A)),
						low.AspirateToken(List(
							TipWellVolumePolicy(tipState_TIP1_B, vss_P1_A01_B, LiquidVolume.ul(50), policy),
							TipWellVolumePolicy(tipState_TIP2_B, vss_P1_B01_B, LiquidVolume.ul(50), policy)
						)),
						low.DispenseToken(List(
							TipWellVolumePolicy(tipState_TIP1_C, vss_P1_C01_C, LiquidVolume.ul(50), policy)
						)),
						low.DispenseToken(List(
							TipWellVolumePolicy(tipState_TIP2_D, vss_P1_C02_D, LiquidVolume.ul(50), policy)
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
}