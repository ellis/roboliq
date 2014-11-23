package roboliq.input

import spray.json.JsObject
import spray.json.JsString
import spray.json.JsNumber
import spray.json.JsValue
import roboliq.entities.EntityBase

/*
trait RjsValue
case class RjsCall(name: String, input: Map[String, JsValue]) extends RjsValue
case class RjsNumber(n: BigDecimal) extends RjsValue
*/

class Evaluator(
	eb: EntityBase
) {
	
	def evaluate(jsval: JsValue, scope: Map[String, JsObject]): ContextT[JsObject] = {
		jsval match {
			// TODO: Add warnings for extra fields
			case jsobj @ JsObject(m) =>
				(m.get("TYPE"), m.get("VALUE")) match {
					case (None, None) =>
						ContextT.unit(JsObject(Map("TYPE" -> JsString("set"), "VALUE" -> jsval)))
					case (Some(JsString("call")), Some(JsString(nameFn))) =>
						for {
							scope2 <- m.get("INPUT") match {
								case None =>
									ContextT.unit(scope)
								case Some(JsObject(input)) =>
									ContextT.map(input.toList)({ case (name, jsval2) =>
										evaluate(jsval2, scope).map(name -> _)
									}).map(_.toMap).map(scope ++ _)
								case Some(input) =>
									ContextT.error("Expected `INPUT` of type JsObject: "+input)
							}
							res <- evaluateCall(nameFn, scope2)
						} yield res
					case (Some(JsString(typ)), Some(jsval2)) =>
						evaluateType(typ, jsval2, scope)
					case _ =>
						ContextT.error("Expected field `TYPE` of type JsString and field `VALUE`")
				}
			case JsNumber(n) =>
				ContextT.unit(Converter2.makeNumber(n))
			// A value
			case _ =>
				ContextT.error("Don't know how to evaluate: "+jsval)
		}
	}
	
	def evaluateCall(nameFn: String, scope: Map[String, JsObject]): ContextT[JsObject] = {
		for {
			res <- nameFn match {
				case "add" =>
					for {
						res <- new BuiltinAdd().evaluate(scope, eb)
					} yield res
				case _ =>
					ContextT.error(s"Unknown function `$nameFn`")
			}
		} yield res
	}

	def evaluateType(typ: String, jsval: JsValue, scope: Map[String, JsObject]): ContextT[JsObject] = {
		(typ, jsval) match {
			case ("number", JsNumber(n)) =>
				ContextT.unit(Converter2.makeNumber(n))
			case ("subst", JsString(name)) =>
				ContextT.from(scope.get(name), s"variable `$name` not in scope")
			case _ =>
				ContextT.error("evaluateType() not completely implemented")
		}
	}
}