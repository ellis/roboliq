package roboliq.input

import spray.json.JsObject
import spray.json.JsString
import spray.json.JsNumber
import spray.json.JsValue
import roboliq.entities.EntityBase
import spray.json.JsArray
import scala.collection.mutable.HashMap

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

class Evaluator() {
	
	def evaluate(jsval: JsValue): ContextE[JsObject] = {
		println(s"evaluate($jsval)")
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
					case (Some(JsString("build")), None) =>
						evaluateBuild(m)
					case (Some(JsString("lambda")), None) =>
						evaluateLambda(m)
					case (Some(JsString("let")), None) =>
						evaluateLet(m)
					case (Some(JsString("stringf")), Some(JsString(format))) =>
						evaluateStringf(format)
					case (Some(JsString(typ)), Some(jsval2)) =>
						evaluateType(typ, jsval2)
					case _ =>
						ContextE.unit(Converter2.makeMap(m))
						//ContextE.error("Expected field `TYPE` of type JsString and field `VALUE`")
				}
			case JsNumber(n) =>
				ContextE.unit(Converter2.makeNumber(n))
			case JsArray(l0) =>
				for {
					l <- ContextE.mapAll(l0) { item => evaluate(item) }
				} yield Converter2.makeList(l)
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
				case "build" =>
					new BuiltinBuild().evaluate()
				case name =>
					evaluateLambdaCall(name)
			}
		} yield res
	}

	def evaluateBuild(m: Map[String, JsValue]): ContextE[JsObject] = {
		println(s"evaluateBuild($m)")
		m.get("ITEM") match {
			case Some(js_l: JsArray) =>
				for {
					item0_l <- ContextE.fromJson[List[Map[String, JsObject]]](js_l)
					item_l <- makeItems(item0_l)
				} yield Converter2.makeList(item_l)
			case _ =>
				ContextE.unit(Converter2.makeMap(Map()))
		}
	}

	private def makeItems(item_l: List[Map[String, JsObject]]): ContextE[List[JsObject]] = {
		println("makeItems:")
		item_l.foreach(println)
		val var_m = new HashMap[String, JsObject]
		
		def handleVAR(m: Map[String, JsValue]): ContextE[Unit] = {
			m.get("NAME") match {
				case Some(JsString(name)) =>
					val jsobj = JsObject(m - "NAME")
					for {
						x <- ContextE.evaluate(jsobj)
					} yield {
						var_m(name) = x
					}
				case _ =>
					ContextE.error("variable `NAME` must be supplied")
			}
		}

		def handleADD(jsobj: JsObject): ContextE[Option[JsObject]] = {
			jsobj.fields.get("TYPE") match {
				case Some(JsString("map")) =>
					jsobj.fields.get("VALUE") match {
						case None =>
							ContextE.unit(Some(Converter2.makeMap(var_m.toMap)))
						case Some(JsObject(m3)) =>
							ContextE.unit(Some(Converter2.makeMap(var_m.toMap ++ m3)))
						case x =>
							ContextE.error("invalid `VALUE`: "+x)
					}
				case _ =>
					ContextE.evaluate(jsobj).map(x => Some(x))
			}
		}
		
		for {
			output0_l <- ContextE.mapAll(item_l.zipWithIndex) { case (m, i) =>
				ContextE.context("item["+(i+1)+"]") {
					m.toList match {
						case List(("VAR", JsObject(m2))) =>
							handleVAR(m2).map(_ => None)
						case List(("ADD", jsobj2@JsObject(m2))) =>
							handleADD(jsobj2)
					}
				}
			}
		} yield output0_l.flatten
	}

	def evaluateLambda(m: Map[String, JsValue]): ContextE[JsObject] = {
		println(s"evaluateLambda($m)")
		ContextE.unit(JsObject(m))
	}

	def evaluateLambdaCall(name: String): ContextE[JsObject] = {
		println(s"evaluateLambdaCall($name)")
		for {
			scope <- ContextE.getScope
			res <- scope.get(name) match {
				case Some(JsObject(m)) if (m.get("TYPE") == Some(JsString("lambda"))) =>
					ContextE.context("EXPRESSION") { m.get("EXPRESSION") match {
						case None => ContextE.error("missing field")
						case Some(jsval) => ContextE.evaluate(jsval)
					}}
				case Some(jsval) =>
					ContextE.error(s"invalid lambda `$name`: $jsval")
				case None =>
					ContextE.error(s"no lambda `$name` in scope")
			}
		} yield res
	}

	def evaluateLet(m: Map[String, JsValue]): ContextE[JsObject] = {
		for {
			_ <- ContextE.context("VAR") { m.get("VAR") match {
				case None => ContextE.unit(())
				case Some(JsArray(l)) =>
					ContextE.foreach(l.zipWithIndex) { case (jsval, i) =>
						ContextE.context(s"[${i+1}]") {
							jsval match {
								case JsObject(m) =>
									m.toList match {
										case (name, jsobj2@JsObject(_)) :: Nil =>
											for {
												value <- ContextE.evaluate(jsobj2)
												_ <- ContextE.addToScope(Map(name -> value))
											} yield ()
										case Nil => ContextE.unit(())
										case _ => ContextE.error("invalid item: "+jsval)
									}
								case _ =>
									ContextE.error("Expected a JsObject: "+jsval)
							}
						}
					}
				case _ =>
					ContextE.error("expected a JsArray")
			}}
			res <- ContextE.context("EXPRESSION") { m.get("EXPRESSION") match {
				case Some(jsval) =>
					ContextE.evaluate(jsval)
				case None =>
					ContextE.error("field missing")
			}}
		} yield res
	}

	def evaluateStringf(format: String): ContextE[JsObject] = {
		println(s"evaluateStringf($format)")
		for {
			s <- StringfParser.parse(format)
		} yield Converter2.makeString(s)
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