package roboliq.ai.strips

import scala.language.implicitConversions
import scala.collection.mutable
import scalaz._
import scalaz.Scalaz._
import scala.collection.mutable.ListBuffer
import roboliq.ai.plan.Unique

class Signature(val name: String, val paramName_l: List[String], val paramTyp_l: List[String]) {
	assert(paramTyp_l.length == paramName_l.length)
	
	def getSignatureString = name + (paramName_l zip paramTyp_l).map(pair => s"${pair._1}:${pair._2}").mkString("(", " ", ")")
	def toStripsText: String = {
		(name :: (paramName_l zip paramTyp_l).map(pair => pair._1 + " - " + pair._2)).mkString("(", " ", ")")
	}
	override def toString = getSignatureString
}

object Signature {
	def apply(name: String, nameToTyp_l: (String, String)*): Signature = {
		val (paramName_l, paramTyp_l) = nameToTyp_l.toList.unzip
		new Signature(name, paramName_l, paramTyp_l)
	}
	
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
	def bind(map: Map[String, String]): Atom =
		copy(params = params.map(s => map.getOrElse(s, s)))
	override def toString = (name :: params.toList).mkString(" ")
	def toStripsText: String = s"(${toString})"
}

object Atom {
	def apply(ss: String*): Atom = {
		new Atom(ss.head, ss.tail.toSeq)
	}
	
	def parse(s: String): Atom = {
		val l = s.split(" ")
		new Atom(l.head, l.tail)
	}
	implicit def stringToAtom(s: String): Atom = parse(s)
}

case class Literal(atom: Atom, pos: Boolean) {
	def bind(map: Map[String, String]): Literal =
		copy(atom = atom.bind(map))
	def unary_! : Literal = copy(pos = !pos)
	override def toString = (if (pos) "" else "!") ++ atom.toString
	def toStripsText: String = if (pos) atom.toStripsText else s"(not ${atom.toStripsText})" 
}

object Literal {
	def apply(pos: Boolean, name: String, params: String*): Literal =
		Literal(Atom(name, params.toSeq), pos)
	
	def parse(text: String): Literal = {
		val (pos, text1) = {
			if (text.startsWith("!")) (false, text.tail)
			else (true, text)
		}
		val l = text1.split(" ")
		Literal(new Atom(l.head, l.tail), pos)
	}
}
/*object Literal {
	def apply(s: String, pos: Boolean = true): Literal = {
		Literal(Atom.apply(s), pos)
	}
	implicit def stringToLiteral(s: String): Literal = apply(s, true)
}*/

class Literals private (val l: Unique[Literal], val pos: Set[Atom], val neg: Set[Atom]) {
	/**
	 * Check whether the given literal holds among the literals.
	 * If the literal is positive, 'holding' means that the set of positive atom contains the literal's atom.
	 * Otherwise, 'holding' means that the set of positive atoms does not contain the literal's atom.
	 */
	def holds(literal: Literal): Boolean = {
		if (literal.pos)
			pos.contains(literal.atom)
		else
			!pos.contains(literal.atom)
	}
	
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
	
	def ++(that: Literals): Literals = {
		// TODO: consider removing superfluous values from l
		//val removePos = pos intersect that.neg
		//val removeNeg = neg intersect that.pos
		new Literals(l ++ that.l, pos -- that.neg ++ that.pos, neg ++ that.neg -- that.pos)
	}
	
	def bind(map: Map[String, String]): Literals =
		Literals(l.map(_.bind(map)))
		
	override def toString: String = {
		l.mkString("Literals(", ",", ")")
	}
	
	override def equals(that: Any) = that match {
		case that2: Literals => this.pos == that2.pos && this.neg == that2.neg
		case _ => false
	}
}

object Literals {
	val empty = new Literals(Unique(), Set(), Set())
	def apply(l: Unique[Literal]): Literals = {
		val pos: Unique[Atom] = l.collect { case Literal(atom, true) => atom }
		val neg: Unique[Atom] = l.collect { case Literal(atom, false) => atom }
		// TODO: consider removing pos from neg
		// TODO: consider removing duplicates from l, as well as negatives for which there is a positive
		new Literals(l, pos.toSet, neg.toSet)
	}
	def apply(pos: List[Atom], neg: List[Atom]): Literals = {
		val pos1 = pos.map(atom => Literal(atom, true))
		val neg1 = neg.map(atom => Literal(atom, false))
		// TODO: consider removing pos1 from neg1
		val l = Unique((pos1 ++ neg1) : _*)
		new Literals(l, pos.toSet, neg.toSet)
	}
	def apply(state: State): Literals = {
		apply(state.atoms.toList, Nil)
	}
	def fromStrings(l: String*): Literals = {
		val l2 = Unique[Literal](l.map(Literal.parse) : _*)
		apply(l2)
	}
}

case class State(atoms: Set[Atom]) {
	def holds(atom: Atom): Boolean = atoms.contains(atom)
	def satisfied(literals: Literals): Boolean = {
		literals.pos.forall(atoms.contains) && literals.neg.forall(atom => !atoms.contains(atom))
	}
	def ++(that: State): State = {
		State(atoms ++ that.atoms)
	}
	def ++(literals: Literals): State = {
		State(atoms -- literals.neg ++ literals.pos)
	}
}

object State {
	def apply(literals: Literals): State = {
		State(literals.pos)
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
	
	def bind(map: Map[String, String]): Operator = {
		Operator(
			name = name,
			paramName_l = paramName_l.map(s => map.getOrElse(s, s)),
			paramTyp_l = paramTyp_l,
			preconds = preconds.bind(map),
			effects = effects.bind(map)
		)
	}
	
	/**
	 * Returns a list of this operator's eq/ne literal preconditions and a new operator without any such preconditions 
	 */
	def removeEqualityPreconds(): (List[Literal], Operator) = {
		val (eq_l, preconds2) = preconds.l.partition(lit => List("eq", "ne").contains(lit.atom.name))
		val op2 = Operator(
			name = name,
			paramName_l = paramName_l,
			paramTyp_l = paramTyp_l,
			preconds = Literals(preconds2),
			effects = effects
		)
		(eq_l.toList, op2)
	}

	def tostripsLines(indent: String = ""): List[String] = {
		val l = new ListBuffer[String]
		l += s"(:action $name"
		l += s"  :parameters ("
		l ++= (paramName_l zip paramTyp_l).map(pair => s"    ${pair._1} - ${pair._2}")
		l += s"  )"
		l += s"  :precondition (and"
		l ++= preconds.l.map(literal => s"    ${literal.toStripsText}")
		l += s"  )"
		l += s"  :effect (and"
		l ++= effects.l.map(literal => s"    ${literal.toStripsText}")
		l += s"  )"
		l += s")"
		l.result.map(indent + _)
	}
	
	def toStripsText(indent: String = ""): String =
		tostripsLines(indent).mkString("\n")

/*		"  (:action tecan_pipette1
    :parameters (?a - tecan ?d - pipetter ?p - pipetterProgram ?l1 - labware ?m1 - model ?s1 - site ?sm1 - siteModel)
    :precondition (and
      (agent-has-device ?a ?d)
      (device-can-site ?d ?s1)
      (model ?s1 ?sm1)
      (stackable ?sm1 ?m1)
      (model ?l1 ?m1)
      (location ?l1 ?s1)
"    )
*/
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
	type_m: Map[String, String],
	constantToType_m: Map[String, String],
	predicate_l: List[Signature],
	operator_l: List[Operator]
) {
	// FIXME: For some reason, Logger is taking many seconds to instantiate
	//private val logger = Logger("roboliq.ai.strips.Domain")
	//private val logger = Logger[this.type]
	
	val nameToPredicate_m = predicate_l.map(x => x.name -> x).toMap
	val nameToOperator_m = operator_l.map(x => x.name -> x).toMap
	
	// Get the chain of types from the given type up to `any` (but not including `any`)
	def getTypeChain(typ: String): List[String] = {
		if (typ == "any") Nil
		else typ :: getTypeParents(typ)
	}
	
	// Get the chain of types from the given type up to `any`, but not including the given type or `any`
	def getTypeParents(typ: String): List[String] = {
		type_m.get(typ) match {
			case Some(parent) => getTypeChain(parent)
			case None => Nil
		}
	}
	
	def getOperator(name: String): Either[String, Operator] =
		nameToOperator_m.get(name).toRight(s"could not find operator `$name`")

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
						val preconds2 = preconds.removePos(pp)
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
		//logger.debug(acc, op, effects, σ, goals)
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
			type_m,
			constantToType_m,
			predicate_l,
			operator2_l
		)
	}
	
	def toStripsText: String = {
		val s = List(
				"(define (domain X)",
				"  (:requirements :strips :typing)"
			).mkString("", "\n", "\n") +
			type_m.toList.sorted.map(pair => s"${pair._1} - ${pair._2}").mkString("  (:types\n    ", "\n    ", "\n  )\n") +
			predicate_l.map(_.toStripsText).sorted.mkString("  (:predicates\n    ", "\n    ", "\n  )\n") +
			operator_l.map(_.toStripsText("  ")).mkString("\n") +
			"\n)"
		s
	}
}

object Domain {
	def apply(
		type_m: Map[String, String],
		constantToType_l: List[(String, String)],
		predicate_l: List[Signature],
		operator_l: List[Operator]
	): Domain = {
		println("Domain.apply")
		// TODO: error if there are duplicate names
		new Domain(
			type_m,
			constantToType_l.toMap,
			predicate_l,
			operator_l
		)
	}
}

case class Problem(
	domain: Domain,
	typToObject_l: List[(String, String)],
	state0: State,
	goals: Literals
) {
	// Map of parameter type to object set
	val typToObjects_m: Map[String, List[String]] = {
		// Get extended type -> object list that includes all super-types
		// I.e. 'reader' is a type of 'device', so ('reader', 'mario__reader') => [('reader', 'mario__reader'), ('device', 'mario__reader')]
		val l = typToObject_l.flatMap { case (typ, obj) =>
			println("domain.getTypeChain("+typ+"): "+domain.getTypeChain(typ))
			domain.getTypeChain(typ).map(typ => typ -> obj)
		}
		println("l: "); l.sorted.foreach(println)
		l.groupBy(_._1).mapValues(_.map(_._2))
	}
	println("typToObject_l: "+typToObject_l)
	println("typToObjects_m:"); typToObjects_m.foreach(println)

	/**
	 * Get the relaxed problem in which the negative effects of operators are removed.
	 */
	def relaxed: Problem = {
		Problem(domain.relaxed, typToObject_l, state0, goals)
	}
	
	def toStripsText: String = {
		val l = new ListBuffer[String]
		l += "(define (problem X-problem)"
		l += "  (:domain X)"
		
		l += "  (:objects"
		l ++= typToObject_l.sorted.map(pair => s"    ${pair._2} - ${pair._1}").sorted
		l += "  )"
		
		l += "  (:init"
		l ++= state0.atoms.toList.map("    " + _.toStripsText).sorted
		l += "  )"
		
		l += "  (:goals (and"
		l ++= goals.l.toList.map("    " + _.toStripsText)
		l += "  ))"
		
		l += ")"
		
		l.result.mkString("\n")
	}
}
