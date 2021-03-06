package roboliq.commands.pipette.planner

import scala.collection.immutable.SortedSet
import scalaz._
import scalaz.Scalaz._
import ailib.ch03
import ailib.ch03._
import roboliq.core._,roboliq.entity._,roboliq.processor._,roboliq.events._
import roboliq.commands.pipette._
import roboliq.devices.pipette.PipetteDevice

// REFACTOR: delete this class and rename TransferPlanner2 to TransferPlanner
object TransferPlanner {

	case class Item(
		src_l: List[Well],
		dst: Well,
		volume: LiquidVolume
	)
	
	case class MyState(
		n0: Int,
		n: Int
	) {
		val n1 = n0 + n
		
		def +(action: MyAction): MyState = {
			action match {
				case MyAction_Pipette(n_#) =>
					MyState(n1, n_#)
				//case MyAction_Clean() =>
				//	MyState(n1, Set())
			}
		}
		override def toString = s"($n0, $n1)"
	}

	trait MyAction
	case class MyAction_Pipette(n: Int) extends MyAction
	//case class MyAction_Clean() extends MyAction
	
	case class MyNode(
		val id: Int,
		val state: MyState, 
		val parentOpt: Option[MyNode], 
		val pathCost: Int, 
		val heuristic: Int
	) extends ch03.Node[MyState] with NodeCost[Int] with NodeHeuristic[Int] {
		override def getPrintString: String = {
			state.toString + ": g=" + pathCost.toString + ", f=" + heuristic
		}
		override def toString =
			s"MyNode($id, $state, ${parentOpt.map(_.id)}, $pathCost, $heuristic)"
	}
	
	class MyProblem(
		device: PipetteDevice,
		tip_l: SortedSet[Tip],
		tipModel: TipModel,
		pipettePolicy: PipettePolicy,
		item_l: List[Item]
	) extends ch03.Problem[MyState, MyAction, MyNode] {
		assert(!tip_l.isEmpty)
		
		//private val tipForModel_l = tip_l.filter(tip => tip.permanent_?.isEmpty || tip.permanent_? == Some(tipModel))
		private val N = item_l.size
		private var idPrev = 0;

		val state0 = MyState(0, 0)
		
		val root = new MyNode(0, state0, None, 0, calcHeuristic(state0))
		
		def goalTest(state: MyState): Boolean = (state.n1 == N)
		
		def actions(state: MyState): Iterable[MyAction] = {
			val nMax = math.min(tip_l.size, N - state.n1)
			println(s"math.min(${tip_l.size}, ${N - state.n1}) = $nMax")
			(1 to nMax).map(n => MyAction_Pipette(n))
		}
		
		def childNode(parent: MyNode, action: MyAction): MyNode = {
			val state = parent.state + action
			val g = parent.pathCost + calcCost(state)
			val h = calcHeuristic(state)
			val f = g + h
			idPrev += 1
			new MyNode(idPrev, state, Some(parent), g, f)
		}
		
		def calcCost(state: MyState): Int = {
			val tip2_l = tip_l.toList.take(state.n)
			val item2_l = item_l.drop(state.n0).take(state.n)
			val src_ll = item2_l.map(_.src_l)
			val dst_l = item2_l.map(_.dst)
			
			val tipState2_l = tip2_l.map(TipState.createEmpty)
			
			// Decide which source wells to assign the tips to
			val tspItem_l: List[TipSourcePlanner.Item] = (tipState2_l zip item2_l).map(pair => {
				val (tip, item) = pair
				TipSourcePlanner.Item(tip, item.src_l, item.volume, pipettePolicy)
			})
			val tipToSrcWell_m = TipSourcePlanner.searchGraph(device, tspItem_l).getOrElse(null)
			
			// Aspirate items
			val twvpA_l = (tipState2_l zip item2_l).map(pair => {
				val (tip, item) = pair
				val tipState = TipState.createEmpty(tip)
				val src = tipToSrcWell_m(tip)
				TipWellVolumePolicy(tipState, src, item.volume, pipettePolicy)
			})
			// Dispense items
			val twvpD_l = (tip2_l zip item2_l).map(pair => {
				val (tip, item) = pair
				val tipState = TipState.createEmpty(tip)
				TipWellVolumePolicy(tipState, item.dst, item.volume, pipettePolicy)
			})
			val nA = device.groupSpirateItems(twvpA_l).size
			val nD = device.groupSpirateItems(twvpD_l).size
			(if (nA > 0) nA + 4 else 0) + (if (nD > 0) nD + 4 else 0)
		}
		
		def calcHeuristic(state: MyState): Int = {
			val item_n = N - state.n1
			val group_n = math.ceil(item_n.toDouble / tip_l.size).toInt
			// for each group: one aspirate, one dispense, each costing 5
			group_n * 5
		}
	}
	
	private def getChain(node: MyNode): List[MyNode] = {
		def step(node: MyNode): List[MyNode] = {
			node :: node.parentOpt.map(step).getOrElse(Nil)
		}
		step(node).reverse.tail
	}

	def searchGraph(
		device: PipetteDevice,
		tip_l: SortedSet[Tip],
		tipModel: TipModel,
		pipettePolicy: PipettePolicy,
		item_l: List[Item]
	): RqResult[List[Int]] = {
		val problem = new MyProblem(device, tip_l, tipModel, pipettePolicy, item_l)
		val search = new HeuristicGraphSearch[MyState, MyAction, Int, MyNode]
		val frontier = new MinFirstFrontier[MyState, Int, MyNode] {
			def compareNodes(a: MyNode, b: MyNode): Int = b.heuristic - a.heuristic 
		}
		val debug = new DebugSpec(printFrontier = true, printExpanded = true)
		println("A*:")
		
		search.run(problem, frontier, debug) match {
			case None =>
				RqError("Tips could not be assigned to items")
			case Some(node) =>
				val chain_l = getChain(node)
				println("chain:", chain_l.size, chain_l)
				node.printChain
				// drop the root node, which has no interesting information
				val n_l = chain_l.map(_.state.n)
				RqSuccess(n_l)
		}
	}
}
