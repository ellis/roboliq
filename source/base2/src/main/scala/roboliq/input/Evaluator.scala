package roboliq.input

import scala.collection.mutable.HashMap

class Evaluator() {
	
	def evaluate(rjsval: RjsValue): ResultE[RjsValue] = {
		println(s"evaluate($rjsval)")
		rjsval match {
			case x: RjsBuild => evaluateBuild(x)
			case x: RjsCall => evaluateCall(x)
			case x: RjsDefine => evaluateDefine(x)
			case x: RjsFormat => evaluateFormat(x)
			case x: RjsImport => evaluateImport(x)
			case x: RjsInclude => evaluateInclude(x)
			case x: RjsInstruction => evaluateInstruction(x)
			case x: RjsList => evaluateList(x)
			case x: RjsSection => evaluateSection(x)
			case x: RjsSubst => evaluateSubst(x)
			case x: RjsAbstractMap =>
				x.typ_? match {
					case Some(_) => evaluateTypedMap(x)
					case _ => evaluateMap(x)
				}
			case x => ResultE.unit(x)
		}
	}

	def evaluateBuild(x: RjsBuild): ResultE[RjsValue] = {
		println(s"evaluateBuild($x)")

		val var_m = new HashMap[String, RjsValue]
		
		for {
			output0_l <- ResultE.mapAll(x.item_l.zipWithIndex) { case (item, i) =>
				ResultE.context("item["+(i+1)+"]") {
					item match {
						case x2: RjsBuildItem_VAR =>
							for {
								x3 <- ResultE.evaluate(x2.value)
							} yield {
								var_m(x2.name) = x3
								None
							}
						case x2: RjsBuildItem_ADD =>
							for {
								x3 <- ResultE.evaluate(x2.value)
								res <- x3 match {
									case RjsMap(map) =>
										ResultE.unit(Some(RjsMap(var_m.toMap ++ map)))
									case _ =>
										ResultE.error(s"expected value to evaluate to a map: $x3")
								}
							} yield res
					}
				}
			}
		} yield RjsList(output0_l.flatten)
	}
	
	def evaluateCall(x: RjsCall): ResultE[RjsValue] = {
		println(s"evaluateCall($x)")
		for {
			// Get evaluated input map
			input <- evaluateMap(x.input)
			scope0 <- ResultE.getScope
			// Try to lookup the lambda, and also get the scope it was defined in
			res <- scope0.getWithScope(x.name) match {
				case None =>
					evaluateCallBuiltin(x.name, input)
				case Some((lambda: RjsLambda, scope)) =>
					evaluateCallLambda(x.name, input, lambda, scope)
				case Some((function: RjsFunction, _)) =>
					evaluateCallFunction(x.name, input, function)
				case Some((x2, _)) =>
					ResultE.error(s"expected `${x.name}` to be a lambda")
			}
		} yield res
	}
		
	def evaluateCallBuiltin(name: String, input: RjsMap): ResultE[RjsValue] = {
		println(s"evaluateCallBuiltin($name, $input)")
		name match {
			case "add" =>
				ResultE.withScope(new EvaluatorScope(input)) {
					new BuiltinAdd().evaluate()
				}
			case _ =>
				ResultE.error(s"unknown function `$name`")
		}
	}

	def evaluateCallLambda(name: String, input: RjsMap, lambda: RjsLambda, scope: EvaluatorScope): ResultE[RjsValue] = {
		println(s"evaluateCallLambda($name, $input, $lambda, $scope)")
		ResultE.withScope(scope.add(input)) {
			ResultE.evaluate(lambda.expression)
		}
	}

	def evaluateCallFunction(name: String, input: RjsMap, function: RjsFunction): ResultE[RjsValue] = {
		println(s"evaluateCallFunction($name, $input)")
		function.fn(input)
	}

	/**
	 * Adds the given binding to the scope
	 */
	def evaluateDefine(x: RjsDefine): ResultE[RjsValue] = {
		println(s"evaluateDefine($x)")
		for {
			res <- ResultE.evaluate(x.value)
			_ <- ResultE.addToScope(Map(x.name -> res))
		} yield RjsNull
	}

	def evaluateFormat(x: RjsFormat): ResultE[RjsValue] = {
		println(s"evaluateFormat($x)")
		for {
			s <- StringfParser.parse(x.format)
		} yield RjsText(s)
	}

	/**
	 * Imports bindings from an external file
	 */
	def evaluateImport(x: RjsImport): ResultE[RjsValue] = {
		println(s"evaluateImport($x)")
		val basename = x.name + "-" + x.version
		for {
			file <- ResultE.or(
				ResultE.findFile(basename+".json"),
				ResultE.findFile(basename+".yaml")
			)
			rjsval <- ResultE.loadRjsFromFile(file)
			_ <- ResultE.evaluate(rjsval)
		} yield RjsNull
	}

	/**
	 * Evaluates the content of another file and returns the result
	 */
	def evaluateInclude(x: RjsInclude): ResultE[RjsValue] = {
		println(s"evaluateInclude($x)")
		for {
			file <- ResultE.findFile(x.filename)
			jsfile <- ResultE.loadRjsFromFile(file)
			jsobj <- ResultE.evaluate(jsfile)
		} yield jsobj
	}
	
	def evaluateInstruction(x: RjsInstruction): ResultE[RjsInstruction] = {
		println(s"evaluateInstruction($x)")
		for {
			input <- evaluateMap(x.input)
		} yield {
			RjsInstruction(x.name, input)
		}
	}
	
	def evaluateList(x: RjsList): ResultE[RjsList] = {
		println(s"evaluateList($x)")
		ResultE.mapAll(x.list)(x2 =>
			ResultE.evaluate(x2)
		).map(l => RjsList(l))
	}
	
	def evaluateMap(x: RjsAbstractMap): ResultE[RjsMap] = {
		println(s"evaluateMap($x)")
		ResultE.mapAll(x.getValueMap.toList)({ case (name, xs) =>
			for {
				res <- ResultE.evaluate(xs)
			} yield (name, res)
		}).map(l => RjsMap(l.toMap))
	}

	def evaluateSection(x: RjsSection): ResultE[RjsValue] = {
		println(s"evaluateSection($x)")
		var resFinal: RjsValue = RjsNull
		for {
			// TODO: add warnings about values which evaluate to something other than RjsNull, unless they are the final expression 
			_ <- ResultE.foreach(x.body.zipWithIndex) { case (rjsval0, i) =>
				ResultE.context(s"[${i+1}]") {
					for {
						res <- ResultE.evaluate(rjsval0)
					} yield {
						resFinal = res
					}
				}
			}
		} yield resFinal
	}

	def evaluateSubst(x: RjsSubst): ResultE[RjsValue] = {
		println(s"evaluateSubst($x)")
		ResultE.getScopeValue(x.name)
	}
	
	def evaluateTypedMap(x: RjsAbstractMap): ResultE[RjsValue] = {
		println(s"evaluateTypedMap($x)")
		for {
			rjsval <- RjsValue.evaluateTypedMap(x)
			rjsval2 <- rjsval match {
				case x2: RjsAbstractMap if x.typ_? == x2.typ_? =>
					ResultE.unit(rjsval)
				case _ =>
					evaluate(rjsval)
			}
		} yield rjsval2
	}
}
