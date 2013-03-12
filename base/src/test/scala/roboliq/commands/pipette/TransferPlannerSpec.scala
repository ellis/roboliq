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
					"cmd": [
					  { "cmd": "pipette.transfer", "source": ["P1(A01)"], "destination": ["P1(B01)"], "amount": ["50ul"], "pipettePolicy": "POLICY" }
					]
					}""").asJsObject
			)

			it("should have no errors or warnings") {
				val vss_P1_A01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P1(A01)", List(0)))
				val vss_P1_B01 = checkObj(p.getObjFromDbAt[VesselSituatedState]("P1(B01)", List(0)))
				val item_l = List(
					TransferPlanner.Item(vss_P1_A01, vss_P1_B01, LiquidVolume.ul(50))
				)
				
				val device = new roboliq.test.TestPipetteDevice1
				val tip_l = List(Config01.tip1)
				val x = TransferPlanner.searchGraph(
					device,
					SortedSet(Config01.tip1),
					Config01.tipModel1000,
					PipettePolicy("POLICY", PipettePosition.Free),
					item_l
				)
			
				assert(x === RqSuccess(List(1)))
			}
		}
	}
}