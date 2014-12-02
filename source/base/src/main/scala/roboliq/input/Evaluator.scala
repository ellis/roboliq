package roboliq.input

import spray.json.JsObject
import spray.json.JsString
import spray.json.JsNumber
import spray.json.JsValue
import roboliq.entities.EntityBase
import spray.json.JsArray
import scala.collection.mutable.HashMap
import spray.json.JsNull

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
				for {
					typ_? <- ContextE.fromJson[Option[String]](m, "TYPE")
					res <- typ_? match {
						case None =>
							ContextE.unit(JsObject(Map("TYPE" -> JsString("set"), "VALUE" -> jsobj)))
						case Some(typ) =>
							println(s"C: $typ")
							typ match {
								case "call" => evaluateCall(m)
								case "build" => evaluateBuild(m)
								case "define" => evaluateDefine(m)
								case "import" => evaluateImport(m)
								case "include" => evaluateInclude(m)
								case "lambda" => evaluateLambda(m)
								case "let" => evaluateLet(m)
								case "stringf" => evaluateStringf(m)
								case typ => evaluateType(typ, m)
							}
					}
				} yield res
			case JsNumber(n) =>
				ContextE.unit(Converter2.makeNumber(n))
			case JsArray(l0) =>
				for {
					l <- ContextE.mapAll(l0) { item => evaluate(item) }
				} yield {
					// HACK: remove empty JsObjects, because import will create an empty JsObject.  This probably isn't the right thing to do, though.
					val l1 = l.filterNot(_.fields.isEmpty)
					Converter2.makeList(l1)
				}
			// A value
			case _ =>
				ContextE.error("Don't know how to evaluate: "+jsval)
		}
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
	
	def evaluateCall(m: Map[String, JsValue]): ContextE[JsObject] = {
		println(s"evaluateCall($m)")
		for {
			name <- ContextE.fromJson[String](m, "NAME")
			// Get non-evaluated input object
			input0_? <- ContextE.fromJson[Option[JsObject]](m, "INPUT")
			// Get non-evaluated input map 
			input0_m = input0_?.map(_.fields).getOrElse(Map())
			// Get evaluated input map
			input_l <- ContextE.mapAll(input0_m.toList) { case (name, jsval) =>
				for {
					res <- ContextE.evaluate(jsval)
				} yield (name, res)
			}
			input_m = input_l.toMap
			scope0 <- ContextE.getScope
			// Try to lookup the lambda, and also get the scope it was defined in
			res <- scope0.getWithScope(name) match {
				case None =>
					evaluateCallBuiltin(name, input_m)
				case Some((jsobj, scope)) =>
					evaluateCallLambda(name, jsobj, scope, input_m)
			}
		} yield res
	}
		
	def evaluateCallBuiltin(name: String, input_m: Map[String, JsObject]): ContextE[JsObject] = {
		println(s"evaluateCallBuiltin($name, ${input_m})")
		name match {
			case "add" =>
				for {
					_ <- ContextE.modify(_.addToScope(input_m))
					res <- new BuiltinAdd().evaluate()
				} yield res
			case _ =>
				ContextE.error(s"unknown function `$name`")
		}
	}

	def evaluateCallLambda(name: String, jsobj: JsObject, scope: EvaluatorScope, input_m: Map[String, JsObject]): ContextE[JsObject] = {
		println(s"evaluateCallLambda($jsobj, $scope, ${input_m})")
		val m = jsobj.fields
		for {
			_ <- ContextE.assert(m.get("TYPE") == Some(JsString("lambda")), s"cannot call `$name` because it is not a function: $jsobj")
			jsExpression <- ContextE.fromJson[JsValue](m, "EXPRESSION")
			res <- ContextE.evaluate(jsExpression, scope.add(input_m))
		} yield res
	}

	/**
	 * Adds the given binding to the scope
	 */
	def evaluateDefine(m: Map[String, JsValue]): ContextE[JsObject] = {
		println(s"evaluateDefine($m)")
		for {
			name <- ContextE.fromJson[String](m, "NAME")
			jsval <- ContextE.fromJson[JsValue](m, "VALUE")
			res <- ContextE.evaluate(jsval)
			_ <- ContextE.addToScope(Map(name -> res))
		} yield JsObject()
	}

	/**
	 * Imports bindings from an external file
	 */
	def evaluateImport(m: Map[String, JsValue]): ContextE[JsObject] = {
		println(s"evaluateImport($m)")
		(m.get("NAME"), m.get("VERSION")) match {
			case (None, _) =>
				ContextE.error("`NAME` field required")
			case (Some(JsString(name)), Some(JsString(ver))) =>
				val basename = name + "-" + ver
				for {
					file <- ContextE.or(
						ContextE.findFile(basename+".json"),
						ContextE.findFile(basename+".yaml")
					)
					jsfile <- ContextE.loadJsonFromFile(file)
					_ <- ContextE.evaluate(jsfile)
				} yield JsObject()
			case _ =>
				ContextE.error("`NAME` and `VER` expected to be JsString")
		}
	}

	/**
	 * Evaluates the content of another file and returns the result
	 */
	def evaluateInclude(m: Map[String, JsValue]): ContextE[JsObject] = {
		println(s"evaluateInclude($m)")
		m.get("FILENAME") match {
			case None =>
				ContextE.error("`FILENAME` field required")
			case Some(JsString(filename)) =>
				for {
					file <- ContextE.findFile(filename)
					jsfile <- ContextE.loadJsonFromFile(file)
					jsobj <- ContextE.evaluate(jsfile)
				} yield jsobj
			case _ =>
				ContextE.error("`FILENAME` expected to be JsString")
		}
	}

	def evaluateLambda(m: Map[String, JsValue]): ContextE[JsObject] = {
		println(s"evaluateLambda($m)")
		ContextE.unit(JsObject(m))
	}

	def evaluateLet(m: Map[String, JsValue]): ContextE[JsObject] = {
		println(s"evaluateLet($m)")
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

	def evaluateStringf(m: Map[String, JsValue]): ContextE[JsObject] = {
		println(s"evaluateStringf($m)")
		for {
			format <- ContextE.fromJson[String](m, "VALUE")
			s <- StringfParser.parse(format)
		} yield Converter2.makeString(s)
	}

	def evaluateType(typ: String, m: Map[String, JsValue]): ContextE[JsObject] = {
		println(s"evaluateType($typ, $m)")
		for {
			jsval <- ContextE.fromJson[JsValue](m, "VALUE")
			res <- (typ, jsval) match {
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
					ContextE.error(s"evaluateType() not completely implemented: $typ, $jsval")
			}
		} yield res
	}
}