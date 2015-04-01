package roboliq.input

import org.scalatest.FunSpec

class ProtocolDataSpec extends FunSpec {
	import roboliq.input.ResultCWrapper._

	describe("ProtocolData") {
		it("merge()") {
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
	}

}