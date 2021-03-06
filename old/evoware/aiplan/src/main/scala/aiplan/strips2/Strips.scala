package aiplan.strips2

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
	
	/*case class Literal(atom: Atom, pos: Boolean) {
		def !(literal: Literal): Literal = literal.copy(pos = !literal.pos)
		override def toString = (if (pos) "" else "!") ++ atom.toString
	}
	
	object Literal {
		def apply(s: String, pos: Boolean = true): Literal = {
			Literal(Atom.apply(s), pos)
		}
		implicit def stringToLiteral(s: String): Literal = apply(s, true)
	}*/
	case class Literals(pos: Set[Atom], neg: Set[Atom])
	
	object Literals {
		val empty = Literals(Set(), Set())
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
			val effects2 = effects.copy(pos = effects.pos, neg = effects.neg -- effects.pos)
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
				Literals(preconds_+.toSet, preconds_-.toSet),
				Literals(effects_+.toSet, effects_-.toSet)
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
			def bind(atom_l: Set[Atom]): Set[Atom] = {
				atom_l.map(atom => {
					val params2 = atom.params.map(s => bindings.getOrElse(s, s))
					atom.copy(params = params2)
				})
			}
			
			Operator(
				name = op.name,
				paramName_l = param_l,
				paramTyp_l = op.paramTyp_l,
				preconds = Literals(bind(op.preconds.pos), bind(op.preconds.neg)),
				effects = Literals(bind(op.effects.pos), bind(op.effects.neg))
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
							val preconds2 = Literals(preconds.pos - pp, preconds.neg)
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
					Literals(o.effects.pos, Set())
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
}