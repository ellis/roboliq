package roboliq.input

import spray.json.JsValue
import roboliq.core.RsResult
import spray.json.JsObject
import roboliq.entities.EntityBase
import spray.json.JsNumber

/*
case class BuiltinAdd2Params(
	n1: BigDecimal, 
	n2: BigDecimal 
)

class BuiltinAdd2 {
	def evaluate(scope: Map[String, JsValue], eb: EntityBase): ContextT[JsObject] = {
		ContextT.context("add2") {
			for {
				//params <- Converter.convAs[BuiltinAddParams](JsObject(scope), eb, None)
				n1 <- Converter2.toBigDecimal(scope, "n1")
				n2 <- Converter2.toBigDecimal(scope, "n2")
			} yield {
				Converter2.makeNumber(n1 + n2)
			}
		}
	}
}
*/

case class BuiltinAddParams(
	numbers: List[BigDecimal] 
)

class BuiltinAdd {
	def evaluate(scope: Map[String, JsValue], eb: EntityBase): ContextT[JsObject] = {
		ContextT.context("add") {
			for {
				params <- Converter2.fromJson[BuiltinAddParams](JsObject(scope))
			} yield {
				Converter2.makeNumber(params.numbers.sum)
			}
		}
	}
}