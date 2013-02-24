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
		it("should read back equivalent JsValues as those set, with time=Nil") {
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
		
		it("should read back equivalent JsValues as those set with time != Nil") {
			val db = new DataBase
			val jsval0 = JsObject("s" -> JsString("_"), "n" -> JsNumber(0))
			val jsval1 = JsObject("s" -> JsString("a"), "n" -> JsNumber(1))
			val jsval2 = JsObject("s" -> JsString("b"), "n" -> JsNumber(2))
			val tkp = TKP("TABLE", "KEY", Nil)
			
			db.set(tkp, Nil, jsval0)
			assert(db.get(tkp) === RqSuccess(jsval0))
			
			db.set(tkp, List(1), jsval1)
			assert(db.getAt(tkp, List(1)) === RqSuccess(jsval1))
			
			db.set(tkp, List(2), jsval2)
			assert(db.getAt(tkp, List(2)) === RqSuccess(jsval2))

			assert(db.get(tkp) === jsval0)
			assert(db.getAt(tkp, Nil) === jsval0)
			assert(db.getAt(tkp, List(1)) === RqSuccess(jsval1))
		}
	}
}