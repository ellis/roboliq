package roboliq.device.transport

import org.scalatest.FunSpec
import org.scalatest.GivenWhenThen
import spray.json._
import roboliq.core._
import roboliq.entity._
import roboliq.processor._
import roboliq.events._
import roboliq.test.Config01
import roboliq.processor.ProcessorData
import roboliq.commands.CommandSpecBase


// REFACTOR: move arm.movePlate command to MovePlateSpec
// REFACTOR: should probably derive this class from CommandSpecBase
class MovePlateSpec extends CommandSpecBase {
	describe("A Processor") {
		describe("should handle arm.movePlate") {
			implicit val p = makeProcessorBsse(
				Config01.protocol1Json,
				JsonParser("""{
					"cmd": [
						{ "cmd": "transport.movePlate", "plate": "P_1", "destination": "cool2PCR", "deviceId": "ROMA2" }
					]
					}""").asJsObject
			)
			
			it("should have no errors or warnings") {
				assert(p.getMessages === Nil)
			}
			
			it("should generate correct tokens") {
				val (_, token_l) = p.getTokenList.unzip
				assert(token_l === List(
					MovePlateToken(
						Some("ROMA2"),
						Config01.plate_P1,
						Config01.plateLocation_cooled1,
						Config01.plateLocation_cooled2
					)
				))
			}
			
			it("should place plate at correct final location") {
				val plateState_P1_2 = getState[PlateState]("P_1", List(2))
				assert(plateState_P1_2.location_?.map(_.id) === Some("cool2PCR"))
			}
		}
	}
}
