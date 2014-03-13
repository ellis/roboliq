package aiplan.strips2

import scala.language.implicitConversions
import scala.collection.mutable
import grizzled.slf4j.Logger
import scalaz._
import Scalaz._

object Strips {
	type Typ = String
	type Call = List[String]
	
	class Signature(val name: String, val paramName_l: List[String], val paramTyp_l: List[String]) {
		assert(paramTyp_l.length == paramName_l.length)
		
		def getSignatureString = name + (paramName_l zip paramTyp_l).map(pair => s"${pair._1}:${pair._2}").mkString("(", " ", ")")
		override def toString = getSignatureString
	}
	
	object Signature {
		def apply(s: String): Signature = {
			val l = s.split(" ").toList
			val name = l.head
			val (paramName_l, paramTyp_l) = l.tail.map(s => {
				val pair = s.span(_ != ':')
				pair.copy(_2 = pair._2.tail)
			}).unzip
			new Signature(name, paramName_l, paramTyp_l)
		}
		implicit def stringToSignature(s: String): Signature =
			apply(s)
	}
	
	case class Atom(name: String, params: Seq[String]) {
		override def toString = (name :: params.toList).mkString(" ")
	}
	
	object Atom {
		def apply(s: String): Atom = {
			val l = s.split(" ")
			new Atom(l.head, l.tail)
		}
		implicit def stringToAtom(s: String): Atom = apply(s)
	}
	
	case class Literal(atom: Atom, pos: Boolean) {
		def !(literal: Literal): Literal = literal.copy(pos = !literal.pos)
		override def toString = (if (pos) "" else "!") ++ atom.toString
	}
	
	/*object Literal {
		def apply(s: String, pos: Boolean = true): Literal = {
			Literal(Atom.apply(s), pos)
		}
		implicit def stringToLiteral(s: String): Literal = apply(s, true)
	}*/
	
	class Literals private (val l: Unique[Literal], val pos: Set[Atom], val neg: Set[Atom]) {
		//def ++(that: Literals) = Literals(pos ++ that.pos, neg ++ that.neg)
		def removePos(atom: Atom): Literals = {
			val lit = Literal(atom, true)
			new Literals(l - lit, pos - atom, neg)
		}
		def removeNeg(atom: Atom): Literals = {
			val lit = Literal(atom, false)
			new Literals(l - lit, pos, neg - atom)
		}
		def removeNegs(atom_l: Set[Atom]): Literals = {
			val lit_l = atom_l.map(atom => Literal(atom, false))
			new Literals(l -- lit_l, pos, neg -- atom_l)
		}
		/**
		 * Remove all negative literals
		 */
		def removeNegs(): Literals = {
			val l2 = l.filter(_.pos)
			new Literals(l2, pos, Set())
		}
		
		def ++(that: Literals): Literals =
			new Literals(l ++ that.l, pos ++ that.pos, neg ++ that.neg)
	}
	
	object Literals {
		val empty = new Literals(Unique(), Unique(), Unique())
		def apply(l: Unique[Literal]): Literals = {
			val pos: Unique[Atom] = l.collect { case Literal(atom, true) => atom }
			val neg: Unique[Atom] = l.collect { case Literal(atom, false) => atom }
			new Literals(l, pos, neg)
		}
		def apply(pos: List[Atom], neg: List[Atom]): Literals = {
			val pos1 = pos.map(atom => Literal(atom, true))
			val neg1 = neg.map(atom => Literal(atom, false))
			val l = Unique((pos1 ++ neg1) : _*)
			new Literals(l, pos.toSet, neg.toSet)
		}
	}
	
	case class State(atoms: Set[Atom]) {
		def holds(atom: Atom): Boolean = atoms.contains(atom)
		def satisfied(literals: Literals): Boolean = {
			literals.pos.forall(atoms.contains) && literals.neg.forall(atom => !atoms.contains(atom))
		}
	}
	
	class Operator private(
		override val name: String,
		override val paramName_l: List[String],
		override val paramTyp_l: List[String],
		val preconds: Literals,
		val effects: Literals
	) extends Signature(name, paramName_l, paramTyp_l) {
		// We need to assert that non of the positive effects are in effects_-,
		// or else we get problems with backwards search.
		assert(effects.neg.intersect(effects.pos).isEmpty)

		def isApplicableIn(state: State): Boolean = {
			preconds.pos.subsetOf(state.atoms) && preconds.neg.intersect(state.atoms).isEmpty
		}
		
		def isRelevantFor(goals: Literals): Boolean = {
			// If the actions effects contribute to the goal
			val contribute = goals.pos.exists(effects.pos.contains) || goals.neg.exists(effects.neg.contains)
			// If the actions effects conflict with the goal
			val noConflict = goals.pos.intersect(effects.neg).isEmpty && goals.neg.intersect(effects.pos).isEmpty
			contribute && noConflict
		}
		
		def transition(state: State): State = {
			val atoms2 = (state.atoms -- effects.neg) ++ effects.pos
			State(atoms2)
		}

		def getOperatorString = {
			getSignatureString + "\n  preconds: " + preconds.toString + "\n  effects: " + effects.toString
		}
	}
	
	object Operator {
		def apply(
			name: String,
			paramName_l: List[String],
			paramTyp_l: List[String],
			preconds: Literals,
			effects: Literals
		): Operator = {
			// We need to make sure that none of the positive effects are in effects_-,
			// or else we get problems with backwards search.
			val effects2 = effects.removeNegs(effects.pos)
			new Operator(
				name, paramName_l, paramTyp_l, preconds, effects2
			)
		}

		implicit def tupleToOperator(tuple: (String, List[String], List[String])): Operator = {
			val sig = Signature.stringToSignature(tuple._1)
			val preconds_+ = tuple._2.filterNot(_.startsWith("!")).map(Atom.stringToAtom)
			val preconds_- = tuple._2.filter(_.startsWith("!")).map(s => Atom.stringToAtom(s.tail))
			val effects_+ = tuple._3.filterNot(_.startsWith("!")).map(Atom.stringToAtom)
			val effects_- = tuple._3.filter(_.startsWith("!")).map(s => Atom.stringToAtom(s.tail))
			new Operator(
				sig.name,
				sig.paramName_l,
				sig.paramTyp_l,
				Literals(preconds_+, preconds_-),
				Literals(effects_+, effects_-)
			)
		}
	}
	
	case class Domain(
		type_l: Set[String],
		constantToType_m: Map[String, String],
		predicate_l: List[Signature],
		operator_l: List[Operator]
	) {
		private val logger = Logger[this.type]
		
		val nameToPredicate_m = predicate_l.map(x => x.name -> x).toMap
		val nameToOperator_m = operator_l.map(x => x.name -> x).toMap

		def makeAction(name: String, param_l: List[String]): Operator = {
			val op = nameToOperator_m(name)
			assert(op.paramName_l.length == param_l.length)

			val bindings = (op.paramName_l zip param_l).toMap
			/*def bind(atom_l: Set[Atom]): Set[Atom] = {
				atom_l.map(atom => {
					val params2 = atom.params.map(s => bindings.getOrElse(s, s))
					atom.copy(params = params2)
				})
			}*/
			def bind(lits: Literals): Literals = {
				val l0 = lits.l
				val l1 = lits.l.map { lit =>
					val params2 = lit.atom.params.map(s => bindings.getOrElse(s, s))
					Literal(lit.atom.copy(params = params2), lit.pos)
				}
				Literals(l1)
			}
			
			Operator(
				name = op.name,
				paramName_l = param_l,
				paramTyp_l = op.paramTyp_l,
				preconds = bind(op.preconds),
				effects = bind(op.effects)
			)
		}
	
		def getApplicableActions(
			s: State
		): List[Operator] =
			operator_l.flatMap(op => addApplicables(Nil, op, op.preconds, Map(), s))
	
		def getApplicableActions(
			s: State,
			op: Operator
		): List[Operator] =
			addApplicables(Nil, op, op.preconds, Map(), s)
	
		private def addApplicables(
			acc: List[Operator],
			op: Operator,
			preconds: Literals,
			σ: Map[String, String],
			s: State
		): List[Operator] = {
			//logger.debug(acc, op, preconds, σ, s)
			if (preconds.pos.isEmpty) {
				val valid = preconds.neg.forall(np => {
					val atom = substitute(np, σ)
					!s.holds(atom)
				})
				if (valid) {
					val action = substitute(op, σ)
					val acc2 = if (acc.contains(action)) acc else acc ++ List(action)
					acc2
				}
				else
					acc
			}
			else {
				//val pp = precs_+.chooseOne()
				val pp = preconds.pos.head
				//logger.debug("pp: "++pp.toString)
				s.atoms.toList.filter(_.name == pp.name).foldLeft(acc) { (acc, sp) =>
					extendBindings(σ, sp, pp) match {
						case Some(σ2) =>
							val preconds2 = preconds.removeNeg(pp)
							addApplicables(acc, op, preconds2, σ2, s)
						case _ => acc
					}
				}
			}
		}
		
		def getRelevantActions(
			op: Operator,
			goals: Literals,
			objects: Set[String]
		): List[Operator] = {
			addRelevants(Nil, op, op.effects, Map(), goals, Some(objects))
		}
	
		def getRelevantPartialActions(
			op: Operator,
			goals: Literals
		): List[Operator] =
			addRelevants(Nil, op, op.effects, Map(), goals, None)
	
		private def addRelevants(
			acc: List[Operator],
			op: Operator,
			effects: Literals,
			σ: Map[String, String],
			goals: Literals,
			objects_? : Option[Set[String]]
		): List[Operator] = {
			logger.debug(acc, op, effects, σ, goals)
			// If the actions effects contribute to the goal
			val contributingEffects = goals.pos.exists(op.effects.pos.contains) || goals.neg.exists(op.effects.neg.contains)
			val seen_l = mutable.Set[String]()
			for {
				pos <- List(true, false)
				// get an open goal
				goal <- if (pos) goals.pos else goals.neg
				// get a relevant effect, if there is one
				effect <- (if (pos) op.effects.pos else op.effects.neg).toList.filter(effect => effect.name == goal.name)
				// see whether effects can be unified with this goal
				partialBindings <- extendBindings(σ, goal, effect).toList
				bindings <- getGroundBindings(partialBindings, op, objects_?)
				// Create the action
				action = substitute(op, bindings)
				_ <- if (seen_l.contains(action.toString)) Nil else List(1)
				_ <- if (action.isRelevantFor(goals)) List(1) else Nil
			} yield {
				seen_l += action.toString
				println("seen: "+seen_l)
				action
			}
		}
		
		def substitute(params: Seq[String], bindings: Map[String, String]): Seq[String] =
			params.map(s => bindings.getOrElse(s, s))
		
		def substitute(atom: Atom, bindings: Map[String, String]): Atom =
			atom.copy(params = substitute(atom.params, bindings))
	
		def substitute(op: Operator, bindings: Map[String, String]): Operator =
			makeAction(op.name, substitute(op.paramName_l, bindings).toList)
		
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
		
		/**
		 * If objects_? is None, return the partialBindings.
		 * Otherwise instantiate a list of bindings, each of which ensures that the atom is grounds.
		 */
		def getGroundBindings(
			partialBindings: Map[String, String],
			op: Operator,
			objects_? : Option[Set[String]]
		): List[Map[String, String]] = {
			objects_? match {
				case None => List(partialBindings)
				case Some(objects) =>
					//println("params: "+op.params)
					val param_l = op.paramName_l.filterNot(partialBindings.contains)
					//println("param_l: "+param_l)
					if (param_l.isEmpty) {
						List(partialBindings)
					}
					else {
						val value_ll = objects.toList.replicateM(param_l.length)
						//println("value_ll: "+value_ll)
						value_ll.map(value_l => partialBindings ++ (param_l zip value_l))
					}
			}
		}
		
		/**
		 * Get the relaxed domain in which the negative effects of operators are removed.
		 */
		def relaxed: Domain = {
			// Remove the negative effects from each operator
			val operator2_l = operator_l.map(o => {
				Operator(
					o.name,
					o.paramName_l,
					o.paramTyp_l,
					o.preconds,
					Literals(o.effects.l.filter(_.pos))
				)
			})
			new Domain(
				type_l,
				constantToType_m,
				predicate_l,
				operator2_l
			)
		}
	}
	
	object Domain {
		def apply(
			type_l: List[String],
			constant_l: List[(String, String)],
			predicate_l: List[Signature],
			operator_l: List[Operator]
		): Domain = {
			// TODO: error if there are duplicate names
			new Domain(
				type_l.toSet,
				constant_l.toMap,
				predicate_l,
				operator_l
			)
		}
	}
	
	case class Problem(
		domain: Domain,
		object_l: List[(String, String)],
		state0: State,
		goals: Literals
	) {
		/**
		 * Get the relaxed problem in which the negative effects of operators are removed.
		 */
		def relaxed: Problem = {
			Problem(domain.relaxed, object_l, state0, goals)
		}
	}
	
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
		assignment_m: Map[String, String],
		//inequality1_m: Map[String, Set[String]],
		//inequality2_m: Map[String, Set[String]],
		impossible_m: Map[String, String]
	) {
		def copy(
			variable_m: Map[String, B] = variable_m,
			assignment_m: Map[String, String] = assignment_m,
			//inequality1_m: Map[String, Set[String]] = inequality1_m,
			//inequality2_m: Map[String, Set[String]] = inequality2_m,
			impossible_m: Map[String, String] = impossible_m
		): Bindings = new Bindings(
			variable_m,
			assignment_m,
			//inequality1_m,
			//inequality2_m,
			impossible_m
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
		def addVariable(name: String, option_l: Set[String]): Option[Bindings] = {
			// This should only be called once per variable
			assert(!variable_m.contains(name))

			setVariableOptions(name, option_l)
		}
		
		private def isVariableName(name: String): Boolean = name.contains(':')
		
		private def setImpossible(name: String, reason: String): Bindings = {
			new Bindings(
				(variable_m - name).mapValues(b => b.copy(ne_l = b.ne_l - name)),
				assignment_m - name,
				impossible_m + (name -> reason)
			)
		}
		
		private def substitute(name1: String, name2: String): Bindings = {
			assert(isVariableName(name1))
			assert(!assignment_m.contains(name1))
			assert(!assignment_m.contains(name2))
			assert(!impossible_m.contains(name1))
			new Bindings(
				(variable_m - name1).mapValues(b => b.copy(ne_l = b.ne_l.map(s => if (s == name1) name2 else s))),
				assignment_m + (name1 -> name2),
				impossible_m
			)
		}
		
		private def setVariableOptions(name: String, option_l: Set[String]): Option[Bindings] = {
			assert(isVariableName(name))
			assert(!assignment_m.contains(name))
			assert(!impossible_m.contains(name))
			
			if (option_l.isEmpty) {
				Some(setImpossible(name, "empty set of possible values"))
			}
			else if (option_l.size == 1) {
				assign(name, option_l.head)
			}
			else {
				val b = variable_m.getOrElse(name, B())
				Some(copy(
					variable_m = variable_m + (name -> b.copy(option_l = option_l))
				))
			}
		}
		
		def assign(assignee: String, value: String): Option[Bindings] = {
			val List(x, y) = List(getCanonicalName(assignee), getCanonicalName(value)).sorted
			if (x == y) return Some(this)
			
			(variable_m.get(x), variable_m.get(y)) match {
				// Both are variables
				case (Some(bX), Some(bY)) =>
					setVariableEquality(x, bX, y, bY)
				// One is a variable and the other is an object
				case (Some(b), None) =>
					Some(substitute(x, y))
				case (None, Some(b)) =>
					Some(substitute(y, x))
				//case (None, None) =>
			}
		}

		/**
		 * set name1 = name2, replacing references to name1 in assignment and inequality values
		 * When variables are set to be equal, collapse one into the other.
		 * a and b get sorted, and the alphanumerically larger one gets collapsed into the smaller one.
		 */
		private def setVariableEquality(name1: String, b1: B, name2: String, b2: B): Option[Bindings] = {
			// Make sure that x and y are not already set to non-equal
			assert(!b1.ne_l.contains(name2))

			// Take intersection of x & y options for x, and remove y entry
			val option_l = b1.option_l intersect b2.option_l
			for {
				bindings1 <- this.setVariableOptions(name2, option_l)
				name2b = bindings1.getCanonicalName(name2)
				bindings2 = bindings1.substitute(name1, name2b)
			} yield bindings2
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
		
		def exclude(name: String, value: String): Option[Bindings] = {
			CONTINUE HERE
		}
		
		def setVariableInequality(a: String, b: String): Option[Bindings] = {
			val List(x, y) = List(getCanonicalName(a), getCanonicalName(b)).sorted
			// Make sure that x and y are not already set to equal
			assert(x != y)
			// Check whether x and y are already set to non-equal
			if (inequality1_m.getOrElse(x, Set()).contains(y)) return Some(this)
			
			// Add variables to each other's inequality sets
			val inequalityX = inequality1_m.getOrElse(x, Set()) + y 
			val inequalityY = inequality1_m.getOrElse(y, Set()) + x 
			val inequality12_m = inequality1_m + (x -> inequalityX) + (y -> inequalityY)
			
			// Remove 
			
			Some(copy(inequality1_m = inequality12_m))
		}
		
		/*private def removeInequalityRow(inequality_m: Map[String, Set[String]], name: String): Map[String, Set[String]] =
			inequality_m - name
		
		private def removeInequalityCol(inequality_m: Map[String, Set[String]], name: String): Map[String, Set[String]] =
			inequality_m.mapValues(_ - name)
		
		private def replaceInequalityValue(inequality_m: Map[String, Set[String]], name: String, value_l: Set[String]): Map[String, Set[String]] = {
			// Remove entry for name
			val m2 = inequality_m - name
			// Replace all instances of name with value_l
			val m3 = m2.mapValues(l => { if (l.contains(name)) l - name ++ value_l else l })
			m3
		}*/
		
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
		val ordering_l: Set[(Int, Int)],
		val binding_m: Map[String, Binding],
		val link_l: Set[CausalLink],
		val openGoal_l: Set[(Int, Int)],
		val possibleLink_l: List[(CausalLink, Map[String, String])]
		//val openGoals: Literals
	) {
		/**
		 * Add an action with the given link and bindings.
		 * This will increase the number of orderings.
		 * It may increase the number of bindings, open goals.
		 * The new bindings may eliminate some of the of possible links.
		 * The action's effects may become possible links for open precondition.
		 * The action's preconditions may become possible links for other action effects. 
		 */
		def addAction(op: Operator, link: CausalLink, bindings: Map[String, Binding]): PartialPlan = {
			// Create a new action with uniquely numbered parameter names
			val i = action_l.size
			val paramName_l = op.paramName_l.map(s => s"${s}_${i}")
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
			val binding2_m = binding_m ++ bindings
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
		}
		
		/**
		 * Add an ordering.
		 * This will reduce the number of possible links.
		 */
		def addOrdering(before_i: Int, after_i: Int): PartialPlan = {
			new PartialPlan(
				action_l = action_l,
				ordering_l = ordering_l + (before_i -> after_i),
				binding_m = binding_m,
				link_l = link_l,
				openGoal_l = openGoal_l,
				possibleLink_l = possibleLink_l
			)
		}
		
		/**
		 * Add an causal link.
		 * This may add a new ordering.
		 * The link will be removed from list of possible links.  
		 */
		def addLink(link: CausalLink): PartialPlan = {
			new PartialPlan(
				action_l = action_l,
				ordering_l = ordering_l,
				binding_m = binding_m,
				link_l = link_l + link,
				openGoal_l = openGoal_l,
				possibleLink_l = possibleLink_l
			)
		}

		def getExistingProviders(consumer_i: Int, precond_i: Int): Unit = {
			val precond = action_l(consumer_i).preconds.l(precond_i)
			
			// Get indexes of actions which may be before consumer_i
			val provider_li = (0 until action_l.size).filter(provider_i =>
				consumer_i != provider_i && !ordering_l.contains((consumer_i, provider_i))
			)
			for (provider_i <- provider_li) {
				val action = action_l(provider_i)
				// If this is a positive preconditions
				if (precond.pos) {
					// Search for valid action/poseffect/binding combinations
					for ((effect, effect_i) <- action.effects.pos.zipWithIndex if effect.name == precond.atom.name) {
						val l0 = effect.params zip precond.atom.params
						val l1 = l0.map(pair => )
					}
				}
				// Else this is a negative precondition
				else {
					// Search for valid action/negeffect/binding combinations
					// This will involve bindings for the negeffect,
					// but we also need to check that the binding doesn't create a poseffect
					// that negates the negative precondition.
				}
			}
		}
		
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
		
		/*
		def isThreat(action: Operator, link: CausalLink): Boolean = {
			
		}
		
		def isConsistent(ordering: (Int, Int)): Boolean = {
			null
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
				Vector(action0, action1), ordering_l, Map(), Set(), openGoal_l, Nil
			)
		}
	}
}