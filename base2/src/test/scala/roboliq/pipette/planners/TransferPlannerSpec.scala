package roboliq.pipette.planners

import scala.collection.immutable.SortedSet
import roboliq.core._
import roboliq.entities._
import org.scalatest.FunSpec


class TransferPlannerSpec extends FunSpec {
	describe("TransferPlanner") {
		describe("BSSE configuration") {
			import TransferPlanner.{Item,BatchItem,Batch}
			/*
			implicit val p = makeProcessorBsse(
				Config01.database1Json,
				Config01.protocol1Json,
				JsonParser("""{
					"vessel": [
						{ "id": "P_1(A01)" },
						{ "id": "P_1(B01)" },
						{ "id": "P_1(C01)" },
						{ "id": "P_1(D01)" }
					],
					"vesselState": [
						{ "id": "P_1(A01)", "content": { "water": "100ul" } },
						{ "id": "P_1(B01)", "content": { "water": "100ul" } },
						{ "id": "P_1(C01)", "content": { "water": "100ul" } },
						{ "id": "P_1(D01)", "content": { "water": "100ul" } }
					],
					"vesselSituatedState": [
						{ "id": "P_1(A01)", "position": { "plate": "P_1", "index": 0 } },
						{ "id": "P_1(B01)", "position": { "plate": "P_1", "index": 1 } },
						{ "id": "P_1(C01)", "position": { "plate": "P_1", "index": 2 } },
						{ "id": "P_1(D01)", "position": { "plate": "P_1", "index": 3 } }
					]
					}""").asJsObject
			)
			*/
			
			val device = new roboliq.test.TestPipetteDevice1
			val tip_l = SortedSet(Config01.tip1, Config01.tip2, Config01.tip3, Config01.tip4)
			
			val state0 = new WorldStateBuilder
			val site_cooled1 = Site("cooled1")
			val p_P1 = Plate("P_1")
			state0.labware_location_m(p_P1) = site_cooled1

			val v_P1_A01 = Well("P_1(A01)")
			state0.addWell(v_P1_A01, p_P1, RowCol(0, 0), 0)
			state0.well_aliquot_m(v_P1_A01) = Aliquot(Mixture(Left(Config01.water)), Distribution.fromVolume(LiquidVolume.ul(100)))

			val v_P1_B01 = Well("P_1(B01)")
			state0.addWell(v_P1_B01, p_P1, RowCol(1, 0), 0)
			state0.well_aliquot_m(v_P1_B01) = Aliquot(Mixture(Left(Config01.water)), Distribution.fromVolume(LiquidVolume.ul(100)))
			
			it("should work for 1 item") {
				val item_l = List(
					TransferPlanner.Item(List(v_P1_A01), v_P1_B01, LiquidVolume.ul(50))
				)
				
				val x = TransferPlanner.searchGraph(
					device,
					state0.toImmutable,
					tip_l,
					Config01.tipModel1000,
					PipettePolicy("POLICY", PipettePosition.Free),
					item_l
				)
			
				assert(x === RsSuccess(List(
					Batch(List(
						BatchItem(Config01.tip1, v_P1_A01, v_P1_B01, LiquidVolume.ul(50))
					))
				)))
			}

			/*
			it("should work for 2 neighboring items") {
				val vss_P1_A01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P_1(A01)", List(0)))
				val vss_P1_B01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P_1(B01)", List(0)))
				val vss_P1_C01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P_1(C01)", List(0)))
				val vss_P1_D01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P_1(D01)", List(0)))
				val item_l = List(
					TransferPlanner2.Item(List(vss_P1_A01), vss_P1_C01, LiquidVolume.ul(50)),
					TransferPlanner2.Item(List(vss_P1_B01), vss_P1_D01, LiquidVolume.ul(50))
				)
				
				val x = TransferPlanner2.searchGraph(
					device,
					tip_l,
					Config01.tipModel1000,
					PipettePolicy("POLICY", PipettePosition.Free),
					item_l
				)
			
				assert(x === RqSuccess(List(
					Batch(List(
						BatchItem(Config01.tip1, vss_P1_A01, vss_P1_C01, LiquidVolume.ul(50)),
						BatchItem(Config01.tip2, vss_P1_B01, vss_P1_D01, LiquidVolume.ul(50))
					))
				)))
			}

			it("should work for 2 non-neighboring items") {
				val vss_P1_A01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P_1(A01)", List(0)))
				val vss_P1_B01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P_1(B01)", List(0)))
				val vss_P1_C01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P_1(C01)", List(0)))
				val vss_P1_D01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P_1(D01)", List(0)))
				val item_l = List(
					TransferPlanner2.Item(List(vss_P1_A01), vss_P1_D01, LiquidVolume.ul(50)),
					TransferPlanner2.Item(List(vss_P1_B01), vss_P1_C01, LiquidVolume.ul(50))
				)
				
				val x = TransferPlanner2.searchGraph(
					device,
					tip_l,
					Config01.tipModel1000,
					PipettePolicy("POLICY", PipettePosition.Free),
					item_l
				)
			
			
				assert(x === RqSuccess(List(
					Batch(List(
						BatchItem(Config01.tip1, vss_P1_A01, vss_P1_D01, LiquidVolume.ul(50)),
						BatchItem(Config01.tip2, vss_P1_B01, vss_P1_C01, LiquidVolume.ul(50))
					))
				)))
			}
			*/
		}
	}
}