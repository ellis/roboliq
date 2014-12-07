package roboliq.input

import roboliq.entities.EntityBase
import scala.collection.mutable.HashMap

class Evaluator() {
	
	def evaluate(rjsval: RjsValue): ContextE[RjsValue] = {
		println(s"evaluate($rjsval)")
		rjsval match {
			case x: RjsBuild => evaluateBuild(x)
			case x: RjsCall => evaluateCall(x)
			case x: RjsDefine => evaluateDefine(x)
			case x: RjsFormat => evaluateFormat(x)
			case x: RjsImport => evaluateImport(x)
			case x: RjsInclude => evaluateInclude(x)
			case x: RjsList => evaluateList(x)
			case x: RjsMap => evaluateMap(x)
			case x: RjsSection => evaluateSection(x)
			case x: RjsSubst => evaluateSubst(x)
			case x => ContextE.unit(x)
		}
	}

	def evaluateBuild(x: RjsBuild): ContextE[RjsValue] = {
		println(s"evaluateBuild($x)")

		val var_m = new HashMap[String, RjsValue]
		
		for {
			output0_l <- ContextE.mapAll(x.item_l.zipWithIndex) { case (item, i) =>
				ContextE.context("item["+(i+1)+"]") {
					item match {
						case x2: RjsBuildItem_VAR =>
							for {
								x3 <- ContextE.evaluate(x2.value)
							} yield {
								var_m(x2.name) = x3
								None
							}
						case x2: RjsBuildItem_ADD =>
							for {
								x3 <- ContextE.evaluate(x2.value)
								res <- x3 match {
									case RjsMap(map) =>
										ContextE.unit(Some(RjsMap(var_m.toMap ++ map)))
									case _ =>
										ContextE.error(s"expected value to evaluate to a map: $x3")
								}
							} yield res
					}
				}
			}
		} yield RjsList(output0_l.flatten)
	}
	
	def evaluateCall(x: RjsCall): ContextE[RjsValue] = {
		println(s"evaluateCall($x)")
		for {
			// Get evaluated input map
			input <- evaluateMap(x.input)
			scope0 <- ContextE.getScope
			// Try to lookup the lambda, and also get the scope it was defined in
			res <- scope0.getWithScope(x.name) match {
				case None =>
					evaluateCallBuiltin(x.name, input)
				case Some((lambda: RjsLambda, scope)) =>
					evaluateCallLambda(x.name, input, lambda, scope)
				case Some((x2, _)) =>
					ContextE.error(s"expected `${x.name}` to be a lambda")
			}
		} yield res
	}
		
	def evaluateCallBuiltin(name: String, input: RjsMap): ContextE[RjsValue] = {
		println(s"evaluateCallBuiltin($name, $input)")
		name match {
			case "add" =>
				ContextE.withScope(new EvaluatorScope(input)) {
					new BuiltinAdd().evaluate()
				}
			case _ =>
				ContextE.error(s"unknown function `$name`")
		}
	}

	def evaluateCallLambda(name: String, input: RjsMap, lambda: RjsLambda, scope: EvaluatorScope): ContextE[RjsValue] = {
		println(s"evaluateCallLambda($name, $input, $lambda, $scope)")
		ContextE.withScope(scope.add(input)) {
			ContextE.evaluate(lambda.expression)
		}
	}

	/**
	 * Adds the given binding to the scope
	 */
	def evaluateDefine(x: RjsDefine): ContextE[RjsValue] = {
		println(s"evaluateDefine($x)")
		for {
			res <- ContextE.evaluate(x.value)
			_ <- ContextE.addToScope(Map(x.name -> res))
		} yield RjsNull
	}

	def evaluateFormat(x: RjsFormat): ContextE[RjsValue] = {
		println(s"evaluateFormat($x)")
		for {
			s <- StringfParser.parse(x.format)
		} yield RjsText(s)
	}

	/**
	 * Imports bindings from an external file
	 */
	def evaluateImport(x: RjsImport): ContextE[RjsValue] = {
		println(s"evaluateImport($x)")
		val basename = x.name + "-" + x.version
		for {
			file <- ContextE.or(
				ContextE.findFile(basename+".json"),
				ContextE.findFile(basename+".yaml")
			)
			_ = println("A")
			rjsval <- ContextE.loadJsonFromFile(file)
			_ = println("B")
			_ <- ContextE.evaluate(rjsval)
			_ = println("C")
		} yield RjsNull
	}

	/**
	 * Evaluates the content of another file and returns the result
	 */
	def evaluateInclude(x: RjsInclude): ContextE[RjsValue] = {
		println(s"evaluateInclude($x)")
		for {
			file <- ContextE.findFile(x.filename)
			jsfile <- ContextE.loadJsonFromFile(file)
			jsobj <- ContextE.evaluate(jsfile)
		} yield jsobj
	}
	
	def evaluateList(x: RjsList): ContextE[RjsList] = {
		println(s"evaluateList($x)")
		ContextE.mapAll(x.list)(x2 =>
			ContextE.evaluate(x2)
		).map(l => RjsList(l))
	}
	
	def evaluateMap(x: RjsMap): ContextE[RjsMap] = {
		println(s"evaluateMap($x)")
		ContextE.mapAll(x.map.toList)({ case (name, xs) =>
			for {
				res <- ContextE.evaluate(xs)
			} yield (name, res)
		}).map(l => RjsMap(l.toMap))
	}

	def evaluateSection(x: RjsSection): ContextE[RjsValue] = {
		println(s"evaluateSection($x)")
		var resFinal: RjsValue = RjsNull
		for {
			// TODO: add warnings about values which evaluate to something other than RjsNull, unless they are the final expression 
			_ <- ContextE.foreach(x.body.zipWithIndex) { case (rjsval0, i) =>
				ContextE.context(s"[${i+1}]") {
					for {
						res <- ContextE.evaluate(rjsval0)
					} yield {
						resFinal = res
					}
				}
			}
		} yield resFinal
	}

	def evaluateSubst(x: RjsSubst): ContextE[RjsValue] = {
		println(s"evaluateSubst($x)")
		ContextE.getScopeValue(x.name)
	}
}
