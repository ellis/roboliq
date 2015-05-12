package roboliq.input

import org.scalatest.FunSpec
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsNull
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe.TypeTag
import org.scalatest.exceptions.TestFailedException
import spray.json.JsString

case class RjsValueSpecCaseClass1(
	a: String,
	b: Int
)

class RjsValueSpec extends FunSpec {
	import ResultCWrapper._
	import ResultEWrapper._
	
	val number = RjsNumber(12); val jsNumber = JsNumber(12)
	val string = RjsString("john"); val jsString = JsString("john")
	val text = RjsText("john"); val jsText = JsString("\"john\"")
	val format = RjsFormat("${john}"); val jsFormat = JsString("f\"${john}\"")
	val subst = RjsSubst("john"); val jsSubst = JsString("$john")
	val map = RjsBasicMap("a" -> RjsNumber(1), "b" -> RjsString("hi")); val jsMap = JsObject("a" -> JsNumber(1), "b" -> JsString("hi"))

	val action = RjsAction("someaction", RjsMap("a" -> RjsNumber(1)))
	val jsAction = JsObject("TYPE" -> JsString("action"), "NAME" -> JsString("someaction"), "INPUT" -> JsObject("a" -> JsNumber(1)))
	
	describe("RjsValue") {
		it("RjsValue.toJson for non-Rjs types") {
			assert(RjsValue.toJson("john", typeOf[String]).run().value == JsString("john"))
		}
		it("RjsValue.fromJson") {
			assert(RjsValue.fromJson(jsNumber).run().value == number)
			assert(RjsValue.fromJson(jsString).run().value == string)
			assert(RjsValue.fromJson(jsText).run().value == text)
			assert(RjsValue.fromJson(jsFormat).run().value == format)
			assert(RjsValue.fromJson(jsSubst).run().value == subst)
			assert(RjsValue.fromJson(jsMap).run().value == map)
			assert((for {
				rjsval0 <- ResultE.from(RjsValue.fromJson(jsAction))
				tm = rjsval0.asInstanceOf[RjsBasicMap]
				rjsval1 <- RjsValue.evaluateTypedMap(tm)
			} yield rjsval1).run().value == action)
		}
		it("RjsValue.fromObject") {
			val obj1 = RjsValueSpecCaseClass1("hello", 1)
			val obj2 = RjsValueSpecCaseClass1("bye", 2)
			val map1 = Map[String, RjsValueSpecCaseClass1]("obj1" -> obj1, "obj2" -> obj2)
			
			val rjsval1 = RjsBasicMap("a" -> RjsString("hello"), "b" -> RjsNumber(1))
			val rjsval2 = RjsBasicMap("a" -> RjsString("bye"), "b" -> RjsNumber(2))
			val rjsval3 = RjsBasicMap("obj1" -> rjsval1, "obj2" -> rjsval2)
			
			assert(RjsValue.fromObject(obj1).run().value == rjsval1)
			assert(RjsValue.fromObject(obj2).run().value == rjsval2)
			assert(RjsValue.fromObject(map1).run().value == rjsval3)
		}
		it("rjsvalue.toJson") {
			assert(number.toJson.run().value == jsNumber)
			assert(string.toJson.run().value == jsString)
			assert(text.toJson.run().value == jsText)
			assert(format.toJson.run().value == jsFormat)
			assert(subst.toJson.run().value == jsSubst)
			assert(action.toJson.run().value == jsAction)
		}
		it("RjsValue.evaluateTypedMap") {
			val tmLambda = RjsBasicMap("lambda", Map(
				"EXPRESSION" -> RjsBasicMap("call", Map(
					"NAME" -> RjsString("add"),
					"INPUT" -> RjsBasicMap(Map("numbers" -> RjsList(List(RjsSubst("x"), RjsNumber(1,None)))))
				))
			))
			val lambda = RjsLambda(Nil, RjsBasicMap("call", Map(
				"NAME" -> RjsString("add"),
				"INPUT" -> RjsBasicMap(Map("numbers" -> RjsList(List(RjsSubst("x"), RjsNumber(1,None)))))
			)))
			assert(RjsValue.evaluateTypedMap(tmLambda).run().value == lambda)

			val tmCustom1Basic = RjsBasicMap("CustomType1", Map(
				"a" -> RjsString("Hello"),
				"b" -> RjsNumber(42)
			))
			assert(RjsValue.evaluateTypedMap(tmCustom1Basic).run().value == tmCustom1Basic)

			val tmCustom2Basic = RjsBasicMap("CustomType2", Map(
				"myLambda" -> tmLambda
			))
			val tmCustom2 = RjsMap("CustomType2", Map(
				"myLambda" -> lambda
			))
			assert(RjsValue.evaluateTypedMap(tmCustom2Basic).run().value == tmCustom2)
		}
		it("RjsValue.merge") {
			val l1 = RjsList(number)
			val l2 = RjsList(string)
			assert(RjsValue.merge(l1, l2).run().value == RjsList(number, string))
			
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
			val protocolDataA = RjsBasicMap(
				"object1" -> object1a,
				"object2" -> object2
			)
			val protocolDataB = RjsBasicMap(
				"object1" -> object1b,
				"object3" -> object3
			)
			
			val result_? = protocolDataA.merge(protocolDataB)
			
			val expected = RjsBasicMap(
				"object1" -> object1,
				"object2" -> object2,
				"object3" -> object3
			)
			
			assert(result_?.run().value == expected)
			
		}
		it("RjsValue.toBasicValue") {
			assert(RjsValue.toBasicValue(1).run().value == RjsNumber(1))
			assert(RjsValue.toBasicValue(Map[String, String]("a" -> "1", "b" -> "2")).run().value == RjsBasicMap("a" -> RjsString("1"), "b" -> RjsString("2")))
			assert(RjsValue.toBasicValue(RjsNumber(1)).run().value == RjsNumber(1))
			assert(RjsValue.toBasicValue(RjsNumber(1)).run().value == RjsNumber(1))
			assert(RjsValue.toBasicValue(RjsValueSpecCaseClass1("one", 2)).run().value == RjsBasicMap("a" -> RjsString("one"), "b" -> RjsNumber(2)))
		}
	}
}