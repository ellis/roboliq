package roboliq.processor2

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.Type
import spray.json.JsValue

//sealed trait RqParam
//case class RqParam_Entity(kco: KeyClassOpt) extends RqParam
//case class RqParam_Deref extends RqParam

case class KeyClass(key: TKP, clazz: Type, time: List[Int] = Nil) {
	def changeKey(key: String): KeyClass =
		this.copy(key = this.key.copy(key = key))
	def isJsValue: Boolean =
		(clazz =:= ru.typeOf[JsValue])
	def changeClassToJsValue: KeyClass =
		this.copy(clazz = ru.typeOf[JsValue])
	def changeTime(time: List[Int]): KeyClass =
		this.copy(time = time)
	val id: String = {
		key.id + (if (!isJsValue) "<"+getSimplifiedClassName+">" else "") + (if (time.isEmpty) "" else time.mkString("@(", "/", ")"))
	}
	def getSimplifiedClassName: String = {
		val s0 = clazz.typeSymbol.name.toString
		val iPeriod = s0.lastIndexOf('.')
		if (iPeriod >= 0) s0.substring(iPeriod + 1)
		else s0
	}
	
	override def toString = id
}

case class KeyClassOpt(
	kc: KeyClass,
	opt: Boolean = false,
	conversion_? : Option[RqFunctionArgs] = None
) {
	def changeKey(key: String): KeyClassOpt =
		this.copy(kc = kc.changeKey(key))
	def changeClassToJsValue: KeyClassOpt =
		this.copy(kc = kc.changeClassToJsValue)
	def changeTime(time: List[Int]): KeyClassOpt =
		this.copy(kc = kc.changeTime(time))
}
