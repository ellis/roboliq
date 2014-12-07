package roboliq.input

import spray.json.JsObject
import spray.json.JsValue
import spray.json.JsString
import spray.json.JsNumber
import spray.json.JsArray
import spray.json.JsBoolean
import spray.json.JsBoolean
import spray.json.JsNull

/*object RjsType extends Enumeration {
	val
		RCall,
		RIdent,
		RList,
		RMap,
		RNumber,
		RString,
		RSubst
		= Value
}*/

sealed abstract class RjsValue/*(val typ: RjsType.Value)*/ {
	def toJson: JsValue
	def toText: String = toString
}

case class RjsBoolean(b: Boolean) extends RjsValue {
	def toJson: JsValue = JsBoolean(b)
	override def toText: String = b.toString
}

sealed trait RjsBuildItem {
	def toJson: JsObject
}

case class RjsBuildItem_VAR(name: String, value: RjsValue) extends RjsBuildItem {
	def toJson: JsObject = JsObject(Map("VAR" -> JsObject(Map("NAME" -> JsString(name), "VALUE" -> value.toJson))))
}

case class RjsBuildItem_ADD(value: RjsValue) extends RjsBuildItem {
	def toJson: JsObject = JsObject(Map("ADD" -> value.toJson))
}

case class RjsBuild(item_l: List[RjsBuildItem]) extends RjsValue {
	def toJson: JsValue = JsObject(Map("TYPE" -> JsString("build"), "ITEM" -> JsArray(item_l.map(_.toJson))))
}

case class RjsCall(name: String, input: RjsMap) extends RjsValue {
	def toJson: JsValue = {
		JsObject(Map[String, JsValue](
			"TYPE" -> JsString("call"),
			"NAME" -> JsString(name),
			"INPUT" -> JsObject(input.map.mapValues(_.toJson))
		))
	}
}

case class RjsDefine(name: String, value: RjsValue) extends RjsValue {
	def toJson: JsValue = JsObject(Map("TYPE" -> JsString("define"), "NAME" -> JsString(name), "VALUE" -> value.toJson))
}

case class RjsFormat(format: String) extends RjsValue {
	def toJson: JsValue = JsString("f\""+format+"\"")
}

case class RjsImport(name: String, version: String) extends RjsValue {
	def toJson: JsValue = {
		JsObject(Map(
			"TYPE" -> JsString("import"), 
			"NAME" -> JsString(name),
			"VERSION" -> JsString(version)
		))
	}
}

case class RjsInclude(filename: String) extends RjsValue {
	def toJson: JsValue = Converter2.makeInclude(filename)
}

case class RjsInstruction(name: String, input: RjsMap) extends RjsValue {
	def toJson: JsValue = {
		JsObject(Map(
			"TYPE" -> JsString("instruction"),
			"NAME" -> JsString(name),
			"INPUT" -> JsObject(input.map.mapValues(_.toJson))
		))
	}
}

case class RjsLambda(param: List[String], expression: RjsValue) extends RjsValue {
	def toJson: JsValue =
		JsObject(Map("TYPE" -> JsString("lambda"), "EXPRESSION" -> expression.toJson))
}

case class RjsList(list: List[RjsValue]) extends RjsValue {
	def toJson: JsValue = JsArray(list.map(_.toJson))
}

case class RjsMap(map: Map[String, RjsValue]) extends RjsValue {
	def toJson: JsValue = JsObject(map.mapValues(_.toJson))
	
	def get(name: String): Option[RjsValue] = map.get(name)
	def add(name: String, value: RjsValue): RjsMap = RjsMap(map + (name -> value))
	def add(map: Map[String, RjsValue]): RjsMap = RjsMap(this.map ++ map)
	def add(map: RjsMap): RjsMap = RjsMap(this.map ++ map.map)
}

object RjsMap {
	def apply(nv_l: (String, RjsValue)*): RjsMap = {
		RjsMap(Map(nv_l : _*))
	}
}

case object RjsNull extends RjsValue {
	def toJson: JsValue = JsNull
	override def toText: String = "null"
}

case class RjsNumber(n: BigDecimal, unit: Option[String] = None) extends RjsValue {
	def toJson: JsValue = unit match {
		case None => JsNumber(n)
		case Some(s) => JsString(n.toString+s)
	}
	override def toText: String = unit match {
		case None => n.toString
		case Some(s) => n.toString+s
	}
}

case class RjsSection(body: List[RjsValue]) extends RjsValue {
	def toJson: JsValue = {
		JsObject(Map(
			"TYPE" -> JsString("scope"),
			"BODY" -> JsArray(body.map(_.toJson))
		))
	}
}

/**
 * This is a string which can represent anything that can be encoded as a string.
 * It is not meant as text to be displayed -- for that see RjsText.
 */
case class RjsString(s: String) extends RjsValue {
	def toJson: JsValue = JsString(s)
	override def toText: String = s
}

case class RjsSubst(name: String) extends RjsValue {
	def toJson: JsValue = Converter2.makeSubst(name)
}

case class RjsText(text: String) extends RjsValue {
	def toJson: JsValue = JsString("\""+text+"\"")
	override def toText: String = text
}

object RjsValue {
	def fromJson(jsval: JsValue): ContextE[RjsValue] = {
		jsval match {
			case JsBoolean(b) =>
				ContextE.unit(RjsBoolean(b))
			case JsArray(l) =>
				ContextE.mapAll(l.zipWithIndex)({ case (jsval2, i) =>
					ContextE.context(s"[${i+1}]") {
						RjsValue.fromJson(jsval2)
					}
				}).map(RjsList)
			case JsNumber(n) =>
				ContextE.unit(RjsNumber(n, None))
			case JsNull =>
				ContextE.unit(RjsNull)
			case JsString(s) =>
				if (s.startsWith("\"") && s.endsWith("\"")) {
					ContextE.unit(RjsText(s.substring(1, s.length - 1)))
				}
				else if (s.startsWith("f\"") && s.endsWith("\"")) {
					ContextE.unit(RjsFormat(s.substring(2, s.length - 1)))
				}
				// TODO: handle numbers and numbers with units
				else {
					ContextE.unit(RjsString(s))
				}
			case jsobj: JsObject =>
				jsobj.fields.get("TYPE") match {
					case Some(JsString(typ)) =>
						fromJsObject(typ, jsobj)
					case _ =>
						ContextE.mapAll(jsobj.fields.toList)({ case (key, value) =>
							fromJson(value).map(key -> _)
						}).map(l => RjsMap(l.toMap))
				}
		}
	}
	
	private def fromJsObject(typ: String, jsobj: JsObject): ContextE[RjsValue] = {
		typ match {
			case "call" =>
				for {
					name <- Converter2.fromJson[String](jsobj, "NAME")
					jsInput <- Converter2.fromJson[JsObject](jsobj, "INPUT")
					input_l <- ContextE.mapAll(jsInput.fields.toList) { case (name, jsval) =>
						RjsValue.fromJson(jsval).map(name -> _)
					}
				} yield RjsCall(name, RjsMap(input_l.toMap))
			case "define" =>
				for {
					name <- Converter2.fromJson[String](jsobj, "NAME")
					jsval <- Converter2.fromJson[JsValue](jsobj, "VALUE")
					value <- RjsValue.fromJson(jsval)
				} yield RjsDefine(name, value)
			case "import" =>
				for {
					name <- Converter2.fromJson[String](jsobj, "NAME")
					version <- Converter2.fromJson[String](jsobj, "VERSION")
				} yield RjsImport(name, version)
			case "include" =>
				for {
					filename <- Converter2.fromJson[String](jsobj, "FILENAME")
				} yield RjsInclude(filename)
			case "instruction" =>
				for {
					name <- Converter2.fromJson[String](jsobj, "NAME")
					jsInput <- Converter2.fromJson[JsObject](jsobj, "INPUT")
					input_l <- ContextE.mapAll(jsInput.fields.toList) { case (name, jsval) =>
						RjsValue.fromJson(jsval).map(name -> _)
					}
				} yield RjsInstruction(name, RjsMap(input_l.toMap))
			case "lambda" =>
				for {
					jsval <- Converter2.fromJson[JsValue](jsobj, "EXPRESSION")
					expression <- RjsValue.fromJson(jsval)
				} yield RjsLambda(Nil, expression)
			case "section" =>
				for {
					jsBody_l <- Converter2.fromJson[List[JsValue]](jsobj, "BODY")
					body_l <- ContextE.mapAll(jsBody_l)(RjsValue.fromJson(_))
				} yield RjsSection(body_l)
			case "subst" =>
				for {
					name <- Converter2.fromJson[String](jsobj, "NAME")
				} yield RjsSubst(name)
			case _ =>
				ContextE.error(s"conversion for TYPE=$typ not implemented.")
		}
	}
}
