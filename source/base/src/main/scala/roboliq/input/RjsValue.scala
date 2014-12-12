package roboliq.input

import spray.json.JsObject
import spray.json.JsValue
import spray.json.JsString
import spray.json.JsNumber
import spray.json.JsArray
import spray.json.JsBoolean
import spray.json.JsBoolean
import spray.json.JsNull
import roboliq.ai.strips

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

case class RjsJsonType(typ: String) extends scala.annotation.StaticAnnotation
case class RjsJsonName(name: String) extends scala.annotation.StaticAnnotation

sealed abstract class RjsValue/*(val typ: RjsType.Value)*/ {
	def toJson: JsValue
	def toText: String = toString
}

@RjsJsonType("action")
case class RjsAction(
	@RjsJsonName("NAME") name: String,
	@RjsJsonName("INPUT") input: RjsMap
) extends RjsValue {
	def toJson: JsValue = {
		JsObject(Map[String, JsValue](
			"TYPE" -> JsString("action"),
			"NAME" -> JsString(name),
			"INPUT" -> JsObject(input.map.mapValues(_.toJson))
		))
	}
}

case class RjsActionDefParam(
	`type`: String,
	mode: InputMode.Value
) {
	def toJson: JsValue = {
		JsObject(Map(
			"type" -> JsString(`type`),
			"mode" -> JsString(mode.toString)
		))
	}
}

case class RjsActionDef(
	description_? : Option[String],
	documentation_? : Option[String],
	params: Map[String, RjsActionDefParam],
	preconds: List[strips.Literal],
	effects: List[strips.Literal],
	value: RjsValue
) extends RjsValue {
	def toJson: JsValue = {
		val l = List[Option[(String, JsValue)]](
			description_?.map(s => "DESCRIPTION" -> JsString(s)),
			documentation_?.map(s => "DOCUMENTATION" -> JsString(s)),
			Some("PARAMS" -> JsObject(params.mapValues(_.toJson))),
			Some("PRECONDS" -> JsArray(preconds.map(lit => JsString(lit.toString)))),
			Some("EFFECTS" -> JsArray(effects.map(lit => JsString(lit.toString)))),
			Some("VALUE" -> value.toJson)
		)
		JsObject(l.flatten.toMap)
	}
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
	def ++(that: RjsMap): RjsMap = this.add(that)
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

case class RjsProtocolLabware(
	model_? : Option[String] = None,
	location_? : Option[String] = None
) extends RjsValue {
	def toJson: JsValue = {
		JsObject(List[Option[(String, JsValue)]](
			model_?.map("model" -> JsString(_)),
			location_?.map("location" -> JsString(_))
		).flatten.toMap)
	}
}

case class RjsProtocolSubstance() extends RjsValue {
	def toJson: JsValue = {
		JsObject(List[Option[(String, JsValue)]](
		).flatten.toMap)
	}
}

case class RjsProtocolSourceSubstance(
	name: String,
	amount_? : Option[String] = None
) extends RjsValue {
	def toJson: JsValue = {
		JsObject(List[Option[(String, JsValue)]](
			Some("name" -> JsString(name)),
			amount_?.map("amount" -> JsString(_))
		).flatten.toMap)
	}
}

case class RjsProtocolSource(
	well: String,
	substances: List[RjsProtocolSourceSubstance] = Nil
) extends RjsValue {
	def toJson: JsValue = {
		JsObject(List[Option[(String, JsValue)]](
			Some("well" -> JsString(well)),
			if (substances.isEmpty) None else Some("substances" -> JsArray(substances.map(_.toJson)))
		).flatten.toMap)
	}
}

case class RjsProtocol(
	labwares: Map[String, RjsProtocolLabware],
	substances: Map[String, RjsProtocolSubstance],
	sources: Map[String, RjsProtocolSource],
	commands: List[RjsValue]
) extends RjsValue {
	def toJson: JsValue = {
		JsObject(Map(
			"TYPE" -> JsString("protocol"),
			"VALUE" -> JsObject(List[Option[(String, JsValue)]](
				if (labwares.isEmpty) None else Some("labwares" -> JsObject(labwares.mapValues(_.toJson))),
				if (substances.isEmpty) None else Some("substances" -> JsObject(substances.mapValues(_.toJson))),
				if (sources.isEmpty) None else Some("sources" -> JsObject(sources.mapValues(_.toJson))),
				if (commands.isEmpty) None else Some("commands" -> JsArray(commands.map(_.toJson)))
			).flatten.toMap)
		))
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
		println(s"RjsValue.fromJson($jsval)")
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
			case "action" =>
				for {
					name <- Converter2.fromJson[String](jsobj, "NAME")
					jsInput <- Converter2.fromJson[JsObject](jsobj, "INPUT")
					input_l <- ContextE.mapAll(jsInput.fields.toList) { case (name, jsval) =>
						RjsValue.fromJson(jsval).map(name -> _)
					}
				} yield RjsAction(name, RjsMap(input_l.toMap))
			case "actionDef" =>
				for {
					description_? <- Converter2.fromJson[Option[String]](jsobj, "DESCRIPTION")
					documentation_? <- Converter2.fromJson[Option[String]](jsobj, "DOCUMENTATION")
					params <- Converter2.fromJson[Map[String, RjsActionDefParam]](jsobj, "PARAMS")
					preconds0 <- Converter2.fromJson[List[String]](jsobj, "PRECONDS")
					preconds = preconds0.map { s => strips.Literal.parse(s) }
					effects0 <- Converter2.fromJson[List[String]](jsobj, "EFFECTS")
					effects = effects0.map { s => strips.Literal.parse(s) }
					value0 <- Converter2.fromJson[JsValue](jsobj, "VALUE")
					value <- RjsValue.fromJson(value0)
				} yield RjsActionDef(description_?, documentation_?, params, preconds, effects, value)
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

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.Type
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.typeOf

/*
import spray.json._
import scala.reflect.runtime.{universe => ru}
import roboliq.input._
val jsobj = JsObject("TYPE" -> JsString("action"), "NAME" -> JsString("someaction"), "INPUT" -> JsObject("a" -> JsNumber(1)))

val eb = new roboliq.entities.EntityBase
val data0 = ContextEData(EvaluatorState(eb))

RjsValue.fromJsObjectToRjs(jsobj, ru.typeOf[RjsAction]).run(data0)
*/

	def fromJsObjectToRjs(
		jsobj: JsObject,
		typ: Type
	): ContextE[Any] = {
		val ctor = typ.member(ru.termNames.CONSTRUCTOR).asMethod
		val p0_l = ctor.paramLists(0)
		val typA = ru.typeOf[RjsJsonName]
		val nameToType_l = p0_l.map { p =>
			val nameAnnotation_? = p.annotations.find(a => a.tree.tpe == typA)
			val name_? : Option[String] = nameAnnotation_?.flatMap { a =>
				val args = a.tree.children.tail
				val values = args.map(a => a.productElement(0).asInstanceOf[ru.Constant].value)
				values match {
					case List(name: String) => Some(name)
					case _ => None
				}
			}
			val name: String = name_?.getOrElse(p.name.decodedName.toString.replace("_?", ""))
			name -> p.typeSignature
		}
		
		for {
			nameToObj_m <- Converter2.convMapString(jsobj, nameToType_l)
		} yield {
			val arg_l = nameToType_l.map(pair => nameToObj_m(pair._1))
			val c = typ.typeSymbol.asClass
			//println("arg_l: "+arg_l)
			val mirror = ru.runtimeMirror(this.getClass.getClassLoader)
			val mm = mirror.reflectClass(c).reflectConstructor(ctor)
			//logger.debug("arg_l: "+arg_l)
			val obj = mm(arg_l : _*)
			obj
		}
	}
/*
import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.Type
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe._
case class A(name: String) extends scala.annotation.StaticAnnotation
case class X(@A("STRING") string: String)
val typ = typeOf[X]
val typClass = typ.typeSymbol.asClass
val typA = ru.typeOf[A]

val ctor = typ.member(termNames.CONSTRUCTOR).asMethod
val p0_l = ctor.paramLists(0)
val p = p0_l.head

val nameAnnotation_? = p.annotations.find(a => a.tree.tpe == typA)

val mirror = runtimeMirror(this.getClass.getClassLoader)

val typAnnotation_? = typClass.annotations.find(a => a.tree.tpe == typA)
val jsTyp_? : Option[(String, JsValue)] = typAnnotation_?.flatMap { a =>
	val args = a.tree.children.tail
	val values = args.map(a => a.productElement(0).asInstanceOf[ru.Constant].value)
	values match {
		case List(typ: String) => Some("TYPE" -> JsString(typ))
		case _ => None
	}
	}
*/

	/*
	def x(
		jsval: JsValue,
		typ: Type
	) {
		import scala.reflect.runtime.universe._

		val ctor = typ.member(termNames.CONSTRUCTOR).asMethod
		val p0_l = ctor.paramLists(0)
		val nameToType_l = p0_l.map(p => p.name.decodedName.toString.replace("_?", "") -> p.typeSignature)
		for {
			nameToObj_m <- jsval match {
				case jsobj: JsObject =>
					convMapString(jsobj, nameToType_l)
				case _ =>
					ContextE.error(s"unhandled type or value. type=${typ}, value=${jsval}")
			}
		} yield {
			val arg_l = nameToType_l.map(pair => nameToObj_m(pair._1))
			val c = typ.typeSymbol.asClass
			val mirror = runtimeMirror(this.getClass.getClassLoader)
			val mm = mirror.reflectClass(c).reflectConstructor(ctor)
			val obj = mm(arg_l : _*)
			obj
		}
		
	}*/
	
	/*
	def toJson(
		rjsval: RjsValue,
		typ: Type
	) {
		import scala.reflect.runtime.universe._

		// Get 'TYPE' field value, if annotated
		val typJsonTypeAnnotation = ru.typeOf[RjsJsonType]
		val typJsonNameAnnotation = ru.typeOf[RjsJsonName]
		val typClass = typ.typeSymbol.asClass
		val typAnnotation_? = typClass.annotations.find(a => a.tree.tpe == typJsonTypeAnnotation)
		val jsTyp_? : Option[(String, JsValue)] = typAnnotation_?.flatMap { a =>
			val args = a.tree.children.tail
			val values = args.map(a => a.productElement(0).asInstanceOf[ru.Constant].value)
			values match {
				case List(typ: String) => Some("TYPE" -> JsString(typ))
				case _ => None
			}
		}

		val ctor = typ.member(termNames.CONSTRUCTOR).asMethod
		val p0_l = ctor.paramLists(0)
		p0_l.flatMap { p =>
			val nameAnnotation_? = p.annotations.find(a => a.tree.tpe == typJsonNameAnnotation)
			val jsNameToParam_? : Option[(String, JsValue)] = nameAnnotation_?.flatMap { a =>
				val args = a.tree.children.tail
				val values = args.map(a => a.productElement(0).asInstanceOf[ru.Constant].value)
				values match {
					case List(name: String) =>
						typ.mem
						Some(name -> JsString(typ))
					case _ => None
				}
			}
		}
		val nameToType_l = p0_l.map(p => p.name.decodedName.toString.replace("_?", "") -> p.typeSignature)
	}
	*/
}
