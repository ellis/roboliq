package roboliq.input

import org.scalatest.FunSpec
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsNull
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe.TypeTag
import org.scalatest.exceptions.TestFailedException
import spray.json.JsString

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
		}
	}
}