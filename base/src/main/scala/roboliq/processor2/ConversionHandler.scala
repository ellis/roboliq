package roboliq.processor2

import scalaz._
import spray.json._
import roboliq.core._


abstract class ConversionHandler {
	import InputListToTuple._
	
	//val cmd_l: List[String]
	def getResult(jsval: JsValue): ConversionResult

	protected case class RequireItem[A: Manifest](id: String) {
		val clazz = implicitly[Manifest[A]].runtimeClass
	}
	
	protected def handlerRequire[A: Manifest](a: RequireItem[A])(fn: (A) => ConversionResult): ConversionResult = {
		RqSuccess(
			List(
				ConversionItem_Conversion(List(IdClass(a.id, a.clazz)),
					(j_l) => check1(j_l).flatMap { a => fn(a) }
				)
			)
		)
	}
	
	protected def as[A: Manifest](id: String): RequireItem[A] = RequireItem[A](id)
	protected def as[A: Manifest](symbol: Symbol): RequireItem[A] = as[A]("$"+symbol.name)
}