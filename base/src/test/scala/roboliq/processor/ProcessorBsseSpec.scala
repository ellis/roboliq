package roboliq.processor

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe.TypeTag
import grizzled.slf4j.Logger
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.GivenWhenThen
import spray.json._
import _root_.roboliq.core._
import ConversionsDirect._
import _root_.roboliq.commands._
import _root_.roboliq._
import roboliq.commands.pipette.TipWellVolumePolicy
import roboliq.test.Config01


class ProcessorBsseSpec extends FunSpec with GivenWhenThen {
	private def makeProcessor(nameToJson_l: (String, JsObject)*): ProcessorData = {
		Given("a BSSE configuration (8 fixed tips, 4 large, 4 small)")
		val p = new ProcessorData(List(
			new arm.MovePlateHandler,
			new roboliq.commands.pipette.low.AspirateHandler,
			new roboliq.commands.pipette.low.DispenseHandler,
			new roboliq.commands.pipette.low.MixHandler
		))
		p.loadJsonData(Config01.benchJson)
		p.loadJsonData(Config01.protocol1Json)
		
		val (name_l, json_l) = nameToJson_l.unzip
		Given("custom configs: "+name_l.mkString(", "))
		json_l.foreach(p.loadJsonData)

		When("commands are run")
		val g = p.run()
		//org.apache.commons.io.FileUtils.writeStringToFile(new java.io.File("temp.dot"), g.toDot)
		p
	}
	
	def getObj[A <: Object : TypeTag](id: String)(implicit p: ProcessorData): A = {
		checkObj(p.getObjFromDbAt[A](id, Nil))
	}
	
	def getState[A <: Object : TypeTag](id: String, time: List[Int])(implicit p: ProcessorData): A = {
		checkObj(p.getObjFromDbBefore[A](id, time))
	}
	
	def checkObj[A <: Object : TypeTag](a_? : RqResult[A]): A = {
		a_? match {
			case RqSuccess(a, w) =>
				assert(w === Nil)
				a
			case RqError(e, w) =>
				info(w.toString)
				assert(e === Nil)
				null.asInstanceOf[A]
		}		
	}

	describe("A Processor") {
		describe("should handle arm.movePlate") {
			implicit val p = makeProcessor(
				"labware" -> JsonParser(
					"""{
					"plate": [
						{ "id": "P1", "model": "D-BSSE 96 Well PCR Plate" }
					]
					}""").asJsObject,
				"states" -> JsonParser(
					"""{	
					"plateState": [
						{ "id": "P1", "location": "cooled1" }
					]
					}""").asJsObject,
				"commands" -> JsonParser("""{
					"cmd": [
						{ "cmd": "arm.movePlate", "plate": "P1", "dest": "cooled2", "deviceId": "ROMA2" }
					]
					}""").asJsObject
			)
			
			//info(p.db.toString)
			
			it("should have no errors or warnings") {
				assert(p.getMessages === Nil)
			}
			
			it("should generate correct tokens") {
				val (_, token_l) = p.getTokenList.unzip
				assert(token_l === List(
					commands.arm.MovePlateToken(
						Some("ROMA2"),
						Config01.plate_P1,
						Config01.plateLocation_cooled1,
						Config01.plateLocation_cooled2
					)
				))
			}
			
			it("should place plate at correct final location") {
				val plateState_P1_2 = getState[PlateState]("P1", List(2))
				assert(plateState_P1_2.location_?.map(_.id) === Some("cooled2"))
			}
		}
	}
}
