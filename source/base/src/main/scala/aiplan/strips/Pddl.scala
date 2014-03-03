package aiplan.strips

import scala.collection.mutable
import grizzled.slf4j.Logger
import scalaz._
import Scalaz._


case class Typ(name: String)

//case class Predicate(name: String, args: Seq[(Typ, String)])

case class Atom(name: String, params: Seq[String]) {
	override def toString = (name :: params.toList).mkString(" ")
}

case class Literal(atom: Atom, pos: Boolean) {
	override def toString = (if (pos) "" else "!") ++ atom.toString
}

case class State(atoms: Set[Atom]) {
	def holds(atom: Atom): Boolean = atoms.contains(atom)
	def satisfied(literals: Set[Literal]): Boolean = {
		literals.forall(literal => {
			(!literal.pos) ^ atoms.contains(literal.atom)
		})
	}
}

case class Operator(name: String, params: Seq[String], preconds: Set[Literal], effects: Set[Literal])

case class Action(operator: Operator, params: Seq[String]) {
	val bindings = (operator.params zip params).toMap
	val preconds = operator.preconds.map(literal => {
		val params2 = literal.atom.params.map(s => bindings.getOrElse(s, s))
		literal.copy(atom = literal.atom.copy(params = params2))
	})
	val effects = operator.effects.map(literal => {
		val params2 = literal.atom.params.map(s => bindings.getOrElse(s, s))
		literal.copy(atom = literal.atom.copy(params = params2))
	})

	def isApplicableIn(state: State): Boolean = {
		val preconds_+ = Stuff.pos(preconds)
		val preconds_- = Stuff.neg(preconds)
		preconds_+.subsetOf(state.atoms) && preconds_-.intersect(state.atoms).isEmpty
	}
	
	def isRelevantFor(goals: Set[Literal]): Boolean = {
		val effects_+ = Stuff.pos(effects)
		val effects_- = Stuff.neg(effects) -- effects_+
		val goals_+ = Stuff.pos(goals)
		val goals_- = Stuff.neg(goals)
		// If the actions effects contribute to the goal
		val contribute = goals_+.exists(effects_+.contains) || goals_-.exists(effects_-.contains)
		// If the actions effects conflict with the goal
		val noConflict = goals_+.intersect(effects_-).isEmpty && goals_-.intersect(effects_+).isEmpty
		contribute && noConflict
	}
	
	def transition(state: State): State = {
		val effects_+ = Stuff.pos(effects)
		val effects_- = Stuff.neg(effects)
		val atoms2 = (state.atoms -- effects_-) ++ effects_+
		State(atoms2)
	}
	
	override def toString = (operator.name :: params.toList).mkString(" ")
}


object Stuff {
	private val logger = Logger[this.type]

	def pos(literals: Set[Literal]): Set[Atom] = literals.filter(_.pos).map(_.atom)
	def neg(literals: Set[Literal]): Set[Atom] = literals.filterNot(_.pos).map(_.atom)
	/*def bind(predicate: Predicate, binding: Binding): Atom = {
		
	}*/
	
	def getApplicableActions(
		op: Operator,
		s: State
	): List[Action] =
		addApplicables(Nil, op, pos(op.preconds), neg(op.preconds), Map(), s)

	private def addApplicables(
		acc: List[Action],
		op: Operator,
		precs_+ : Set[Atom],
		precs_- : Set[Atom],
		σ: Map[String, String],
		s: State
	): List[Action] = {
		logger.debug(acc, op, precs_+, precs_-, σ, s)
		if (precs_+.isEmpty) {
			val valid = precs_-.forall(np => {
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
			val pp = precs_+.head
			logger.debug("pp: "++pp.toString)
			s.atoms.toList.filter(_.name == pp.name).foldLeft(acc) { (acc, sp) =>
				extendBindings(σ, sp, pp) match {
					case Some(σ2) => addApplicables(acc, op, precs_+ - pp, precs_-, σ2, s)
					case _ => acc
				}
			}
		}
	}
	
	def getRelevantActions(
		op: Operator,
		goals: Set[Literal],
		objects: Set[String]
	): List[Action] = {
		addRelevants(Nil, op, pos(op.effects), neg(op.effects), Map(), goals, Some(objects))
	}

	def getRelevantPartialActions(
		op: Operator,
		goals: Set[Literal]
	): List[Action] =
		addRelevants(Nil, op, pos(op.effects), neg(op.effects), Map(), goals, None)

	private def addRelevants(
		acc: List[Action],
		op: Operator,
		effects_+ : Set[Atom],
		effects_- : Set[Atom],
		σ: Map[String, String],
		goals: Set[Literal],
		objects_? : Option[Set[String]]
	): List[Action] = {
		logger.debug(acc, op, effects_+, effects_-, σ, goals)
		// If the actions effects contribute to the goal
		val contributingEffects = goals.exists(op.effects.contains)
		val seen_l = mutable.Set[String]()
		for {
			// get an open goal
			goal <- goals.toList
			// get a relevant effect, if there is one
			effect <- op.effects.toList.filter(effect => effect.pos == goal.pos && effect.atom.name == goal.atom.name)
			// see whether effects can be unified with this goal
			partialBindings <- extendBindings(σ, goal.atom, effect.atom).toList
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

	def substitute(op: Operator, bindings: Map[String, String]): Action =
		Action(op, substitute(op.params, bindings))
	
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
				val param_l = op.params.filterNot(partialBindings.contains)
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
}