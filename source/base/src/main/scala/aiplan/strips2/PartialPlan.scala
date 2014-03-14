package aiplan.strips2

import Strips._

	
	//sealed trait BindingConstraint
//case class BindingEq(a: String, b: String) extends BindingConstraint
//case class BindingNe(a: String, b: String) extends BindingConstraint
//case class BindingIn(a: String, l: Set[String]) extends BindingConstraint
case class Binding(eq: Option[String], ne: Option[String], in: Set[String])

/**
 * @param option_l Set of objects that the variable may use
 * @param ne_l Set of variables which the variable shall be equal to
 */
case class B(option_l: Set[String] = Set(), ne_l: Set[String] = Set())

/**
 * @param variable_m map of variables to possible object values
 * @param equality_m map of variable to another variable
 * @param assignment_m map of variables to either another variable or the chosen object value
 * @param impossible_m map of impossible variables to description of the problem
 * 
 * Invariants:
 * - A variable with a single possible value is held in assignment_m, not variable_m
 * - A variable with no possible values is held in impossible_m and nowhere else
 * - A variable with multiple possible values is held in variable_m
 * - A variable which is not equal to an object does not have that object in it's options set 
 * - Any variable that is a key for assignment_m or impossible_m, is not a key in any other map
 * - Any variable that is a key for assignment_m or impossible_m, is not a value in any map
 * - When a variable is assigned to another variable, add the equality to assignment_m and remove references to one of the variables from all other maps
 * - If x is in y's inequality set, then y is in x's inequality set:w
 * 
 */
class Bindings(
	variable_m: Map[String, B],
	assignment_m: Map[String, String]
) {
	private def copy(
		variable_m: Map[String, B] = variable_m,
		assignment_m: Map[String, String] = assignment_m
	): Bindings = new Bindings(
		variable_m,
		assignment_m
	)
	
	def getCanonicalName(name: String): String = {
		assignment_m.get(name) match {
			case None => name
			case Some(name2) => getCanonicalName(name2)
		}
	}
		
	/**
	 * Add a variable and it's possible values to the bindings.
	 * This function should be called before that variable is referenced.
	 */
	def addVariable(name: String, option_l: Set[String]): Either[String, Bindings] = {
		// This should only be called once per variable
		assert(!variable_m.contains(name))

		setVariableOptions(name, option_l)
	}
	
	def addVariables(map: Map[String, Set[String]]): Either[String, Bindings] = {
		def step(l: List[(String, Set[String])], acc: Bindings): Either[String, Bindings] = {
			l match {
				case Nil => Right(acc)
				case (name, value_l) :: rest =>
					for {
						acc2 <- acc.addVariable(name, value_l).right
						acc3 <- step(rest, acc2).right
					} yield acc3
			}
		}
		step(map.toList, this)
	}
	
	private def isVariableName(name: String): Boolean = name.contains(':')
	
	private def substitute(name1: String, name2: String): Bindings = {
		assert(isVariableName(name1))
		assert(!assignment_m.contains(name1))
		assert(!assignment_m.contains(name2))
		new Bindings(
			(variable_m - name1).mapValues(b => b.copy(ne_l = b.ne_l.map(s => if (s == name1) name2 else s))),
			assignment_m + (name1 -> name2)
		)
	}
	
	private def setVariableOptions(name: String, option_l: Set[String]): Either[String, Bindings] = {
		assert(isVariableName(name))
		assert(!assignment_m.contains(name))
		
		if (option_l.isEmpty) {
			Left(s"empty set of possible values for $name")
		}
		else if (option_l.size == 1) {
			assign(name, option_l.head)
		}
		else {
			val b = variable_m.getOrElse(name, B())
			Right(copy(
				variable_m = variable_m + (name -> b.copy(option_l = option_l))
			))
		}
	}
	
	def assign(assignee: String, value: String): Either[String, Bindings] = {
		val List(x, y) = List(getCanonicalName(assignee), getCanonicalName(value)).sorted
		if (x == y) return Right(this)
		
		(variable_m.get(x), variable_m.get(y)) match {
			// Both are variables
			case (Some(bX), Some(bY)) =>
				setVariableEquality(x, bX, y, bY)
			// One is a variable and the other is an object
			case (Some(b), None) =>
				Right(substitute(x, y))
			case (None, Some(b)) =>
				Right(substitute(y, x))
			//case (None, None) =>
		}
	}
	
	def assign(map: Map[String, String]): Either[String, Bindings] = {
		def step(l: List[(String, String)], acc: Bindings): Either[String, Bindings] = {
			l match {
				case Nil => Right(acc)
				case (name, value) :: rest =>
					for {
						acc2 <- acc.assign(name, value).right
						acc3 <- step(rest, acc2).right
					} yield acc3
			}
		}
		step(map.toList, this)
	}

	/**
	 * set name1 = name2, replacing references to name1 in assignment and inequality values
	 * When variables are set to be equal, collapse one into the other.
	 * a and b get sorted, and the alphanumerically larger one gets collapsed into the smaller one.
	 */
	private def setVariableEquality(name1: String, b1: B, name2: String, b2: B): Either[String, Bindings] = {
		// Make sure that x and y are not already set to non-equal
		assert(!b1.ne_l.contains(name2))

		// Take intersection of x & y options for x, and remove y entry
		val option_l = b1.option_l intersect b2.option_l
		for {
			bindings1 <- this.setVariableOptions(name2, option_l).right
		} yield {
			val name2b = bindings1.getCanonicalName(name2)
			bindings1.substitute(name1, name2b)
		}
	}

	/*
	/**
	 * Add y inequalities to x inequalities,
	 * Remove y entry,
	 * Replace occurrences of y by x.
	 */
	private def updateInequality1(x: String, y: String): Map[String, Set[String]] = {
		val inequality = inequality1_m.getOrElse(x, Set()) ++ inequality1_m.getOrElse(y, Set())
		val m1 = inequality1_m + (x -> inequality)
		val m2 = m1 - y
		val m3 = m2.mapValues(_.map(name => if (name == y) x else y))
		m3
	}
	*/
	
	/**
	 * For the named variable, exclude the given value as a possible assignment.
	 * The given value may be either an object or another value.
	 * If it is an object, that object is removed from the variable's list of options.
	 * If it is a variable, then ...
	 */
	def exclude(name: String, value: String): Either[String, Bindings] = {
		assert(variable_m.contains(name))
		val b1 = variable_m(name)
		variable_m.get(value) match {
			// Value is an object:
			case None =>
				// If the object is still in the option list:
				if (b1.option_l.contains(value)) {
					// Remove the value from the option list, and set the new option list
					val option_l = b1.option_l - value
					setVariableOptions(name, option_l)
				}
				else {
					Right(this)
				}
			// Value is a variable:
			case Some(b2) =>
				// If the variable is already in the exclusion list:
				if (b1.ne_l.contains(value)) {
					Right(this)
				}
				// Otherwise, add the names to each other's exclusion lists:
				else {
					val b12 = b1.copy(ne_l = b1.ne_l + value)
					val b22 = b2.copy(ne_l = b2.ne_l + name)
					Right(copy(variable_m = variable_m + (name -> b12) + (value -> b22)))
				}
		}
	}
	
	def exclude(map: Map[String, Set[String]]): Either[String, Bindings] = {
		def step(l: List[(String, String)], acc: Bindings): Either[String, Bindings] = {
			l match {
				case Nil => Right(acc)
				case (name, value) :: rest =>
					for {
						acc2 <- acc.exclude(name, value).right
						acc3 <- step(rest, acc2).right
					} yield acc3
			}
		}
		val l = map.toList.flatMap(pair => pair._2.toList.map(pair._1 -> _))
		step(l, this)
	}
}

/**
 * @param map Map from action index to the set of indexes that are ordered after that action.
 */
class Orderings(
	val map: Map[Int, Set[Int]]
) {
	def add(before_i: Int, after_i: Int): Either[String, Orderings] = {
		// Make sure the constraints are not violated.
		// In other words, make sure before_i is not already after after_i.
		if (map.getOrElse(after_i, Set()).contains(before_i))
			Left(s"New ordering constraint violates preexising constraints: ${before_i} -> ${after_i}")
		else {
			
			val before_li = map.getOrElse(before_i, Set()) + before_i
			// 
			val map2 = for (pair@(i, li) <- map) yield {
				// before_i is before after_i
				if (i == before_i)
					i -> (li + after_i)
				// Any index which is before before_i is now also before after_i
				else if (li.contains(i))
					i -> (li + after_i)
				// Leave the others as they were
				else
					pair
			}
			Right(new Orderings(map2))
		}
	}
}


/**
 * Represents a causal link.
 * a and b are indexes of actions, and p is the index of a precondition in b.
 * The proposition should be protected so that no actions are placed between a and b which negate the effect.
 */
case class CausalLink(provider_i: Int, consumer_i: Int, precond_i: Int)

/**
 * @param action_l Set of partially instantiated operators
 * @param ordering_l Ordering constraints
 * @param binding_m Map of variable bindings
 * @param link_l Causal links between actions
 */
class PartialPlan private (
	val action_l: Vector[Operator],
	val orderings: Orderings,
	val bindings: Bindings,
	val link_l: Set[CausalLink],
	val openGoal_l: Set[(Int, Int)]
	//val possibleLink_l: List[(CausalLink, Map[String, String])]
) {
	private def copy(
		action_l: Vector[Operator] = action_l,
		orderings: Orderings = orderings,
		bindings: Bindings = bindings,
		link_l: Set[CausalLink] = link_l,
		openGoal_l: Set[(Int, Int)] = openGoal_l
		//possibleLink_l: List[(CausalLink, Map[String, String])] = possibleLink_l
	): PartialPlan = {
		new PartialPlan(
			action_l,
			orderings,
			bindings,
			link_l = link_l,
			openGoal_l
			//possibleLink_l
		)
	}
	
	/**
	 * Add an action.
	 * This will create unique parameter names for the action's parameters.
	 * The parameters will be added to the bindings using possible values for the given type.
	 */
	def addAction(problem: Problem, op: Operator): Either[String, PartialPlan] = {
		// Create a new action with uniquely numbered parameter names
		val i = action_l.size
		val paramName_l = op.paramName_l.map(s => s"${i-1}:${s}")
		val action = Operator(
			name = op.name,
			paramName_l = paramName_l,
			paramTyp_l = op.paramTyp_l,
			preconds = op.preconds,
			effects = op.effects
		)
		val action2_l: Vector[Operator] = action_l :+ action
		
		// Get list of parameters and their possible objects
		val typeToObjects_m: Map[String, List[String]] =
			problem.object_l.groupBy(_._1).mapValues(_.map(_._2))
		val variableToOptions_m: Map[String, Set[String]] =
			(paramName_l zip op.paramTyp_l).toMap.mapValues(typ => typeToObjects_m.getOrElse(typ, Nil).toSet)
		
		for {
			bindings2 <- bindings.addVariables(variableToOptions_m).right
		} yield {
			val openGoal2_l = openGoal_l ++ action.preconds.l.zipWithIndex.map(i -> _._2)
			copy(
				action_l = action2_l,
				bindings = bindings2,
				openGoal_l = openGoal2_l
			)
		}
	}
	/**
	 * Add an action with the given link and bindings.
	 * This will increase the number of orderings.
	 * It may increase the number of bindings, open goals.
	 * The new bindings may eliminate some of the of possible links.
	 * The action's effects may become possible links for open precondition.
	 * The action's preconditions may become possible links for other action effects. 
	 */
	/*def addAction(op: Operator, link: CausalLink, bindingsNew: Map[String, Binding]): PartialPlan = {
		// Create a new action with uniquely numbered parameter names
		val i = action_l.size
		val paramName_l = op.paramName_l.map(s => s"${i-1}:${s}")
		val action = Operator(
			name = op.name,
			paramName_l = paramName_l,
			paramTyp_l = op.paramTyp_l,
			preconds = op.preconds,
			effects = op.effects
		)
		val action2_l: Vector[Operator] = action_l :+ action
		// Add the action's preconditions to the list of open goals
		//val openGoals2 = openGoals ++ action.preconds
		val ordering2_l = ordering_l + ((link.provider_i, link.consumer_i))
		val bindings2 = bindings ++ bindingsNew
		val openGoal2_l = openGoal_l ++ action.preconds.l.zipWithIndex.map(i -> _._2)
		val possibleLink2_l = possibleLink_l.filter(pair => {
			val (link, map) = pair
			binding2_m
			true
		})
		new PartialPlan(
			action_l = action2_l,
			ordering_l = ordering2_l,
			binding_m = binding2_m,
			link_l = link_l,
			openGoal_l = openGoal2_l,
			possibleLink_l = possibleLink2_l
		)
	}*/
	
	/**
	 * Add an ordering.
	 * This will reduce the number of possible links.
	 */
	def addOrdering(before_i: Int, after_i: Int): Either[String, PartialPlan] = {
		for {
			orderings2 <- orderings.add(before_i, after_i).right
		} yield {
			copy(orderings = orderings2)
		}
	}
	
	def addBindingEq(name: String, value: String): Either[String, PartialPlan] = {
		for {
			bindings2 <- bindings.assign(name, value).right
		} yield {
			copy(bindings = bindings2)
		}
	}
	
	def addBindingNe(name: String, value: String): Either[String, PartialPlan] = {
		for {
			bindings2 <- bindings.exclude(name, value).right
		} yield {
			copy(bindings = bindings2)
		}
	}
	
	/**
	 * Add an causal link.
	 * This may add a new ordering.
	 * The precondition will be removed from the open goals.
	 * Bindings will be added.
	 * The link will be removed from list of possible links.  
	 */
	def addLink(link: CausalLink, eq_m: Map[String, String], ne_m: Map[String, Set[String]]): Either[String, PartialPlan] = {
		for {
			orderings2 <- orderings.add(link.provider_i, link.consumer_i).right
			bindings2 <- bindings.assign(eq_m).right
			bindings3 <- bindings2.exclude(ne_m).right
		} yield {
			copy(
				orderings = orderings2,
				bindings = bindings3,
				link_l = link_l + link,
				openGoal_l = openGoal_l - (link.consumer_i -> link.precond_i)
			)
		}
	}

	def getExistingProviders(consumer_i: Int, precond_i: Int): Unit = {
		val precond = action_l(consumer_i).preconds.l(precond_i)
		
		// Get indexes of actions which may be before consumer_i
		val after_li = orderings.map.getOrElse(consumer_i, Set())
		val before_li = ((0 until action_l.size).toSet -- after_li).toList.sorted

		// If this is a positive preconditions
		if (precond.pos) {
			before_li.flatMap(before_i => {
				val action = action_l(before_i)
				// Search for valid action/poseffect/binding combinations
				val l0 = for ((effect, effect_i) <- action.effects.pos.toList.zipWithIndex if effect.name == precond.atom.name) yield {
					val eq_m = (effect.params zip precond.atom.params).toMap
					bindings.assign(eq_m) match {
						case Left(_) => None
						case Right(_) => Some(CausalLink(before_i, consumer_i, precond_i) -> eq_m)
					}
				}
				l0.flatten
			})
		}
		// Else this is a negative precondition
		else {
			// Search for valid action/negeffect/binding combinations
			// This will involve bindings for the negeffect,
			// but we also need to check that the binding doesn't create a poseffect
			// that negates the negative precondition.
		}
	}
	/*
	
	def getBoundValue(name: String): Binding = {
		//CONTINUE HERE
		binding_m.get(name) match {
			case None => Binding(None, None, Set())
			case Some(binding) =>
				binding.eq match {
					case None => binding
					case Some(s) => getBoundValue(s)
				}
		}
	}
	
	
	def extendBindings(bindings: Map[String, String], sp: Atom, pp: Atom): Option[Map[String, String]] = {
		assert(sp.name == pp.name)
		val l = pp.params zip sp.params
		val valid = l.forall(pair => {
			bindings.get(pair._1) match {
				case None => true
				case Some(x) => x == pair._2
			}
		})
		if (valid) Some(bindings ++ l)
		else None
	}
	*/
	
	/*
	def isThreat(action: Operator, link: CausalLink): Boolean = {
		
	}
	
	def isConsistent(ordering: (Int, Int)): Boolean = {
		null
	}*/
	
	/*def toDot(): String = {
		val header = List[String](
			"rankdir=LR",
			"node [shape=record]"
		)
		val actionLine_l: List[String] = action_l.zipWithIndex.map(pair => {
			val (op, i) = pair
			val name = s"${i-1}:${op.name}"
			val precondText = 
			s"$name|{$precondText|$effectText}"
		})
		l.mkString("digraph partialPlan {\n", ";\n\t", ";\n}")
	}*/
}

object PartialPlan {
	def fromProblem(problem: Problem): PartialPlan = {
		val action0 = Operator(
			name = "__initialState",
			paramName_l = Nil,
			paramTyp_l = Nil,
			preconds = Literals.empty,
			effects = Literals(pos = problem.state0.atoms.toList, neg = Nil)
		)
		val action1 = Operator(
			name = "__finalState",
			paramName_l = Nil,
			paramTyp_l = Nil,
			preconds = problem.goals,
			effects = Literals.empty
		)
		val ordering_l = Set((0, 1))
		//val goals = problem.goals
		val openGoal_l = problem.goals.l.zipWithIndex.map(1 -> _._2).toSet
		new PartialPlan(
			action_l = Vector(action0, action1),
			orderings = new Orderings(Map()),
			bindings = new Bindings(Map(), Map()),
			link_l = Set(),
			openGoal_l
		)
	}
	
	def main(args: Array[String]) {
		for {
			domain <- aiplan.strips2.PddlParser.parseDomain(aiplan.quiz.Quiz2b7.domainText).right
			problem <- aiplan.strips2.PddlParser.parseProblem(domain, aiplan.quiz.Quiz2b7.problemText).right
		} {
			val plan0 = fromProblem(problem)
			println("domain:")
			println(domain)
			println()
			println("problem:")
			println(problem)
			println()
			
			println()
		}
	}
}
