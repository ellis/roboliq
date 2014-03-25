package aiplan.strips2

import Strips._
import scala.collection.mutable.ArrayBuffer

	
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
	val variable_m: Map[String, B],
	val assignment_m: Map[String, String]
) {
	private def copy(
		variable_m: Map[String, B] = variable_m,
		assignment_m: Map[String, String] = assignment_m
	): Bindings = new Bindings(
		variable_m,
		assignment_m
	)
	
	def isVariable(name: String): Boolean = variable_m.contains(name)
	
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
		println(s"setVariableOptions($name, ${option_l})")
		assert(isVariableName(name))
		assert(!assignment_m.contains(name))
		
		if (option_l.isEmpty) {
			Left(s"empty set of possible values for $name")
		}
		else if (option_l.size == 1) {
			// Need to add the variable to the variable map before calling `assign`
			val variable2_m = variable_m + (name -> B(option_l, Set()))
			val bindings2 = copy(variable_m = variable2_m)
			bindings2.assign(name, option_l.head)
		}
		else {
			val b = variable_m.getOrElse(name, B())
			Right(copy(
				variable_m = variable_m + (name -> b.copy(option_l = option_l))
			))
		}
	}
	
	def assign(assignee: String, value: String): Either[String, Bindings] = {
		val name1 = getCanonicalName(assignee)
		val name2 = getCanonicalName(value)
		val List(x, y) = List(name1, name2).sorted
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
			case (None, None) =>
				val a = if (assignee == name1) assignee else s"$assignee(=$name1)"
				val b = if (value == name2) value else s"$value(=$name2)"
				Left(s"Cannot assign an object to another object: $a and $b")
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
	
	def bind(atom: Atom): Atom = {
		atom.copy(params = atom.params.map(s => getCanonicalName(s)))
	}
	
	def bind(lit: Literal): Literal = {
		lit.copy(atom = bind(lit.atom))
	}
	
	def bind(l: Literals): Literals = {
		Literals(l.l.map(bind))
	}
	
	def bind(op: Operator): Operator = {
		Operator(
			name = op.name,
			paramName_l = op.paramName_l.map(s => getCanonicalName(s)),
			paramTyp_l = op.paramTyp_l,
			preconds = bind(op.preconds),
			effects = bind(op.effects)
		)
	}
}

/**
 * A complete map of all orderings.  If A < B < C, then the map will hold
 * {A < B, A < C, B < C}.  
 * @param map Map from action index to the set of indexes that are ordered after that action.
 */
class Orderings(
	val map: Map[Int, Set[Int]]
) {
	assert(map.isEmpty || !getMinimalMap.getOrElse(0, Set()).isEmpty)
	def add(before_i: Int, after_i: Int): Either[String, Orderings] = {
		assert(before_i != after_i)
		// If the mapping is already present, just return this ordering
		if (map.getOrElse(before_i, Set()).contains(after_i))
			Right(this)
		// Make sure the constraints are not violated.
		// In other words, make sure before_i is not already after after_i.
		else if (map.getOrElse(after_i, Set()).contains(before_i))
			Left(s"New ordering constraint violates preexising constraints: ${before_i} -> ${after_i}")
		else {
			// Update set of actions which are after before_i with after_i and all it's afters
			val newset = map.getOrElse(before_i, Set()) ++ map.getOrElse(after_i, Set()) + after_i
			if (newset.contains(before_i))
				return Left(s"New ordering constraint indirectly violates preexising constraints: ${before_i} -> ${after_i}")
			val map2 = scala.collection.mutable.HashMap[Int, Set[Int]](map.toSeq : _*)
			map2(before_i) = newset
			
			/*
			val before_li = map2.keys
			val q = scala.collection.mutable.Queue[(Int, Int)]((before_i, after_i))
			// Get the list of actions which are before before_i
			val beforeBefore_l = map.filter(_._2.contains(before_i)).keys.toList
			q ++= beforeBefore_l.flatMap(i => newset.toList.map(i -> _))
			while (!q.isEmpty) {
				val (before_i, after_i) = q.dequeue
				if (before_i == after_i)
					return Left("Ordering constraint violation")
				
			}
			*/
			
			// Get the list of actions which are before before_i
			val beforeBefore_l = map.filter(_._2.contains(before_i)).keys.toList
			val l = beforeBefore_l.flatMap(i => newset.toList.map(i -> _))
			val x = l.foldLeft(Right(new Orderings(map2.toMap)) : Either[String, Orderings]) { (orderings_?, pair) =>
				orderings_? match {
					case Right(orderings) =>
						orderings.add(pair._1, pair._2)
					case x => return x
				}
			}
			
			println(s"Orderings.add(${before_i}, ${after_i})")
			println("before: "+map)
			x match {
				case Right(y) => println("after:  "+y.map)
				case _ =>
			}
			x
			/*
			// Update orderings map
			for (pair@(i, li) <- map) {
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
			// Make sure the ordering before_i < after_i is in the map
			val map3 = if (map2.contains(before_i)) map2 else map2 + (before_i -> Set(after_i))
			println(s"Orderings.add(${before_i}, ${after_i}) => $map3")
			Right(new Orderings(map3))
			*/
		}
	}
	
	/**
	 * Get an ordering map which removes all implied orderings.
	 */
	def getMinimalMap: Map[Int, Set[Int]] = {
		def step(before_i: Int, after_l: List[Int], acc: Set[Int]): (Int, Set[Int]) = {
			after_l match {
				case Nil => (before_i -> acc)
				case after_i :: rest =>
					val acc2 = acc -- map.getOrElse(after_i, Set())
					step(before_i, rest, acc2)
			}
		}
		//println("before: "+map)
		val x = map.map(pair => step(pair._1, pair._2.toList, pair._2))
		//println("after:  "+x)
		x
	}
}


/**
 * Represents a causal link.
 * a and b are indexes of actions, and p is the index of a precondition in b.
 * The proposition should be protected so that no actions are placed between a and b which negate the effect.
 */
case class CausalLink(provider_i: Int, consumer_i: Int, precond_i: Int)

sealed trait Resolver
case class Resolver_Ordering(before_i: Int, after_i: Int) extends Resolver
case class Resolver_Inequality(name1: String, name2: String) extends Resolver

/**
 * @param problem The problem this plan should solve
 * @param action_l Set of partially instantiated operators
 * @param orderings Ordering constraints
 * @param bindings Binding constraints
 * @param link_l Causal links between actions
 * @param openGoal_l Set of open goals, represented as tuples (action_i, precond_i)
 * @param threat_l Set of threats to causal links
 */
class PartialPlan private (
	val problem: Problem,
	val action_l: Vector[Operator],
	val orderings: Orderings,
	val bindings: Bindings,
	val link_l: Set[CausalLink],
	val openGoal_l: Set[(Int, Int)],
	val threat_l: Set[(Int, CausalLink)]
	//val possibleLink_l: List[(CausalLink, Map[String, String])]
) {
	private def copy(
		action_l: Vector[Operator] = action_l,
		orderings: Orderings = orderings,
		bindings: Bindings = bindings,
		link_l: Set[CausalLink] = link_l,
		openGoal_l: Set[(Int, Int)] = openGoal_l,
		threat_l: Set[(Int, CausalLink)] = threat_l
		//possibleLink_l: List[(CausalLink, Map[String, String])] = possibleLink_l
	): PartialPlan = {
		new PartialPlan(
			problem,
			action_l,
			orderings,
			bindings,
			link_l,
			openGoal_l,
			threat_l
		)
	}
	
	/**
	 * Add an action.
	 * This will create unique parameter names for the action's parameters.
	 * The parameters will be added to the bindings using possible values for the given type.
	 */
	def addAction(op: Operator): Either[String, PartialPlan] = {
		println(s"addAction($op)")
		// Create a new action with uniquely numbered parameter names
		val i = action_l.size
		// Get a list of param name/typ for parameters which are still variables 
		val varNameToTyp_l = (op.paramName_l zip op.paramTyp_l).filter(_._1.startsWith("?"))
		val paramName_m = varNameToTyp_l.map(pair => pair._1 -> s"${i-1}:${pair._1}").toMap
		val action = op.bind(paramName_m)
		val action2_l: Vector[Operator] = action_l :+ action
		
		// Get list of parameters and their possible objects
		val variableToOptions_m: Map[String, Set[String]] =
			varNameToTyp_l.map(pair => {
				val (name0, typ) = pair
				val name = paramName_m.getOrElse(name0, name0)
				val options = problem.typToObjects_m.getOrElse(typ, Nil).toSet
				name -> options
			}).toMap
		
		println("variableToOptions_m: "+variableToOptions_m)
		for {
			orderings1 <- orderings.add(0, i).right
			orderings2 <- orderings1.add(i, 1).right
			bindings2 <- bindings.addVariables(variableToOptions_m).right
		} yield {
			println("orderings1: "+orderings1.map)
			println("orderings2: "+orderings2.map)
			val openGoal2_l = openGoal_l ++ action.preconds.l.zipWithIndex.map(i -> _._2)
			copy(
				action_l = action2_l,
				orderings = orderings2,
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

	private def createEffectToPrecondMap(effect: Literal, precond: Literal): Either[String, Map[String, String]] = {
		val l1 = effect.atom.params zip precond.atom.params
		val m1 = l1.groupBy(_._1).mapValues(_.map(_._2))
		def step(l: List[(String, Seq[String])], acc: Map[String, String]): Either[String, Map[String, String]] = {
			l match {
				case Nil => Right(acc)
				case (key, value :: Nil) :: rest => step(rest, acc + (key -> value))
				case (key, value_l) :: rest => Left(s"would need to map $key to multiple values: "+value_l.mkString(","))
			}
		}
		val x = step(m1.toList, Map())
		println(s"createEffectToPrecondMap($effect, $precond) = $x")
		x
	}
	
	private def getProvidersFromList(
		provider_l: List[(Option[Int], Operator)],
		consumer_i: Int,
		precond_i: Int
	): List[(Either[Operator, Int], Map[String, String])] = {
		val precond = action_l(consumer_i).preconds.l(precond_i)
		
		val l = provider_l.flatMap(pair => {
			val (before_i_?, action) = pair
			// Search for valid action/poseffect/binding combinations
			val l0 = for ((effect, effect_i) <- action.effects.l.zipWithIndex if effect.atom.name == precond.atom.name && effect.pos == precond.pos) yield {
				//println("action.effects: "+action.effects.l)
				for {
					res <- {
						if (before_i_?.isEmpty) {
							addAction(action) match {
								case Left(msg) => println("addAction error: "+msg); None
								case Right(plan2) => Some((plan2, plan2.action_l.last))
							}
						}
						else {
							Some((this, action))
						}
					}
					(plan2, action2) = res
					//_ = println("action2: "+action2)
					//_ = println("action2.effects: "+action2.effects.l)
					effect2 = action2.effects.l(effect_i)
					//_ = println("effect2: "+effect2)
					eq_m <- createEffectToPrecondMap(effect2, precond) match {
						case Left(msg) => None
						case Right(x) => Some(x)
					}
					//_ = println("eq_m: "+eq_m)
					bindings2 <- plan2.bindings.assign(eq_m) match {
						case Left(msg) =>
							println("assign error: "+msg)
							None
						case Right(x) => Some(x)
					}
					// Check that the assignment doesn't create a positive effect that negates the precondition
					_ <- {
						if (precond.pos) {
							Some(())
						}
						else {
							val pos_l = action.effects.pos.map(bindings2.bind)
							if (pos_l.contains(precond.atom)) None
							else Some(())
						}
					}
				} yield {
					// FIXME: also need to find which values need to be excluded!
					before_i_? match {
						case Some(before_i) => Right(before_i) -> eq_m
						case None => Left(action) -> eq_m
					}
				}
			}
			l0.flatten
		})
		l
	}
	
	def getExistingProviders(consumer_i: Int, precond_i: Int): List[(Either[Operator, Int], Map[String, String])] = {
		val precond = action_l(consumer_i).preconds.l(precond_i)
		// Get indexes of actions which may be before consumer_i
		val after_li = orderings.map.getOrElse(consumer_i, Set())
		val before_li = ((0 until action_l.size).toSet -- after_li - consumer_i).toList.sorted
		val provider_l = before_li.map(i => Some(i) -> action_l(i))
		getProvidersFromList(provider_l, consumer_i, precond_i)
	}
	
	def getNewProviders(consumer_i: Int, precond_i: Int): List[(Either[Operator, Int], Map[String, String])] = {
		val precond = action_l(consumer_i).preconds.l(precond_i)
		val op_l = problem.domain.operator_l.filter(op => op.effects.l.exists(effect => effect.atom.name == precond.atom.name))
		val provider_l = op_l.map(op => None -> op)
		getProvidersFromList(provider_l, consumer_i, precond_i)
	}
	
	// TODO: It'd be better to calculate the threats incrementally,
	// but that'll be complicated and take me some time to figure out
	// how to do correctly.
	def findThreats(): Set[(Int, CausalLink)] = {
		val l = for {
			action_i <- (0 until action_l.size).toList
			link <- link_l
			res <- if (isThreat(action_i, link)) List(action_i -> link) else Nil
		} yield res
		l.toSet
	}
	
	// REFACTOR: this function duplicates a lot of the code in isThreat
	def getResolvers(action_i: Int, link: CausalLink): List[Resolver] = {
		// See whether we can resolve using ordering constraints 
		val lt_? = orderings.add(action_i, link.provider_i) match {
			case Right(x) => Some(Resolver_Ordering(action_i, link.provider_i))
			case _ => None
		}
		val gt_? = orderings.add(link.consumer_i, action_i) match {
			case Right(x) => Some(Resolver_Ordering(link.consumer_i, action_i))
			case _ => None
		}
		// Try to resolve with bindings
		val precond = action_l(link.consumer_i).preconds.l(link.precond_i)
		val neg = !precond
		val action = action_l(action_i)
		// Find which of the actions effects represent threats to the link
		val effect0_l = action.effects.l.filter(effect => effect.pos != precond.pos && effect.atom.name == precond.atom.name)
		val effect_l = effect0_l.filter(effect => {
			val isThreat_? = for {
				eq_m <- createEffectToPrecondMap(effect, precond).right
				bindings2 <- bindings.assign(eq_m).right
			} yield {
				val action2 = bindings2.bind(action)
				if (precond.pos) {
					if (action2.effects.l.contains(neg) && !action2.effects.l.contains(precond))
						true
					else
						false
				}
				else if (action2.effects.l.contains(neg))
					true
				else
					false
			}
			isThreat_? == Right(true)
		})
		val ne_l = effect_l.toList.flatMap(effect => {
			// Get list of not-equal mappings involving one or more variables
			val ne_l = (effect.atom.params zip precond.atom.params).filter(pair => bindings.isVariable(pair._1) || bindings.isVariable(pair._2))
			// Find which ones are consistent with our bindings
			ne_l.filter(pair => bindings.exclude(pair._1, pair._2).isRight).map(pair => Some(Resolver_Inequality(pair._1, pair._2)))
		})
		
		(lt_? :: gt_? :: ne_l).flatten
	}
	
	private def isThreat(action_i: Int, link: CausalLink): Boolean = {
		// FIXME: for debug only
		val test = action_i == 4 && link == CausalLink(3,2,5)
		if (test)
			()
		// ENDFIX
		//println(s"isThreat(${action_i}, $link)")
		// The actions of a link are not threats to the link
		if (action_i == link.provider_i || action_i == link.consumer_i)
			return false
			
		// Get precondition
		val precond = action_l(link.consumer_i).preconds.l(link.precond_i)
		// Get list of effect which might negate the precondition
		val action = action_l(action_i)
		val effect_l = action.effects.l.filter(effect => effect.pos != precond.pos && effect.atom.name == precond.atom.name)

		// If there are no potentially conflicting effects, then the action isn't a threat
		if (effect_l.isEmpty)
			return false

		println(s"isThreat(${action_i}, $link)")
		// Check whether action_i can be ordered between the provider and consumer
		(for {
			orderings1 <- orderings.add(link.provider_i, action_i).right
			orderings2 <- orderings1.add(action_i, link.consumer_i).right
		} yield {
			()
		}) match {
			// If the ordering isn't valid, then the action isn't a threat
			case Left(_) => return false
			case _ =>
		}
		
		// Check whether any of the effects negate the precondition
		for (effect <- effect_l) {
			for {
				eq_m <- createEffectToPrecondMap(effect, precond).right
				bindings2 <- bindings.assign(eq_m).right
			} {
				val precond2 = bindings2.bind(precond)
				// Negated precondition
				val neg = !precond2
				val action2 = bindings2.bind(action)
				// FIXME: for debug only
				//if (test) {
					//println(s"action2: ${action2}")
					//println(s"precond2: $precond2")
					//println(s"neg: $neg")
					//println(s"action2.effects.l: ${action2.effects.l}")
				//}
				// ENDFIX
				if (precond2.pos) {
					if (action2.effects.l.contains(neg)) {
						if (!action2.effects.l.contains(precond2))
							return true
					}
				}
				else {
					if (action2.effects.l.contains(neg))
						return true
				}
			}
		}
		
		false
	}
	
	def getActionText(action_i: Int): String = {
		val op0 = action_l(action_i)
		val i = action_i
		// substitute assigned values for parameters
		val op = bindings.bind(op0)
		val name = s"${i}:${op.name}"
		(name :: op.paramName_l).mkString(" ")
	}
	
	def toDot(): String = {
		// TODOS:
		// - remove """[0-9]+:""" from parameter names
		val header_l = List[String](
			"rankdir=LR",
			"node [shape=plaintext]"
		)
		val actionLine_l: List[String] = action_l.toList.zipWithIndex.map(pair => {
			val (op0, i) = pair
			// substitute assigned values for parameters
			val op = bindings.bind(op0)
			val name = s"${i-1}:${op.name}"
			// color unbound variables red
			val param_l = op.paramName_l.map(s => if (s.contains(":")) s"""<font color="red">$s</font>""" else s)
			val header = (name :: param_l).mkString(" ")
			val preconds_l = op.preconds.l.toList.zipWithIndex.map(pair => {
				// color whether preconditions are supported
				val color = if (openGoal_l.contains((i, pair._2))) "#ff8080" else "#80ff80"
				// create "ports" for preconditions so that causal links can be drawn directly to them
				s"""<td port="${pair._2}" bgcolor="$color">${pair._1.toString}</td>"""
			})
			val preconds = if (preconds_l.isEmpty) "" else preconds_l.mkString("""<table border="0"><tr>""", "</tr><tr>", "</tr></table>")
			val effects = op.effects.l.toList.map(_.toString).mkString("<br/>")
			s"""action$i [label=<<table border="0" cellborder="1"><tr><td colspan="2">$header</td></tr><tr><td>$preconds</td><td>$effects</td></tr></table>>]"""
		})
		val orderLine_l: List[String] = orderings.getMinimalMap.toList.flatMap(pair => {
			val (before_i, after_l) = pair
			after_l.map(after_i => s"""action${before_i} -> action${after_i}""")
		})
		// draw dotted lines for causal links
		val linkLine_l: List[String] = link_l.toList.map(link => {
			s"""action${link.provider_i} -> action${link.consumer_i}:${link.precond_i} [style=dotted arrowhead=diamond]"""
		})
		(header_l ++ actionLine_l ++ orderLine_l ++ linkLine_l).mkString("digraph partialPlan {\n\t", ";\n\t", ";\n}")
	}
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
		//val goals = problem.goals
		val openGoal_l = problem.goals.l.zipWithIndex.map(1 -> _._2).toSet
		new PartialPlan(
			problem = problem,
			action_l = Vector(action0, action1),
			orderings = new Orderings(Map(0 -> Set(1))),
			bindings = new Bindings(Map(), Map()),
			link_l = Set(),
			openGoal_l = openGoal_l,
			threat_l = Set()
		)
	}
	
	val domainText0 = """
(define (domain random-domain)
  (:requirements :strips)
  (:action op1
    :parameters (?x)
    :precondition (and (S B))
    :effect (and (R ?x) (not (S B)))
  )
)
"""
		
	val problemText0 = """
(define (problem random-pbl1)
  (:domain random-domain)
  (:init
     (S A) (S B)
  )
  (:goal (and (R A) (R B))))
"""
	
	def main0(args: Array[String]) {
		val res = for {
			domain <- aiplan.strips2.PddlParser.parseDomain(domainText0).right
			problem <- aiplan.strips2.PddlParser.parseProblem(domain, problemText0).right
		} yield {
			val plan0 = fromProblem(problem)
			println("domain:")
			println(domain)
			println()
			println("problem:")
			println(problem)
			println()
			println(plan0.toDot)
			println()
			println(plan0.getExistingProviders(1, 0))
			println(plan0.getNewProviders(1, 0))
			println()
			Pop.pop(plan0) match {
				case Left(msg) => println("ERROR: "+msg)
				case Right(plan1) =>
					val dot = plan1.toDot
					println(dot)
				roboliq.utils.FileUtils.writeToFile("test.dot", dot)
			}
		}
		res match {
			case Left(msg) => println("ERROR: "+msg)
			case _ =>
		}
	}
	
	val domainText = """
(define (domain tecan)
  (:requirements :strips :typing)
  (:types
    labware
    model
    site
    siteModel

    tecan

    pipetter
    pipetterProgram
  )
  (:predicates
    (agent-has-device ?agent ?device)
    (device-can-site ?device ?site)
    (location ?labware ?site)
	(model ?labware ?model)
    (stackable ?sm - siteModel ?m - model)
  )
  (:action moveLabware
    :parameters (?labware - labware ?site1 - site ?site2 - site)
    :precondition (and (location ?labware ?site1))
    :effect (and (not (location ?labware ?site1)) (location ?labware ?site2))
  )
  (:action tecan_pipette1
    :parameters (?a - tecan ?d - pipetter ?p - pipetterProgram ?l1 - labware ?m1 - model ?s1 - site ?sm1 - siteModel)
    :precondition (and
      (agent-has-device ?a ?d)
      (device-can-site ?d ?s1)
      (model ?s1 ?sm1)
      (stackable ?sm1 ?m1)
      (model ?l1 ?m1)
      (location ?l1 ?s1)
    )
    :effect ()
  )
)
"""
		
	val problemText = """
(define (problem random-pbl1)
  (:domain random-domain)
  (:objects
    r1 - tecan
    r1_pipetter - pipetter
    m001 - model
    sm001 - siteModel
    r1 - tecan
    siteA - site
    siteB - site
	plateA - labware
	plateB - labware
    prog001 - pipetterProgram
  )
  (:init
    (location plateA siteA)
    (agent-has-device r1 r1_pipetter)
    (model plateA m001)
    (device-can-site r1_pipetter siteB)
    (model siteA sm001)
    (model siteB sm001)
    (stackable sm001 m001)
  )
  (:goal (and (location plateA siteA)))
)
"""
	
	def main(args: Array[String]) {
		val res = for {
			domain <- aiplan.strips2.PddlParser.parseDomain(domainText).right
			problem <- aiplan.strips2.PddlParser.parseProblem(domain, problemText).right
			_ <- Right(println("typToObjects_m: "+ problem.typToObjects_m)).right
			plan0 <- Right(fromProblem(problem)).right
			//op <- domain.getOperator("moveLabware").right
			op0 <- domain.getOperator("tecan_pipette1").right
			op <- Right(op0.bind(Map("?a" -> "r1", "?d" -> "r1_pipetter", "?p" -> "prog001", "?l1" -> "plateA"))).right
			plan1 <- plan0.addAction(op).right
		} yield {
			println("domain:")
			println(domain)
			println()
			println("problem:")
			println(problem)
			println()
			println(plan0.toDot)
			println()
			println(plan1.toDot)
			println()
			//println(plan0.getExistingProviders(1, 0))
			//println(plan0.getNewProviders(1, 0))
			//println()
			val step0 = PopState_SelectGoal(plan1, 0)
			/*for {
				step1 <- Pop.step(step0).right
				step2 <- Pop.step(step1).right
				step3 <- Pop.step(step2).right
				step4 <- Pop.step(step3).right
				step5 <- Pop.step(step4).right
				step6 <- Pop.step(step5).right
			} yield {
				()
			}
			*/
			Pop.stepToEnd(step0) match {
				case Left(msg) => println("ERROR: "+msg)
				case Right(plan1) =>
					val dot = plan1.toDot
					println(dot)
				roboliq.utils.FileUtils.writeToFile("test.dot", dot)
			}
			/*Pop.pop(plan1) match {
				case Left(msg) => println("ERROR: "+msg)
				case Right(plan1) =>
					val dot = plan1.toDot
					println(dot)
				roboliq.utils.FileUtils.writeToFile("test.dot", dot)
			}*/
		}
		res match {
			case Left(msg) => println("ERROR: "+msg)
			case _ =>
		}
	}
}
