package aiplan.hw1

import _root_.ailib.ch03
import _root_.ailib.ch03._
import scala.collection.immutable.HashSet


object Question3 {
	def swap(a: Seq[Int], i1: Int, i2: Int): Seq[Int] = {
		val b = Array[Int](a : _*)
		b(i1) = a(i2)
		b(i2) = a(i1)
		//println(a.mkString("[", " ", "]") ++ " -> " ++ i1.toString ++ i2.toString ++ b.mkString("[", " ", "]"))
		b
	}
	
	def distance(a: Seq[Int]): Int = {
		(0 until 9).map(i => math.abs(i - a.indexOf(i))).reduce(_ + _)
	}
	
	case class State(a: Seq[Int]) {
		//if (a.sorted != Array(0,1,2,3,4,5,6,7,8))
		//	println(a.toList.sorted)
		assert(a.sorted == Seq(0,1,2,3,4,5,6,7,8))
		val i = a.indexOf(0)
		val row = i / 3
		val col = i % 3
		def +(action: Action): State = action.dir match {
			case Direction.Up => State(swap(a, i, i - 3))
			case Direction.Dn => State(swap(a, i, i + 3))
			case Direction.Rt => State(swap(a, i, i + 1)) 
			case Direction.Lt => State(swap(a, i, i - 1))
		}
		override def hashCode = a.foldLeft(0)((acc, n) => acc * 10 + n)
		override def toString = a.mkString("[", " ", "]")
	}
	
	object Direction extends Enumeration {
		val Up, Dn, Rt, Lt = Value
	}
	
	case class Action(dir: Direction.Value) {
	}
	
	case class Node(
		val state: State,
		val parentOpt: Option[Node],
		val pathCost: Int
	) extends ch03.Node[State] with NodeCost[Int] with NodeHeuristic[Int] {
		val heuristic = pathCost + distance(state.a)
		override def getPrintString: String = {
			pathCost.toString + ": " + state.toString
		}
	}
	
	def actions(state: State): List[Action] = {
		List[Option[Action]](
			if (state.row > 0) Some(Action(Direction.Up)) else None,
			if (state.row < 2) Some(Action(Direction.Dn)) else None,
			if (state.col < 2) Some(Action(Direction.Rt)) else None,
			if (state.col > 0) Some(Action(Direction.Lt)) else None
		).flatten
	}

	class EightPuzzleProblem extends Problem[State, Action, Node] {
		// Initial state for Question 1
		//val state0 = new State(Seq(1, 6, 4, 8, 7, 0, 3, 2, 5))
		// Initial state for Question 2
		val state0 = new State(Seq(8, 1, 7, 4, 5, 6, 2, 0, 3))
		
		val root = new Node(state0, None, 0)
		
		def goalTest(state: State): Boolean = state.a match {
			case Seq(0,1,2,3,4,5,6,7,8) => true
			case _ => false
		}
		
		def actions(state: State): Iterable[Action] = {
			List[Option[Action]](
				if (state.row > 0) Some(Action(Direction.Up)) else None,
				if (state.row < 2) Some(Action(Direction.Dn)) else None,
				if (state.col < 2) Some(Action(Direction.Rt)) else None,
				if (state.col > 0) Some(Action(Direction.Lt)) else None
			).flatten
		}
		
		def childNode(parent: Node, action: Action): Node = {
			val state = parent.state + action
			new Node(state, Some(parent), parent.pathCost + 1)
		}
	}
	
	class Frontier extends MinFirstFrontier[State, Int, Node] {
		def compareNodes(a: Node, b: Node): Int = b.heuristic - a.heuristic
	}
	
	def run() {
		val goal = State(Seq(0,1,2,3,4,5,6,7,8))
		val n = step(27, List(goal), HashSet(goal.hashCode))
		println(n)
	}
	
	def run2() {
		val goal = State(Seq(0,1,2,3,4,5,6,7,8))
		val needle = State(Seq(8, 1, 7, 4, 5, 6, 2, 0, 3))
		var prev_? = Option(needle)
		while (prev_?.isDefined) {
			println(prev_?.get)
			prev_? = step2(prev_?.get, List(goal), HashSet(goal.hashCode))
		}
	}

	val xx = List[List[Int]](
		List(8, 1, 7, 4, 5, 6, 2, 0, 3),
		List(8, 1, 7, 4, 5, 6, 2, 3, 0),
		List(8, 1, 7, 4, 5, 0, 2, 3, 6),
		List(8, 1, 0, 4, 5, 7, 2, 3, 6),
		List(8, 0, 1, 4, 5, 7, 2, 3, 6),
		List(0, 8, 1, 4, 5, 7, 2, 3, 6),
		List(4, 8, 1, 0, 5, 7, 2, 3, 6),
		List(4, 8, 1, 2, 5, 7, 0, 3, 6),
		List(4, 8, 1, 2, 5, 7, 3, 0, 6),
		List(4, 8, 1, 2, 5, 7, 3, 6, 0),
		List(4, 8, 1, 2, 5, 0, 3, 6, 7),
		List(4, 8, 1, 2, 0, 5, 3, 6, 7),
		List(4, 0, 1, 2, 8, 5, 3, 6, 7),
		List(4, 1, 0, 2, 8, 5, 3, 6, 7),
		List(4, 1, 5, 2, 8, 0, 3, 6, 7),
		List(4, 1, 5, 2, 0, 8, 3, 6, 7),
		List(4, 1, 5, 0, 2, 8, 3, 6, 7),
		List(0, 1, 5, 4, 2, 8, 3, 6, 7),
		List(1, 0, 5, 4, 2, 8, 3, 6, 7),
		List(1, 2, 5, 4, 0, 8, 3, 6, 7),
		List(1, 2, 5, 0, 4, 8, 3, 6, 7),
		List(1, 2, 5, 3, 4, 8, 0, 6, 7),
		List(1, 2, 5, 3, 4, 8, 6, 0, 7),
		List(1, 2, 5, 3, 4, 8, 6, 7, 0),
		List(1, 2, 5, 3, 4, 0, 6, 7, 8),
		List(1, 2, 0, 3, 4, 5, 6, 7, 8),
		List(1, 0, 2, 3, 4, 5, 6, 7, 8)
	)
	val xxi = xx.zipWithIndex
	val yyi = List[List[Int]](
		List(8,1,7,4,5,6,2,0,3),
		List(8,1,7,4,5,6,0,2,3),
		List(8,1,7,0,5,6,4,2,3),
		List(0,1,7,8,5,6,4,2,3)
	).zipWithIndex

	def step(countdown: Int, frontier: List[State], seen: HashSet[Int]): Int = {
		if (countdown == 0)
			return frontier.length
		val frontier1 = HashSet(frontier.flatMap(state => actions(state).map(state + _)) :_ *)
		val frontier2 = frontier1.filter(state => !seen.contains(state.hashCode)).toList
		val seen2 = seen ++ frontier2.map(_.hashCode)
		if (countdown > 24) {
			println("step: "+(28-countdown))
			frontier2.foreach(state => println(state.a.mkString(" ")))
		}
		
		/*
		for ((xx, i) <- xxi) {
			val state = State(xx)
			if (frontier2.contains(state)) {
				val prev_l = actions(state).map(state + _)
				val hash_l = prev_l.map(_.hashCode)
				println(s"${xx.mkString(" ")} at ${28-countdown} prev "+hash_l.filter(seen.contains))
			}
		}*/
		
		for ((xx, i) <- yyi) {
			val state = State(xx)
			if (frontier2.contains(state)) {
				val prev_l = actions(state).map(state + _)
				val hash_l = prev_l.map(_.hashCode)
				println(f"$i%2d ${xx.mkString("")} at ${28-countdown} prev "+hash_l.filter(seen.contains))
			}
		}

		if (frontier2.isEmpty)
			return -1;
		else
			step(countdown - 1, frontier2, seen2)
	}

	def step2(needle: State, frontier: List[State], seen: HashSet[Int]): Option[State] = {
		val frontier1 = HashSet(frontier.flatMap(state => actions(state).map(state + _)) :_ *)
		val frontier2 = frontier1.filter(state => !seen.contains(state.hashCode)).toList
		if (frontier2.contains(needle)) {
			val prev_l = actions(needle).map(needle + _).filter(state => seen.contains(state.hashCode))
			prev_l match {
				case Nil => None
				case x :: _ => Some(x)
			}
		}
		else if (frontier2.isEmpty) {
			None
		}
		else {
			val seen2 = seen ++ frontier2.map(_.hashCode)
			step2(needle, frontier2, seen2)
		}
	}
}
