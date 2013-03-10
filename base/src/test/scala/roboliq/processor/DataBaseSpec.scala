package roboliq.processor

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
				db.set(tkp, jsval)
				val jsval2_? = db.get(tkp)
				assert(jsval2_? === RqSuccess(jsval))
			}
		}
		
		it("should read back equivalent JsValues as those set with time != Nil") {
			val jsval0 = JsObject("s" -> JsString("_"), "n" -> JsNumber(0))
			val jsval1 = JsObject("s" -> JsString("a"), "n" -> JsNumber(1))
			val jsval2 = JsObject("s" -> JsString("b"), "n" -> JsNumber(2))
			val tkp = TKP("TABLE", "KEY", Nil)

			val db = new DataBase

			// Database is empty, so element shouldn't be found.
			assert(db.get(tkp).isError)
			
			// Set object at time 0
			db.setAt(tkp, List(0), jsval0)
			// Object should now be found at time 0
			assert(db.getAt(tkp, List(0)) === RqSuccess(jsval0))

			// Update object at time 1
			db.setAt(tkp, List(1), jsval1)
			// Updated object should be found at time 1
			assert(db.getAt(tkp, List(1)) === RqSuccess(jsval1))
			
			// Update object again at time 2
			db.setAt(tkp, List(2), jsval2)
			// Updated object should be found at time 2
			assert(db.getAt(tkp, List(2)) === RqSuccess(jsval2))

			// No object set at time=Nil, so shouldn't find one
			assert(db.get(tkp).isError)
			// Should still find original object at time 0
			assert(db.getAt(tkp, List(0)) === RqSuccess(jsval0))
			// Should also find original object at time 0.1
			assert(db.getAt(tkp, List(0, 1)) === RqSuccess(jsval0))
			// Should still jsval1 at time 1
			assert(db.getAt(tkp, List(1)) === RqSuccess(jsval1))
			// Should find jsval2 at time 3
			assert(db.getAt(tkp, List(3)) === RqSuccess(jsval2))
		}
		
		it("should handle state changes when adding a new field") {
			val tkp = TKP("vesselState", "P1(A01)", Nil)
			val time11 = List(1, 1)
			val time1122 = List(1, 1, 2, 2)
			val time1122_+ = List(1, 1, 2, 2, Int.MaxValue)
			val jsval1 = JsonParser("""{"id":"P1(A01)","content":{"water":"100ul"}}""")
			val jsval2 = JsonParser("""{"id":"P1(A01)","content":{"water":0.00275},"isInitialVolumeKnown":null}""")
			
			val db = new DataBase
			db.setAt(tkp, List(0), jsval1)
			assert(db.getBefore(tkp, time11) === RqSuccess(jsval1))
			assert(db.getBefore(tkp, time1122) === RqSuccess(jsval1))

			info(db.toString)
			db.setAt(tkp, time1122_+, jsval2)
			info(db.toString)
			assert(db.getAt(tkp, time1122_+) === RqSuccess(jsval2))
			assert(db.getAt(tkp, List(0)) === RqSuccess(jsval1))
			assert(db.getBefore(tkp, time1122_+) === RqSuccess(jsval1))
			assert(db.getBefore(tkp, time1122) === RqSuccess(jsval1))
		}

		it("should read back all entities in table with getAll()") {
			val jsvalA = JsObject("s" -> JsString("a"), "n" -> JsNumber(1))
			val jsvalB = JsObject("s" -> JsString("b"), "n" -> JsNumber(2))
			val jsvalC = JsObject("s" -> JsString("c"), "n" -> JsNumber(3))

			val db = new DataBase
			
			db.set(TKP("TABLE", "A", Nil), jsvalA)
			db.set(TKP("TABLE", "B", Nil), jsvalB)
			db.set(TKP("TABLE", "C", Nil), jsvalC)

			// Object should now be found at time 0
			assert(db.getAll("TABLE").toSet === Set(jsvalA, jsvalB, jsvalC))
		}
	}
}