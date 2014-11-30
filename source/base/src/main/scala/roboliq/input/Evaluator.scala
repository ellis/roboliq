package roboliq.input

import spray.json.JsObject
import spray.json.JsString
import spray.json.JsNumber
import spray.json.JsValue
import roboliq.entities.EntityBase
import spray.json.JsArray

/*
object RjsType extends Enumeration {
	val
		RCall,
		RIdent,
		RList,
		RMap,
		RNumber,
		RString,
		RSubst
		= Value
}

sealed abstract class RjsValue(val typ: RjsType.Value) {
	def toJson: JsObject
}
case class RjsCall(name: String, input: Map[String, JsValue]) extends RjsValue
case class RjsNumber(n: BigDecimal) extends RjsValue
*/

case class EvaluatorState(
	eb: EntityBase,
	scope: Map[String, JsObject]
)

class Evaluator() {
	
	def evaluate(jsval: JsValue): ContextE[JsObject] = {
		jsval match {
			// TODO: Add warnings for extra fields
			case jsobj @ JsObject(m) =>
				(m.get("TYPE"), m.get("VALUE")) match {
					case (None, None) =>
						ContextE.unit(JsObject(Map("TYPE" -> JsString("set"), "VALUE" -> jsval)))
					case (Some(JsString("call")), Some(JsString(nameFn))) =>
						val ctx: ContextE[Unit] = m.get("INPUT") match {
							case None =>
								ContextE.unit(())
							case Some(JsObject(input)) =>
								val l_? = ContextE.map(input.toList)({ case (name, jsval2) =>
									evaluate(jsval2).map(name -> _)
								})
								l_?.flatMap(l => ContextE.addToScope(l.toMap))
							case Some(input) =>
								ContextE.error("Expected `INPUT` of type JsObject: "+input)
						}
						for {
							_ <- ctx
							res <- evaluateCall(nameFn)
						} yield res
					case (Some(JsString(typ)), Some(jsval2)) =>
						evaluateType(typ, jsval2)
					case _ =>
						ContextE.error("Expected field `TYPE` of type JsString and field `VALUE`")
				}
			case JsNumber(n) =>
				ContextE.unit(Converter2.makeNumber(n))
			// A value
			case _ =>
				ContextE.error("Don't know how to evaluate: "+jsval)
		}
	}
	
	def evaluateCall(nameFn: String): ContextE[JsObject] = {
		for {
			res <- nameFn match {
				case "add" =>
					new BuiltinAdd().evaluate()
				case _ =>
					ContextE.error(s"Unknown function `$nameFn`")
			}
		} yield res
	}

	def evaluateType(typ: String, jsval: JsValue): ContextE[JsObject] = {
		(typ, jsval) match {
			case ("list", JsArray(l)) =>
				for {
					// TODO: need to push a context to a context 
					l2 <- ContextE.map(l.zipWithIndex) { case (jsval2, i0) =>
						val i = i0 + 1
						ContextE.context(s"[$i]") {
							evaluate(jsval2)
						}
					}
				} yield Converter2.makeList(l2)
			case ("number", JsNumber(n)) =>
				ContextE.unit(Converter2.makeNumber(n))
			case ("subst", JsString(name)) =>
				for {
					scope <- ContextE.getScope
					x <- ContextE.from(scope.get(name), s"variable `$name` not in scope")
				} yield x
			case _ =>
				ContextE.error("evaluateType() not completely implemented")
		}
	}
}