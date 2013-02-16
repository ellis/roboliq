package roboliq.processor2

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe.TypeTag
import org.scalatest.FunSpec
import spray.json._
import roboliq.core._


object A extends Enumeration {
	val B, C = Value
}

class ConversionsSpec extends FunSpec {
	private def getTypeTag[T: TypeTag](obj: T) = ru.typeTag[T]
	private def getType[T: TypeTag](obj: T) = ru.typeTag[T].tpe
	
	describe("conv") {
		import ConversionsDirect._
		it("should parse String") {
			val exp = "test"
			val jsval = JsString(exp)
			assert(conv(jsval, getType(exp)) === RqSuccess(exp))
		}
		it("should parse Int") {
			val exp = 42
			val jsval = JsNumber(exp)
			assert(conv(jsval, getType(exp)) === RqSuccess(exp))
		}
		it("should parse Enum") {
			assert(conv(JsString("B"), typeOf[A.Value]) === RqSuccess(A.B))
			assert(conv(JsString("C"), typeOf[A.Value]) === RqSuccess(A.C))
			assert(conv(JsString(""), typeOf[A.Value]).isError)
			assert(conv(JsNumber(1), typeOf[A.Value]).isError)
			assert(conv(JsNull, typeOf[A.Value]).isError)
		}
	}
}