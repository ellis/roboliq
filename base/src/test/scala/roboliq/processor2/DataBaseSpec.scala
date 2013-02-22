package roboliq.processor2

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe.TypeTag
import grizzled.slf4j.Logger
import org.scalatest.FunSpec
import spray.json._
import _root_.roboliq.core._


class DataBaseSpec extends FunSpec {
	describe("DataBase") {
		it("should read back equivalent JsValues as set, time=Nil") {
			val db = new DataBase
			val l = List[(TKP, JsValue)](
				TKP("a", "1", Nil) -> JsString("Hello, World!"),
				TKP("a", "2", Nil) -> JsObject("n" -> JsNumber(42), "l" -> JsArray(JsString("A"), JsString("B"))),
				TKP("a", "3", Nil) -> JsObject(
					"id" -> JsString("3"),
					"l" -> JsArray(JsString("A"), JsString("B")),
					"o" -> JsObject("n" -> JsNumber(42), "l" -> JsArray(JsString("A"), JsString("B")))
				)
			)
			for ((tkp, jsval) <- l) {
				db.set(tkp, Nil, jsval)
				val jsval2_? = db.get(tkp)
				assert(jsval2_? === RqSuccess(jsval))
			}
		}
	}
}