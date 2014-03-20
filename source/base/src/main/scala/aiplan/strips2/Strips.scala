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
		def bind(map: Map[String, String]): Atom =
			copy(params = params.map(s => map.getOrElse(s, s)))
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
		def bind(map: Map[String, String]): Literal =
			copy(atom = atom.bind(map))
		def unary_! : Literal = copy(pos = !pos)
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
		
		def bind(map: Map[String, String]): Literals =
			Literals(l.map(_.bind(map)))
	}
	
	object Literals {
		val empty = new Literals(Unique(), Set(), Set())
		def apply(l: Unique[Literal]): Literals = {
			val pos: Unique[Atom] = l.collect { case Literal(atom, true) => atom }
			val neg: Unique[Atom] = l.collect { case Literal(atom, false) => atom }
			new Literals(l, pos.toSet, neg.toSet)
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
		
		def bind(map: Map[String, String]): Operator = {
			Operator(
				name = name,
				paramName_l = paramName_l.map(s => map.getOrElse(s, s)),
				paramTyp_l = paramTyp_l,
				preconds = preconds.bind(map),
				effects = effects.bind(map)
			)
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
		typToObject_l: List[(String, String)],
		state0: State,
		goals: Literals
	) {
		// Map of parameter type to object set
		val typToObjects_m: Map[String, List[String]] =
			typToObject_l.groupBy(_._1).mapValues(_.map(_._2))

		/**
		 * Get the relaxed problem in which the negative effects of operators are removed.
		 */
		def relaxed: Problem = {
			Problem(domain.relaxed, typToObject_l, state0, goals)
		}
	}
}