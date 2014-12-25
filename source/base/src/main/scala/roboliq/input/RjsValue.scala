package roboliq.input

import scala.reflect.runtime.{universe => ru}
import spray.json.JsObject
import spray.json.JsValue
import spray.json.JsString
import spray.json.JsNumber
import spray.json.JsArray
import spray.json.JsBoolean
import spray.json.JsBoolean
import spray.json.JsNull
import roboliq.ai.strips
import roboliq.core.ResultC
import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat

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
	def toJson: ResultC[JsValue]
	def toText: String = toString
}

sealed trait RjsBasicValue extends RjsValue

case class RjsBoolean(b: Boolean) extends RjsBasicValue {
	def toJson: ResultC[JsValue] = ResultC.unit(JsBoolean(b))
	override def toText: String = b.toString
}

case class RjsFormat(format: String) extends RjsBasicValue {
	def toJson: ResultC[JsValue] = ResultC.unit(JsString("f\""+format+"\""))
}

case class RjsList(list: List[RjsValue]) extends RjsBasicValue {
	def toJson: ResultC[JsValue] = {
		for {
			l <- ResultC.map(list) { _.toJson }
		} yield {
			JsArray(l)
		}
	}
}

trait RjsAbstractMap {
	val typ_? : Option[String]
	def getValueMap: Map[String, RjsValue]
}

case class RjsBasicMap(map: Map[String, RjsBasicValue]) extends RjsBasicValue with RjsAbstractMap {
	val typ_? : Option[String] = {
		map.get("TYPE") match {
			case Some(RjsString(typ)) => Some(typ)
			case _ => None
		}
	}

	def getValueMap: Map[String, RjsValue] = map
	
	def toJson: ResultC[JsValue] = {
		for {
			l <- ResultC.map(map.toList) { case (name, value) =>
				value.toJson.map(name -> _)
			}
		} yield JsObject(l.toMap)
	}
	
	def get(name: String): Option[RjsBasicValue] = map.get(name)
	def add(name: String, value: RjsBasicValue): RjsBasicMap = RjsBasicMap(map + (name -> value))
	def add(map: Map[String, RjsBasicValue]): RjsBasicMap = RjsBasicMap(this.map ++ map)
	def add(map: RjsBasicMap): RjsBasicMap = RjsBasicMap(this.map ++ map.map)
	def ++(that: RjsBasicMap): RjsBasicMap = this.add(that)
	
	def toTypedMap(typ: String): RjsBasicMap = RjsBasicMap(typ, map)
	
	def merge(that: RjsBasicMap): ResultC[RjsBasicMap] = {
		val key_l = map.keySet ++ that.map.keySet
		for {
			merged_l <- ResultC.map(key_l) { key =>
				val value_? : ResultC[RjsBasicValue] = (map.get(key), that.map.get(key)) match {
					case (None, None) => ResultC.error(s"internal merge error on key $key") // Won't happen
					case (Some(a), None) => ResultC.unit(a)
					case (None, Some(b)) => ResultC.unit(b)
					case (Some(a), Some(b)) =>
						(a, b) match {
							case (ma: RjsBasicMap, mb: RjsBasicMap) =>
								for {
									_ <- ResultC.check(ma.typ_? == mb.typ_?, s"Changing TYPE from ${ma.typ_?} to ${mb.typ_?}")
									mc <- ma merge mb
								} yield mc
							case _ =>
								ResultC.unit(b)
						}
				}
				value_?.map(key -> _)
			}
		} yield RjsBasicMap(merged_l.toMap)
	}
	
	override def toString: String = {
		map.toList.sortBy(_._1).map(kv => "\""+kv._1+"\" -> "+kv._2).mkString("RjsBasicMap(", ", ", ")")
	}
}

object RjsBasicMap {
	def apply(nv_l: (String, RjsBasicValue)*): RjsBasicMap = {
		RjsBasicMap(Map(nv_l : _*))
	}
	def apply(typ: String, nv_l: (String, RjsBasicValue)*): RjsBasicMap = {
		val map = Map(nv_l : _*) + ("TYPE" -> RjsString(typ))
		RjsBasicMap(map)
	}
	def apply(typ: String, map: Map[String, RjsBasicValue]): RjsBasicMap = {
		RjsBasicMap(map + ("TYPE" -> RjsString(typ)))
	}
}

case object RjsNull extends RjsBasicValue {
	def toJson: ResultC[JsValue] = ResultC.unit(JsNull)
	override def toText: String = "null"
}

case class RjsNumber(n: BigDecimal, unit: Option[String] = None) extends RjsBasicValue {
	def toJson: ResultC[JsValue] = unit match {
		case None => ResultC.unit(JsNumber(n))
		case Some(s) => ResultC.unit(JsString(n.toString+s))
	}
	override def toText: String = unit match {
		case None => n.toString
		case Some(s) => n.toString+s
	}
}

/**
 * This is a string which can represent anything that can be encoded as a string.
 * It is not meant as text to be displayed -- for that see RjsText.
 */
case class RjsString(s: String) extends RjsBasicValue {
	def toJson: ResultC[JsValue] = ResultC.unit(JsString(s))
	override def toText: String = s
	override def toString: String = "RjsString(\""+s+"\")"
}

case class RjsSubst(name: String) extends RjsBasicValue {
	def toJson: ResultC[JsValue] = ResultC.unit(JsString("$"+name))
	override def toString: String = "RjsString(\""+name+"\")"
}

case class RjsText(text: String) extends RjsBasicValue {
	def toJson: ResultC[JsValue] = ResultC.unit(JsString("\""+text+"\""))
	override def toText: String = text
	override def toString: String = "RjsString(\""+text+"\")"
}

/*
case class RjsTypedMap(typ: String, map: Map[String, RjsValue]) extends RjsBasicValue {
	def toJson: ResultC[JsValue] = {
		for {
			l <- ResultC.map(map.toList) { case (name, value) =>
				value.toJson.map(name -> _)
			}
		} yield JsObject((("TYPE" -> JsString(typ)) :: l).toMap)
	}
	
	def toUntypedMap: RjsMap = RjsMap(map)
}
*/

@RjsJsonType("action")
case class RjsAction(
	@RjsJsonName("NAME") name: String,
	@RjsJsonName("INPUT") input: RjsMap
) extends RjsValue {
	def toJson = RjsValue.toJson(this)
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

@RjsJsonType("actionDef")
case class RjsActionDef(
	@RjsJsonName("DESCRIPTION") description_? : Option[String],
	@RjsJsonName("DOCUMENTATION") documentation_? : Option[String],
	@RjsJsonName("PARAMS") params: Map[String, RjsActionDefParam],
	@RjsJsonName("PRECONDS") preconds: List[strips.Literal],
	@RjsJsonName("EFFECTS") effects: List[strips.Literal],
	@RjsJsonName("VALUE") value: RjsValue
) extends RjsValue {
	//def toJson = RjsValue.toJson(this)
	def toJson: ResultC[JsValue] = {
		for {
			jsValue <- value.toJson
		} yield {
			val l = List[Option[(String, JsValue)]](
				description_?.map(s => "DESCRIPTION" -> JsString(s)),
				documentation_?.map(s => "DOCUMENTATION" -> JsString(s)),
				Some("PARAMS" -> JsObject(params.mapValues(_.toJson))),
				Some("PRECONDS" -> JsArray(preconds.map(lit => JsString(lit.toString)))),
				Some("EFFECTS" -> JsArray(effects.map(lit => JsString(lit.toString)))),
				Some("VALUE" -> jsValue)
			)
			JsObject(l.flatten.toMap)
		}
	}
}

sealed trait RjsBuildItem {
	def toJson: ResultC[JsObject]
}

case class RjsBuildItem_VAR(name: String, value: RjsValue) extends RjsBuildItem {
	def toJson: ResultC[JsObject] = {
		for {
			jsValue <- value.toJson
		} yield {
			JsObject(Map("VAR" -> JsObject(Map("NAME" -> JsString(name), "VALUE" -> jsValue))))
		}
	}
}

case class RjsBuildItem_ADD(value: RjsValue) extends RjsBuildItem {
	def toJson: ResultC[JsObject] = {
		for {
			jsValue <- value.toJson
		} yield {
			JsObject(Map("ADD" -> jsValue))
		}
	}
}

case class RjsBuild(item_l: List[RjsBuildItem]) extends RjsValue {
	def toJson: ResultC[JsValue] = {
		for {
			l <- ResultC.map(item_l) { _.toJson }
		} yield {
			JsObject(Map("TYPE" -> JsString("build"), "ITEM" -> JsArray(l)))
		}
	}
}

@RjsJsonType("call")
case class RjsCall(
	@RjsJsonName("NAME") name: String,
	@RjsJsonName("INPUT") input: RjsMap
) extends RjsValue {
	def toJson = RjsValue.toJson(this)
}

@RjsJsonType("define")
case class RjsDefine(
	@RjsJsonName("NAME") name: String,
	@RjsJsonName("VALUE") value: RjsValue
) extends RjsValue {
	def toJson = RjsValue.toJson(this)
}

@RjsJsonType("import")
case class RjsImport(
	@RjsJsonName("NAME") name: String,
	@RjsJsonName("VERSION") version: String
) extends RjsValue {
	def toJson = RjsValue.toJson(this)
}

@RjsJsonType("include")
case class RjsInclude(
	@RjsJsonName("FILENAME") filename: String
) extends RjsValue {
	def toJson = RjsValue.toJson(this)
}

@RjsJsonType("instruction")
case class RjsInstruction(
	@RjsJsonName("NAME") name: String,
	@RjsJsonName("INPUT") input: RjsMap
) extends RjsValue {
	def toJson = RjsValue.toJson(this)
}

@RjsJsonType("lambda")
case class RjsLambda(
	@RjsJsonName("PARAMS") param: List[String],
	@RjsJsonName("EXPRESSION") expression: RjsValue
) extends RjsValue {
	def toJson = RjsValue.toJson(this)
}

case class RjsMap(map: Map[String, RjsValue]) extends RjsValue with RjsAbstractMap {
	val typ_? : Option[String] = {
		map.get("TYPE") match {
			case Some(RjsString(typ)) => Some(typ)
			case _ => None
		}
	}
	
	def getValueMap: Map[String, RjsValue] = map
	
	def toJson: ResultC[JsValue] = {
		for {
			l <- ResultC.map(map.toList) { case (name, value) =>
				value.toJson.map(name -> _)
			}
		} yield JsObject(l.toMap)
	}
	
	def get(name: String): Option[RjsValue] = map.get(name)
	def add(name: String, value: RjsValue): RjsMap = RjsMap(map + (name -> value))
	def add(map: Map[String, RjsValue]): RjsMap = RjsMap(this.map ++ map)
	def add(map: RjsMap): RjsMap = RjsMap(this.map ++ map.map)
	def ++(that: RjsMap): RjsMap = this.add(that)
	
	def toTypedMap(typ: String): RjsMap = RjsMap(typ, map)
	
	def merge(that: RjsMap): ResultC[RjsMap] = {
		val key_l = map.keySet ++ that.map.keySet
		for {
			merged_l <- ResultC.map(key_l) { key =>
				val value_? : ResultC[RjsValue] = (map.get(key), that.map.get(key)) match {
					case (None, None) => ResultC.error(s"internal merge error on key $key") // Won't happen
					case (Some(a), None) => ResultC.unit(a)
					case (None, Some(b)) => ResultC.unit(b)
					case (Some(a), Some(b)) =>
						(a, b) match {
							case (ma: RjsMap, mb: RjsMap) =>
								for {
									_ <- ResultC.check(ma.typ_? == mb.typ_?, s"Changing TYPE from ${ma.typ_?} to ${mb.typ_?}")
									mc <- ma merge mb
								} yield mc
							case _ =>
								ResultC.unit(b)
						}
				}
				value_?.map(key -> _)
			}
		} yield RjsMap(merged_l.toMap)
	}
	
	override def toString: String = {
		map.toList.sortBy(_._1).map(kv => "\""+kv._1+"\" -> "+kv._2).mkString("RjsMap(", ", ", ")")
	}
}

object RjsMap {
	def apply(nv_l: (String, RjsValue)*): RjsMap = {
		RjsMap(Map(nv_l : _*))
	}
	def apply(typ: String, map: Map[String, RjsValue]): RjsMap = {
		RjsMap(map + ("TYPE" -> RjsString(typ)))
	}
}

case class RjsProtocolLabware(
	model_? : Option[String] = None,
	location_? : Option[String] = None
) extends RjsValue {
	def toJson: ResultC[JsValue] = {
		ResultC.unit(JsObject(List[Option[(String, JsValue)]](
			model_?.map("model" -> JsString(_)),
			location_?.map("location" -> JsString(_))
		).flatten.toMap))
	}
}

case class RjsProtocolSubstance() extends RjsValue {
	def toJson: ResultC[JsValue] = {
		ResultC.unit(JsObject(List[Option[(String, JsValue)]](
		).flatten.toMap))
	}
}

case class RjsProtocolSourceSubstance(
	name: String,
	amount_? : Option[String] = None
) extends RjsValue {
	def toJson: ResultC[JsValue] = {
		ResultC.unit(JsObject(List[Option[(String, JsValue)]](
			Some("name" -> JsString(name)),
			amount_?.map("amount" -> JsString(_))
		).flatten.toMap))
	}
}

case class RjsProtocolSource(
	well: String,
	substances: List[RjsProtocolSourceSubstance] = Nil
) extends RjsValue {
	def toJson: ResultC[JsValue] = {
		for {
			jsSubstances <- ResultC.map(substances)(_.toJson)
		} yield {
			JsObject(List[Option[(String, JsValue)]](
				Some("well" -> JsString(well)),
				if (substances.isEmpty) None else Some("substances" -> JsArray(jsSubstances))
			).flatten.toMap)
		}
	}
}

@RjsJsonType("protocol")
case class RjsProtocol(
	labwares: Map[String, RjsProtocolLabware],
	substances: Map[String, RjsProtocolSubstance],
	sources: Map[String, RjsProtocolSource],
	commands: List[RjsValue]
) extends RjsValue {
	def toJson = RjsValue.toJson(this)
}

@RjsJsonType("section")
case class RjsSection(
	@RjsJsonName("BODY") body: List[RjsValue]
) extends RjsValue {
	def toJson = RjsValue.toJson(this)
}

object RjsValue {
	/**
	 * Convert a JSON object to a basic RjsValue, one of: Boolean, List, Number, Null, Text, Format, Subst, String, Map, or TypedMap.
	 * An error can occur if the text of a JsString is not properly formatted.
	 */
	def fromJson(jsval: JsValue): ResultC[RjsBasicValue] = {
		println(s"RjsValue.fromJson($jsval)")
		jsval match {
			case JsBoolean(b) =>
				ResultC.unit(RjsBoolean(b))
			case JsArray(l) =>
				ResultC.mapAll(l.zipWithIndex)({ case (jsval2, i) =>
					ResultC.context(s"[${i+1}]") {
						fromJson(jsval2)
					}
				}).map(RjsList)
			case JsNumber(n) =>
				ResultC.unit(RjsNumber(n, None))
			case JsNull =>
				ResultC.unit(RjsNull)
			case JsString(s) =>
				// TODO: should use regular expressions for matching strings, or parsers
				if (s.startsWith("\"") && s.endsWith("\"")) {
					ResultC.unit(RjsText(s.substring(1, s.length - 1)))
				}
				else if (s.startsWith("f\"") && s.endsWith("\"")) {
					ResultC.unit(RjsFormat(s.substring(2, s.length - 1)))
				}
				else if (s.startsWith("$")) {
					ResultC.unit(RjsSubst(s.tail))
				}
				// TODO: handle numbers and numbers with units
				else {
					ResultC.unit(RjsString(s))
				}
			case jsobj: JsObject =>
				ResultC.mapAll(jsobj.fields.toList)({ case (key, value) =>
					fromJson(value).map(key -> _)
				}).map(l => RjsBasicMap(l.toMap))
		}
	}

	def evaluateTypedMap(m: RjsAbstractMap): ResultE[RjsValue] = {
		m.typ_? match {
			case Some(typ) => evaluateTypedMap(typ, m.getValueMap)
			case None => ResultE.error("tried to evaluate an untyped map")
		}
	}
	
	def evaluateTypedMap(typ: String, map: Map[String, RjsValue]): ResultE[RjsValue] = {
		typ match {
			case "action" => convertMapAs[RjsAction](map)
			case "actionDef" => convertMapAs[RjsActionDef](map)
			case "call" => convertMapAs[RjsCall](map)
			case "define" => convertMapAs[RjsDefine](map)
			case "import" => convertMapAs[RjsImport](map)
			case "include" => convertMapAs[RjsInclude](map)
			case "instruction" => convertMapAs[RjsInstruction](map)
			case "lambda" => convertMapAs[RjsLambda](map)
			case "protocol" => convertMapAs[RjsProtocol](map)
			case "section" => convertMapAs[RjsSection](map)
			case _ =>
				ResultE.error(s"unable to convert from RjsMap to TYPE=${typ}.")
		}
	}
	
	import scala.reflect.runtime.{universe => ru}
	import scala.reflect.runtime.universe.Type
	import scala.reflect.runtime.universe.TypeTag
	import scala.reflect.runtime.universe.typeOf

	def convertMapAs[A <: RjsValue : TypeTag](
		map: Map[String, RjsValue]
	): ResultE[A] = {
		convertMap(map, ru.typeTag[A].tpe).map(_.asInstanceOf[A])
	}
	
	def convertMapAs[A <: RjsValue : TypeTag](
		m: RjsBasicMap
	): ResultE[A] = {
		convertMapAs[A](m.map)
	}
	
	def convertMapAs[A <: RjsValue : TypeTag](
		m: RjsMap
	): ResultE[A] = {
		convertMapAs[A](m.map)
	}
	
	def convertMap(
		map: Map[String, RjsValue],
		typ: Type
	): ResultE[Any] = {
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
			nameToObj_m <- RjsConverter.convMapString(RjsMap(map), nameToType_l)
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

	def toJson[A <: RjsValue : TypeTag](
		rjsval: A
	): ResultC[JsValue] = {
		val typ = ru.typeTag[A].tpe
		toJson(rjsval, typ)
	}

/*
import scala.reflect.runtime.universe._
val typ = typeOf[Option[Int]]
val x = Some(1)
val mirror = runtimeMirror(this.getClass.getClassLoader)
val im = mirror.reflect(x)
*/	

	def toJson(
		x: Any,
		typ: Type
	): ResultC[JsValue] = {
		import scala.reflect.runtime.universe._

		if (typ =:= typeOf[String]) ResultC.unit(JsString(x.asInstanceOf[String]))
		else if (typ =:= typeOf[Int]) ResultC.unit(JsNumber(x.asInstanceOf[Int]))
		else if (typ =:= typeOf[Integer]) ResultC.unit(JsNumber(x.asInstanceOf[Integer]))
		else if (typ =:= typeOf[Float]) ResultC.unit(JsNumber(x.asInstanceOf[Float]))
		else if (typ =:= typeOf[Double]) ResultC.unit(JsNumber(x.asInstanceOf[Double]))
		else if (typ =:= typeOf[BigDecimal]) ResultC.unit(JsNumber(x.asInstanceOf[BigDecimal]))
		else if (typ =:= typeOf[Boolean]) ResultC.unit(JsBoolean(x.asInstanceOf[Boolean]))
		else if (typ =:= typeOf[java.lang.Boolean]) ResultC.unit(JsBoolean(x.asInstanceOf[java.lang.Boolean]))
		else if (typ <:< typeOf[Enumeration#Value]) ResultC.unit(JsString(x.toString))
		else if (typ <:< typeOf[Option[_]]) {
			val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
			x.asInstanceOf[Option[_]] match {
				case None => ResultC.unit(JsNull)
				case Some(x2) => toJson(x2, typ2)
			}
		}
		else if (typ <:< typeOf[Map[String, _]]) {
			//val typKey = typ.asInstanceOf[ru.TypeRefApi].args(0)
			val typVal = typ.asInstanceOf[ru.TypeRefApi].args(1)
			val m0 = x.asInstanceOf[Map[String, _]]
			for {
				l <- ResultC.map(m0.toList) { case (name, v) =>
					toJson(v, typVal).map(name -> _)
				}
			} yield JsObject(l.toMap)
		}
		else if (typ <:< typeOf[Iterable[_]]) {
			val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
			val l0 = x.asInstanceOf[Iterable[_]]
			for {
				l <- ResultC.map(l0) { x2 => toJson(x2, typ2) }
			} yield JsArray(l.toList)
		}
		else {
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
			
			val mirror = runtimeMirror(this.getClass.getClassLoader)
			val im = mirror.reflect(x)
	
			val ctor = typ.member(termNames.CONSTRUCTOR).asMethod
			val p0_l = ctor.paramLists(0)
			for {
				field_l <- ResultC.mapAll(p0_l) { p =>
					val fm = im.reflectField(typ.decl(p.asTerm.name).asTerm)
					val nameAnnotation_? = p.annotations.find(a => a.tree.tpe == typJsonNameAnnotation)
					val name_? = nameAnnotation_?.flatMap { a =>
						val args = a.tree.children.tail
						val values = args.map(a => a.productElement(0).asInstanceOf[ru.Constant].value)
						values match {
							case List(name: String) => Some(name)
							case _ => None
						}
					}
					val name = name_?.getOrElse(p.name.toString)
					val x = fm.get
					for {
						jsval <- x match {
							case s: String => ResultC.unit(JsString(s))
							case rjsval2: RjsValue => rjsval2.toJson
						}
					} yield {
						name -> jsval
					}
				}
			} yield {
				val map = (jsTyp_?.toList ++ field_l).toMap
				JsObject(map)
			}
		}
	}
	
	def toBasicValue(rjsval: RjsValue): ResultC[RjsBasicValue] = {
		rjsval match {
			case x: RjsBasicValue => ResultC.unit(x)
			case _ =>
				for {
					jsval <- rjsval.toJson
					basic <- fromJson(jsval)
				} yield basic
		}
	}
	
	def merge(a: RjsValue, b: RjsValue): ResultC[RjsBasicValue] = {
		for {
			basic1 <- toBasicValue(a)
			basic2 <- toBasicValue(b)
			res <- (basic1, basic2) match {
				case (a: RjsBasicMap, b: RjsBasicMap) =>
					a merge b
				case _ => ResultC.unit(basic2)
			}
		} yield res
	}
	
	def merge(value_l: Iterable[RjsValue]): ResultC[RjsBasicValue] = {
		var a: RjsBasicValue = RjsNull
		for {
			_ <- ResultC.foreach(value_l) { b =>
				for {
					c <- merge(a, b)
				} yield {
					a = c
				}
			}
		} yield a
	}
}
