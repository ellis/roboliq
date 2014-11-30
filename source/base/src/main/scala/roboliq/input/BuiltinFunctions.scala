package roboliq.input

import spray.json.JsValue
import roboliq.core.RsResult
import spray.json.JsObject
import roboliq.entities.EntityBase
import spray.json.JsNumber
import scala.collection.mutable.HashMap

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
	def evaluate(): ContextE[JsObject] = {
		ContextE.context("add") {
			for {
				params <- ContextE.fromScope[BuiltinAddParams]()
			} yield {
				Converter2.makeNumber(params.numbers.sum)
			}
		}
	}
}

case class BuiltinBuildInput(
	item: List[Map[String, JsObject]],
	transform: List[String]
)

class BuiltinBuild {
	def evaluate(scope: Map[String, JsValue], eb: EntityBase): ContextE[JsObject] = {
		ContextE.context("build") {
			for {
				input <- Converter2.fromJson[BuiltinBuildInput](JsObject(scope))
			} yield {
				null
			}
		}
	}
	
	private def makeItems(item_l: List[Map[String, JsObject]]): ContextT[Unit] = {
		val var_m = new HashMap[String, JsObject]
		for (m <- item_l) {
			m.toList match {
				case List(("VAR", JsObject(m2))) =>
					
				case List(("ADD", JsObject(m2))) =>
			}
		}
		ContextT.unit(())
	}
}