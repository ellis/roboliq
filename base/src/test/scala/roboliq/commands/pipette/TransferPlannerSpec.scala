package roboliq.commands.pipette

import scala.collection.immutable.SortedSet
import spray.json.JsonParser
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._
import roboliq.commands.pipette._
import roboliq.commands.CommandSpecBase
import roboliq.test.Config01

class TransferPlannerSpec extends CommandSpecBase {
	describe("TransferPlanner") {
		describe("BSSE configuration") {
			implicit val p = makeProcessorBsse(
				Config01.protocol1Json,
				//{ "cmd": "pipette.tips", "cleanIntensity": "Thorough", "items": [{"tip": "TIP1"}] }
				JsonParser("""{
					"vessel": [
						{ "id": "P1(A01)" },
						{ "id": "P1(B01)" },
						{ "id": "P1(C01)" },
						{ "id": "P1(D01)" }
					],
					"vesselState": [
						{ "id": "P1(A01)", "content": { "water": "100ul" } },
						{ "id": "P1(B01)", "content": { "water": "100ul" } },
						{ "id": "P1(C01)", "content": { "water": "100ul" } },
						{ "id": "P1(D01)", "content": { "water": "100ul" } }
					],
					"vesselSituatedState": [
						{ "id": "P1(A01)", "position": { "plate": "P1", "index": 0 } },
						{ "id": "P1(B01)", "position": { "plate": "P1", "index": 1 } },
						{ "id": "P1(C01)", "position": { "plate": "P1", "index": 2 } },
						{ "id": "P1(D01)", "position": { "plate": "P1", "index": 3 } }
					]
					}""").asJsObject
			)

			val device = new roboliq.test.TestPipetteDevice1
			val tip_l = SortedSet(Config01.tip1, Config01.tip2, Config01.tip3, Config01.tip4)

			it("should work for 1 item") {
				val vss_P1_A01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P1(A01)", List(0)))
				val vss_P1_B01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P1(B01)", List(0)))
				val item_l = List(
					TransferPlanner.Item(vss_P1_A01, vss_P1_B01, LiquidVolume.ul(50))
				)
				
				val x = TransferPlanner.searchGraph(
					device,
					tip_l,
					Config01.tipModel1000,
					PipettePolicy("POLICY", PipettePosition.Free),
					item_l
				)
			
				assert(x === RqSuccess(List(1)))
			}

			it("should work for 2 neighboring items") {
				val vss_P1_A01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P1(A01)", List(0)))
				val vss_P1_B01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P1(B01)", List(0)))
				val vss_P1_C01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P1(C01)", List(0)))
				val vss_P1_D01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P1(D01)", List(0)))
				val item_l = List(
					TransferPlanner.Item(vss_P1_A01, vss_P1_C01, LiquidVolume.ul(50)),
					TransferPlanner.Item(vss_P1_B01, vss_P1_D01, LiquidVolume.ul(50))
				)
				
				val x = TransferPlanner.searchGraph(
					device,
					tip_l,
					Config01.tipModel1000,
					PipettePolicy("POLICY", PipettePosition.Free),
					item_l
				)
			
				assert(x === RqSuccess(List(2)))
			}

			it("should work for 2 non-neighboring items") {
				val vss_P1_A01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P1(A01)", List(0)))
				val vss_P1_B01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P1(B01)", List(0)))
				val vss_P1_C01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P1(C01)", List(0)))
				val vss_P1_D01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P1(D01)", List(0)))
				val item_l = List(
					TransferPlanner.Item(vss_P1_A01, vss_P1_D01, LiquidVolume.ul(50)),
					TransferPlanner.Item(vss_P1_B01, vss_P1_C01, LiquidVolume.ul(50))
				)
				
				val x = TransferPlanner.searchGraph(
					device,
					tip_l,
					Config01.tipModel1000,
					PipettePolicy("POLICY", PipettePosition.Free),
					item_l
				)
			
				assert(x === RqSuccess(List(2)))
			}
		}
	}
}