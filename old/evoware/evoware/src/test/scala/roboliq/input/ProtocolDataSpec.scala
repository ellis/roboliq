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
			val object1a = ProtocolDataVariable(
				name = "prop1",
				description_? = Some("description"),
				value_? = Some(RjsNumber(1))
			)
			val object1b = ProtocolDataVariable(
				name = "prop1",
				value_? = Some(RjsNumber(2))
			)
			val object1 = ProtocolDataVariable(
				name = "prop1",
				description_? = Some("description"),
				value_? = Some(RjsNumber(2))
			)
			val object2 = ProtocolDataVariable("prop2")
			val object3 = ProtocolDataVariable("prop3")
			val protocolDataA = ProtocolData(
				variables = Map(
					"object1" -> object1a,
					"object2" -> object2
				)
			)
			val protocolDataB = ProtocolData(
				variables = Map(
					"object1" -> object1b,
					"object3" -> object3
				)
			)
			
			val result_? = protocolDataA.merge(protocolDataB)
			
			val expected = ProtocolData(
				variables = Map(
					"object1" -> object1,
					"object2" -> object2,
					"object3" -> object3
				)
			)
			
			assert(result_?.run().value == expected)
			//ProtocolData(Map(object1 -> ProtocolDataVariable(prop1,None,None,None,List()), object2 -> ProtocolDataVariable(prop2,None,None,None,List()), object3 -> ProtocolDataVariable(prop3,None,None,None,List())),RjsBasicMap(),Map(),Map(),Literals(),None)
			//ProtocolData(Map(object1 -> ProtocolDataVariable(prop1,Some(description),None,Some(RjsNumber(2,None)),List()), object2 -> ProtocolDataVariable(prop2,None,None,None,List()), object3 -> ProtocolDataVariable(prop3,None,None,None,List())),RjsBasicMap(),Map(),Map(),Literals(),None) (ProtocolDataSpec.scala:55)
		}

		/*it("merge objects") {
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
		}*/

		it("merge commands") {
			val action1 = new ProtocolDataStep(
				params = Map(
					"command" -> RjsString("shakePlate"),
					"labware" -> RjsString("plate1"),
					"site" -> RjsString("P3"),
					"program" -> RjsBasicMap(
						"rpm" -> RjsNumber(200),
						"duration" -> RjsNumber(10)
					)
				),
				children = Map()
			)
			val action2 = new ProtocolDataStep(
				params = Map(
					"command" -> RjsString("shakePlate"),
					"agent" -> RjsString("mario"),
					"device" -> RjsString("mario__Shaker"),
					"labware" -> RjsString("plate1"),
					"site" -> RjsString("P3"),
					"program" -> RjsBasicMap(
						"rpm" -> RjsNumber(200),
						"duration" -> RjsNumber(10)
					)
				),
				children = Map()
			)
			val protocolDataA = ProtocolData(
				steps = Map(
					"1" -> action1,
					"2" -> action1
				)
			)
			val protocolDataB = ProtocolData(
				steps = Map(
					"1" -> action2,
					"3" -> action2
				)
			)
			
			val result_? = protocolDataA.merge(protocolDataB)
			
			val expected = ProtocolData(
				steps = Map(
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