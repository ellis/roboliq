package roboliq.input

import org.scalatest.FunSpec
import roboliq.ai.strips

class ProtocolDataSpec extends FunSpec {
	import roboliq.input.ResultCWrapper._

	describe("ProtocolData") {
		//val objects: RjsBasicMap = RjsBasicMap(),
		//val commands: Map[String, RjsValue] = Map(),
		//val planningDomainObjects: Map[String, String] = Map(),
		//val planningInitialState: strips.Literals = strips.Literals.empty,
		//val processingState_? : Option[ProcessingState] = None
		it("merge variables") {
			val object1a = RjsBasicMap(
				"prop1" -> RjsString("A"),
				"prop2" -> RjsString("B"),
				"prop3" -> RjsString("C")
			)
			val object1b = RjsBasicMap(
				"prop2" -> RjsString("B"),
				"prop3" -> RjsString("*"),
				"prop4" -> RjsString("D")
			)
			val object1 = RjsBasicMap(
				"prop1" -> RjsString("A"),
				"prop2" -> RjsString("B"),
				"prop3" -> RjsString("*"),
				"prop4" -> RjsString("D")
			)
			val object2 = RjsBasicMap(
				"prop1" -> RjsString("A")
			)
			val object3 = RjsBasicMap(
				"prop2" -> RjsString("B")
			)
			val protocolDataA = ProtocolData(
				variables = RjsBasicMap(
					"object1" -> object1a,
					"object2" -> object2
				)
			)
			val protocolDataB = ProtocolData(
				variables = RjsBasicMap(
					"object1" -> object1b,
					"object3" -> object3
				)
			)
			
			val result_? = protocolDataA.merge(protocolDataB)
			
			val expected = ProtocolData(
				variables = RjsBasicMap(
					"object1" -> object1,
					"object2" -> object2,
					"object3" -> object3
				)
			)
			
			assert(result_?.run().value == expected)
		}

		it("merge objects") {
			val object1a = RjsBasicMap(
				"prop1" -> RjsString("A"),
				"prop2" -> RjsString("B"),
				"prop3" -> RjsString("C")
			)
			val object1b = RjsBasicMap(
				"prop2" -> RjsString("B"),
				"prop3" -> RjsString("*"),
				"prop4" -> RjsString("D")
			)
			val object1 = RjsBasicMap(
				"prop1" -> RjsString("A"),
				"prop2" -> RjsString("B"),
				"prop3" -> RjsString("*"),
				"prop4" -> RjsString("D")
			)
			val object2 = RjsBasicMap(
				"prop1" -> RjsString("A")
			)
			val object3 = RjsBasicMap(
				"prop2" -> RjsString("B")
			)
			val protocolDataA = ProtocolData(
				objects = RjsBasicMap(
					"object1" -> object1a,
					"object2" -> object2
				)
			)
			val protocolDataB = ProtocolData(
				objects = RjsBasicMap(
					"object1" -> object1b,
					"object3" -> object3
				)
			)
			
			val result_? = protocolDataA.merge(protocolDataB)
			
			val expected = ProtocolData(
				objects = RjsBasicMap(
					"object1" -> object1,
					"object2" -> object2,
					"object3" -> object3
				)
			)
			
			assert(result_?.run().value == expected)
		}

		it("merge commands") {
			val action1 = RjsAction("shakePlate", RjsMap(
				"labware" -> RjsString("plate1"),
				"site" -> RjsString("P3"),
				"program" -> RjsBasicMap(
					"rpm" -> RjsNumber(200),
					"duration" -> RjsNumber(10)
				)
			))
			val action2 = RjsAction("shakePlate", RjsMap(
				"agent" -> RjsString("mario"),
				"device" -> RjsString("mario__Shaker"),
				"labware" -> RjsString("plate1"),
				"site" -> RjsString("P3"),
				"program" -> RjsBasicMap(
					"rpm" -> RjsNumber(200),
					"duration" -> RjsNumber(10)
				)
			))
			val protocolDataA = ProtocolData(
				commands = Map(
					"1" -> action1,
					"2" -> action1
				)
			)
			val protocolDataB = ProtocolData(
				commands = Map(
					"1" -> action2,
					"3" -> action2
				)
			)
			
			val result_? = protocolDataA.merge(protocolDataB)
			
			val expected = ProtocolData(
				commands = Map(
					"1" -> action2,
					"2" -> action1,
					"3" -> action2
				)
			)
			
			assert(result_?.run().value == expected)
		}

		it("merge planningDomainObjects") {
			val protocolDataA = ProtocolData(
				planningDomainObjects = Map(
					"object1" -> "type1",
					"object2" -> "type2"
				)
			)
			val protocolDataB = ProtocolData(
				planningDomainObjects = Map(
					"object1" -> "type1b",
					"object3" -> "type3"
				)
			)
			
			val result_? = protocolDataA.merge(protocolDataB)
			
			val expected = ProtocolData(
				planningDomainObjects = Map(
					"object1" -> "type1b",
					"object2" -> "type2",
					"object3" -> "type3"
				)
			)
			
			assert(result_?.run().value == expected)
		}

		it("merge planningInitialState") {
			val protocolDataA = ProtocolData(
				planningInitialState = strips.Literals.fromStrings("a", "!b", "c")
			)
			val protocolDataB = ProtocolData(
				planningInitialState = strips.Literals.fromStrings("!a", "b", "d")
			)
			
			val result_? = protocolDataA.merge(protocolDataB)
			
			val expected = ProtocolData(
				planningInitialState = strips.Literals.fromStrings("!a", "b", "c", "d")
			)
			
			assert(result_?.run().value == expected)
		}
	}

}