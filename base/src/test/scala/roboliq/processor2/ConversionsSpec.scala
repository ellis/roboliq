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
		/*it("help") {
			val mirror = ru.runtimeMirror(A.getClass.getClassLoader)
			val typ = typeOf[A.Value]
			// Get enclosing enumeration (e.g. MyStatus.Value => MyStatus)
			val enumType = typ.find(_ <:< typeOf[Enumeration]).get
			val enumModule = enumType.termSymbol.asModule
			val enumMirror = mirror.reflectModule(enumModule)
			val enum1 = enumMirror.instance.asInstanceOf[Enumeration]
			print(enum1.values)

			val enumClass = enumType.typeSymbol.asClass
			val clazz = mirror.runtimeClass(enumClass)
			println(clazz)
			val ctor = clazz.getConstructor()
			println(ctor)
			val enum0 = ctor.newInstance()
			
			val enumCtorMethod = enumType.member(ru.nme.CONSTRUCTOR).asMethod
			val enumCtorMirror = mirror.reflectClass(enumClass).reflectConstructor(enumCtorMethod)
			val enum = enumCtorMirror().asInstanceOf[Enumeration]
			val value_l = enum.values
			val s = "B"
			println(value_l.find(_.toString == s).map(_.asInstanceOf[A.Value]))
		}
	*/
		it("should parse Enum") {
			val exp = 42
			val jsval = JsNumber(exp)
			assert(toEnum[A.Value](JsString("B")) === RqSuccess(A.B))
			assert(conv(JsString("B"), typeOf[A.Value]) === RqSuccess(A.B))
		}
	}
}