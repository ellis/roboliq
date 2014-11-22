package roboliq.input

import spray.json.JsValue
import roboliq.core.RsSuccess
import spray.json.JsString
import spray.json.JsObject
import roboliq.core.RsError
import spray.json.JsNumber
import spray.json.JsBoolean

object Converter2 {
	def toString(jsval: JsValue): ContextT[String] = {
		ContextT.context("toString") {
			jsval match {
				case JsString(text) => ContextT.unit(text)
				case JsObject(m) =>
					for {
						_ <- ContextT.assert(m.get("TYPE") == "string", s"expected JsString or TYPE=string")
						jsval2 <- ContextT.from(m.get("VALUE"), s"expected VALUE for string")
						x <- toString(jsval2)
					} yield x
				case _ => ContextT.error("expected JsString")
			}
		}
	}
	
	def toInt(jsval: JsValue): ContextT[Int] = {
		toBigDecimal(jsval).map(_.toInt)
	}
	
	def toInteger(jsval: JsValue): ContextT[Integer] = {
		toBigDecimal(jsval).map(_.toInt)
	}
	
	def toDouble(jsval: JsValue): ContextT[Double] = {
		toBigDecimal(jsval).map(_.toDouble)
	}
	
	def toBigDecimal(jsval: JsValue): ContextT[BigDecimal] = {
		ContextT.context("toBigDecimal") {
			jsval match {
				case JsObject(m) =>
					(m.get("TYPE"), m.get("VALUE")) match {
						case (Some(JsString("number")), Some(JsNumber(n))) =>
							ContextT.unit(n)
						case _ =>
							ContextT.error("Expected TYPE=number and VALUE of type JsNumber: "+jsval)
					}
				case JsNumber(n) => ContextT.unit(n)
				case _ => ContextT.error("expected JsNumber or JsObject")
			}
		}
	}
	
	def toBigDecimal(scope: Map[String, JsValue], name: String): ContextT[BigDecimal] = {
		scope.get(name) match {
			case None => ContextT.error(s"variable `$name` missing from scope")
			case Some(jsval) => toBigDecimal(jsval)
		}
	}
	
	def toBoolean(jsval: JsValue): ContextT[java.lang.Boolean] = {
		ContextT.context("toBoolean") {
			jsval match {
				case JsObject(m) =>
					for {
						_ <- ContextT.assert(m.get("TYPE") == "boolean", s"expected JsBoolean or TYPE=boolean")
						jsval2 <- ContextT.from(m.get("VALUE"), s"expected VALUE for boolean")
						x <- toBoolean(jsval2)
					} yield x
				case JsBoolean(b) => ContextT.unit(b)
				case _ => ContextT.error("expected JsBoolean")
			}
		}
	}
	
	def makeNumber(n: BigDecimal): JsObject = {
		JsObject(Map("TYPE" -> JsString("number"), "VALUE" -> JsNumber(n)))
	}
	
	def makeCall(name: String, input: Map[String, JsValue]): JsObject = {
		JsObject(Map("TYPE" -> JsString("call"), "VALUE" -> JsString(name), "INPUT" -> JsObject(input)))
	}
}