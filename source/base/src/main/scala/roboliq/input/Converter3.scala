package roboliq.input

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.Type
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.typeOf
import grizzled.slf4j.Logger
import roboliq.core.RsError

object Converter3 {
	private val logger = Logger[this.type]
	
	def toRjsList(rjsval: RjsValue): ContextE[RjsList] = {
		rjsval match {
			case x: RjsList => ContextE.unit(x)
			case _ => ContextE.error(s"cannot convert to list: $rjsval")
		}
	}
	
	def toRjsText(rjsval: RjsValue): ContextE[RjsText] = {
		ContextE.unit(RjsText(rjsval.toText))
	}
	
	def toString(rjsval: RjsValue): ContextE[String] = {
		ContextE.context("toString") {
			rjsval match {
				case RjsString(text) => ContextE.unit(text)
				case RjsMap(m) =>
					for {
						_ <- ContextE.assert(m.get("TYPE") == "string", s"expected RjsString or TYPE=string")
						jsval2 <- ContextE.from(m.get("VALUE"), s"expected VALUE for string")
						x <- toString(jsval2)
					} yield x
				case _ => ContextE.error("expected RjsString")
			}
		}
	}
	
	def toInt(rjsval: RjsValue): ContextE[Int] = {
		toBigDecimal(rjsval).map(_.toInt)
	}
	
	def toInteger(rjsval: RjsValue): ContextE[Integer] = {
		toBigDecimal(rjsval).map(_.toInt)
	}
	
	def toDouble(rjsval: RjsValue): ContextE[Double] = {
		toBigDecimal(rjsval).map(_.toDouble)
	}
	
	def toBigDecimal(rjsval: RjsValue): ContextE[BigDecimal] = {
		ContextE.context("toBigDecimal") {
			rjsval match {
				case RjsNumber(n, None) => ContextE.unit(n)
				case _ => ContextE.error("expected RjsNumber without units")
			}
		}
	}
	
	def toBoolean(rjsval: RjsValue): ContextE[java.lang.Boolean] = {
		ContextE.context("toBoolean") {
			rjsval match {
				case RjsMap(m) =>
					for {
						_ <- ContextE.assert(m.get("TYPE") == "boolean", s"expected RjsBoolean or TYPE=boolean")
						jsval2 <- ContextE.from(m.get("VALUE"), s"expected VALUE for boolean")
						x <- toBoolean(jsval2)
					} yield x
				case RjsBoolean(b) => ContextE.unit(b)
				case _ => ContextE.error("expected RjsBoolean")
			}
		}
	}
	
	def toEnum[A <: Enumeration#Value : TypeTag](rjsval: RjsValue): ContextE[A] = {
		val typ = ru.typeTag[A].tpe
		toEnum(rjsval, typ).map(_.asInstanceOf[A])
	}
	
	private def toEnum(rjsval: RjsValue, typ: ru.Type): ContextE[Any] = {
		try {
			//val mirror = clazz_?.map(clazz => ru.runtimeMirror(clazz.getClassLoader)).getOrElse(scala.reflect.runtime.currentMirror)
			val mirror = ru.runtimeMirror(getClass.getClassLoader)
			// Get enclosing enumeration (e.g. MyStatus.Value => MyStatus)
			val enumType = typ.find(_ <:< typeOf[Enumeration]).get
			val enumModule = enumType.termSymbol.asModule
			val enumMirror = mirror.reflectModule(enumModule)
			val enum = enumMirror.instance.asInstanceOf[Enumeration]
			val value_l = enum.values
			rjsval match {
				case RjsString(s) =>
					ContextE.from(value_l.find(_.toString == s), s"Value '$s' not valid for `${enumModule.name}`.  Expected one of ${value_l.mkString(", ")}.")
				case _ => ContextE.error("expected RjsString")
			}
		} catch {
			case e: Throwable => ContextE.error(s"type: $typ, rjsval: $rjsval, error: ${e.getMessage}")
		}
	}
	
	def fromRjs[A: TypeTag](
		rjsval: RjsValue
	): ContextE[A] = {
		//println(s"fromRjson($rjsval)")
		val typ = ru.typeTag[A].tpe
		for {
			o <- conv(rjsval, typ)
			//_ <- ContextE.assert(o.isInstanceOf[A], s"INTERNAL: mis-converted JSON: `$rjsval` to `$o`")
		} yield o.asInstanceOf[A]
	}

	private def conv(
		rjsval: RjsValue,
		typ: Type,
		path_? : Option[String] = None
	): ContextE[Any] = {
		//println(s"conv($rjsval)")
		import scala.reflect.runtime.universe._

		val mirror = runtimeMirror(this.getClass.getClassLoader)

		//val path = path_r.reverse.mkString(".")
		//val prefix = if (path_r.isEmpty) "" else path + ": "
		//logger.trace(s"conv(${path}, $rjsval, $typ, eb)")

		val ctx: ContextE[Any] = {
			val c = typ.typeSymbol.asClass
			val clazz = rjsval.getClass()
			val m = ru.runtimeMirror(this.getClass.getClassLoader)
			if (m.reflect(rjsval).symbol.toType <:< typ) ContextE.unit(rjsval)
			else if (typ =:= typeOf[RjsMap] && rjsval.isInstanceOf[RjsMap]) ContextE.unit(rjsval)
			else if (typ =:= typeOf[RjsNumber] && rjsval.isInstanceOf[RjsNumber]) ContextE.unit(rjsval)
			else if (typ =:= typeOf[RjsText]) toRjsText(rjsval)
			else if (typ =:= typeOf[RjsList]) toRjsList(rjsval)
			
			else if (typ =:= typeOf[String]) Converter3.toString(rjsval)
			else if (typ =:= typeOf[Int]) toInt(rjsval)
			else if (typ =:= typeOf[Integer]) toInteger(rjsval)
			else if (typ =:= typeOf[Double]) toDouble(rjsval)
			else if (typ =:= typeOf[BigDecimal]) toBigDecimal(rjsval)
			else if (typ =:= typeOf[Boolean]) toBoolean(rjsval)//.map(_.asInstanceOf[Boolean])
			else if (typ =:= typeOf[java.lang.Boolean]) toBoolean(rjsval)
			else if (typ <:< typeOf[Enumeration#Value]) toEnum(rjsval, typ)
			/*
			else if (typ =:= typeOf[AmountSpec]) toAmountSpec(rjsval)
			else if (typ =:= typeOf[LiquidSource]) toLiquidSource(rjsval, eb, state_?)
			else if (typ =:= typeOf[LiquidVolume]) toVolume(rjsval)
			else if (typ =:= typeOf[PipetteAmount]) toPipetteAmount(rjsval)
			else if (typ =:= typeOf[PipetteDestination]) toPipetteDestination(rjsval, eb, state_?)
			else if (typ =:= typeOf[PipetteDestinations]) toPipetteDestinations(rjsval, eb, state_?)
			else if (typ =:= typeOf[PipetteSources]) toPipetteSources(rjsval, eb, state_?)
			// Logic
			else if (typ =:= typeOf[Strips.Literal]) toStripsLiteral(rjsval)
			// Lookups
			else if (typ =:= typeOf[Agent]) toEntityByRef[Agent](rjsval, eb)
			else if (typ =:= typeOf[Labware]) toEntityByRef[Labware](rjsval, eb)
			else if (typ =:= typeOf[Pipetter]) toEntityByRef[Pipetter](rjsval, eb)
			else if (typ =:= typeOf[Shaker]) toEntityByRef[Shaker](rjsval, eb)
			else if (typ =:= typeOf[TipModel]) toEntityByRef[TipModel](rjsval, eb)
			//else if (typ <:< typeOf[Substance]) toSubstance(rjsval)
			*/
			else if (typ <:< typeOf[Option[_]]) {
				val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
				if (rjsval == RjsNull) ContextE.unit(None)
				else conv(rjsval, typ2).map(o => Option(o))
			}
			else if (typ <:< typeOf[List[_]]) {
				val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
				convList(rjsval, typ2)
			}
			else if (typ <:< typeOf[Set[_]]) {
				val typ2 = typ.asInstanceOf[ru.TypeRefApi].args.head
				rjsval match {
					case rjsobj @ RjsMap(fields) =>
						convSet(rjsobj, typ2)
					case _ =>
						convList(rjsval, typ2).map(l => Set(l : _*))
				}
			}
			else if (typ <:< typeOf[Map[_, _]]) {
				rjsval match {
					case rjsobj @ RjsMap(fields) =>
						//println("fields: " + fields)
						val typKey = typ.asInstanceOf[ru.TypeRefApi].args(0)
						val typVal = typ.asInstanceOf[ru.TypeRefApi].args(1)
						val name_l = fields.toList.map(_._1)
						val nameToType_l = name_l.map(_ -> typVal)
						for {
							res <- convMap(rjsobj, typKey, nameToType_l)
						} yield res
					case RjsNull => ContextE.unit(Map())
					case _ =>
						ContextE.error("expected a RjsMap")
				}
			}
			else {
				//println("typ: "+typ)
				val ctor = typ.member(termNames.CONSTRUCTOR).asMethod
				val p0_l = ctor.paramLists(0)
				val nameToType_l = p0_l.map(p => p.name.decodedName.toString.replace("_?", "") -> p.typeSignature)
				for {
					nameToObj_m <- rjsval match {
						case rjsobj: RjsMap =>
							convMapString(rjsobj, nameToType_l)
						//case RjsList(jsval_l) =>
						//	convListToObject(jsval_l, nameToType_l)
						/*case RjsString(s) =>
							eb.getEntity(s) match {
								case Some(obj) =>
									// FIXME: need to check type rather than just assuming that it's correct!
									ContextE.unit(Left(ConvObject(obj)))
								case None =>
									for {
										nameToVal_l <- parseStringToArgs(s)
										//_ = println("nameToVal_l: "+nameToVal_l.toString)
										res <- convArgsToMap(nameToVal_l, typ, nameToType_l, eb, state_?, id_?)
										//_ = println("res: "+res.toString)
									} yield res
							}
						*/
						//case _ =>
						//	convListToObject(List(rjsval), nameToType_l)
						case _ =>
							ContextE.error(s"unhandled type or value. type=${typ}, value=${rjsval}")
					}
				} yield {
					val arg_l = nameToType_l.map(pair => nameToObj_m(pair._1))
					//println("arg_l: "+arg_l)
					val mm = mirror.reflectClass(c).reflectConstructor(ctor)
					//logger.debug("arg_l: "+arg_l)
					val obj = mm(arg_l : _*)
					obj
				}
			}
		}
		path_? match {
			case None => ctx
			case Some(path) => ContextE.context(path)(ctx)
		}
	}

		
	private def convList(
		rjsval: RjsValue,
		typ2: Type
	): ContextE[List[Any]] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)

		rjsval match {
			case RjsList(v) =>
				ContextE.mapAll(v.zipWithIndex) { case (jsval2, i0) =>
					val i = i0 + 1
					conv(jsval2, typ2, Some(s"[$i]"))
				}
			case RjsNull =>
				ContextE.unit(Nil)
			case _ =>
				ContextE.or(
					conv(rjsval, typ2).map(List(_)),
					ContextE.error(s"expected an array of ${typ2.typeSymbol.name.toString}.  Instead found: ${rjsval}")
				)
		}
	}
	
	private def convSet(
		rjsobj: RjsMap,
		typ2: Type
	): ContextE[Set[Any]] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)

		// Try to convert each element of the array
		ContextE.mapAll(rjsobj.map.toList) ({ case (id, rjsval) =>
			conv(rjsval, typ2, Some(id))
		}).map(l => Set(l : _*))
	}

	private def convListToObject(
		jsval_l: List[RjsValue],
		nameToType_l: List[(String, ru.Type)]
	): ContextE[Map[String, _]] = {
		ContextE.error("convListToObject: not yet implemented")
	}

	private def convMap(
		rjsobj: RjsMap,
		typKey: Type,
		nameToType_l: List[(String, ru.Type)]
	): ContextE[Map[_, _]] = {
		import scala.reflect.runtime.universe._
		
		val mirror = runtimeMirror(this.getClass.getClassLoader)
		
		/*println("convMap: ")
		println(path_r)
		println(rjsobj)
		println(typKey)
		println(nameToType_l)*/

		// TODO: Handle keys if they need to be looked up -- this just uses strings
		val key_l = nameToType_l.map(_._1)
		
		// Try to convert each element of the object
		for {
			val_l <- ContextE.map(nameToType_l) { case (name, typ2) =>
				rjsobj.get(name) match {
					case Some(jsval2) => conv(jsval2, typ2, Some(name))
					// Field is missing, so try using RjsNull
					case None => conv(RjsNull, typ2, Some(name))
				}
			}
		} yield {
			(key_l zip val_l).toMap
		}
	}

	private def convMapString(
		rjsobj: RjsMap,
		nameToType_l: List[(String, ru.Type)]
	): ContextE[Map[String, _]] = {
		for {
			map <- convMap(rjsobj, typeOf[String], nameToType_l)
		} yield map.asInstanceOf[Map[String, _]]
	}
	
	def valueToString(rjsval: RjsValue): ContextE[String] = {
		rjsval match {
			case RjsString(s) => ContextE.unit(s)
			case _ => ContextE.unit(rjsval.toString)
		}
	}
}