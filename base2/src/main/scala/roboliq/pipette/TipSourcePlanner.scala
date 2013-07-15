package roboliq.pipette

import scala.collection.immutable.SortedSet
import scalaz._
import Scalaz._
import ailib.ch03
import ailib.ch03._
import roboliq.core._
import roboliq.commands.pipette._
import roboliq.devices.pipette.PipetteDevice
import ch.ethz.reactivesim.RsResult
import ch.ethz.reactivesim.RsError
import ch.ethz.reactivesim.RsSuccess

/**
 * Rules:
 * if multiple possibilities for tip source, choose one with highest volume
 */

object TipSourcePlanner {
	case class Item(
		tip: TipState,
		src_l: List[Well],
		volume: LiquidVolume,
		policy: PipettePolicy
	)
	
	/**
	 * @param n number of items completed (including the item handled by this state)
	 */
	case class MyState(
		n: Int,
		action_? : Option[MyAction]
	) {
		def +(action: MyAction): MyState =
			MyState(n + 1, action.some)
		override def toString = s"($n, ${action_?.getOrElse("()")})"
	}

	/**
	 * @param src_i index of the source well chosen for the corresponding tip
	 */
	case class MyAction(item: Item, well: Well)
	
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
		item_l: List[Item]
	) extends ch03.Problem[MyState, MyAction, MyNode] {
		private val N = item_l.size
		private var idPrev = 0;

		val state0 = MyState(0, None)
		
		val root = new MyNode(0, state0, None, 0, 1)
		
		def goalTest(state: MyState): Boolean = (state.n == N)
		
		def actions(state: MyState): Iterable[MyAction] = {
			if (state.n == N)
				Nil
			else {
				val item = item_l.drop(state.n).head
				item.src_l.map(well => MyAction(item, well))
			}
		}
		
		def childNode(parent: MyNode, action: MyAction): MyNode = {
			val state = parent.state + action
			
			// Calculate the cost
			val state_l = getChain(parent).map(_.state) ++ List(state)
			val twvp_l = state_l.map(state => {
				val action = state.action_?.get
				val item = action.item
				TipWellVolumePolicy(item.tip, action.well, item.volume, item.policy)
			})
			// Cost is number of groups that will need to be made in order to perform this aspiration 
			val g = device.groupSpirateItems(twvp_l).size
			
			// Heuristic (estimated cost to finish)
			val h = 0 // it could be that all further aspirations could be performed in the same step as the last one
			val f = g + h
			idPrev += 1
			new MyNode(idPrev, state, Some(parent), g, f)
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
		item_l: List[Item]
	): RsResult[Map[TipState, Well]] = {
		val problem = new MyProblem(device, item_l)
		val search = new HeuristicGraphSearch[MyState, MyAction, Int, MyNode]
		val frontier = new MinFirstFrontier[MyState, Int, MyNode] {
			def compareNodes(a: MyNode, b: MyNode): Int = b.heuristic - a.heuristic 
		}
		val debug = new DebugSpec(printFrontier = true, printExpanded = true)
		println("A*:")
		
		search.run(problem, frontier, debug) match {
			case None =>
				RsError("Tips could not be assigned to sources")
			case Some(node) =>
				val chain_l = getChain(node)
				println("chain:", chain_l.size, chain_l)
				node.printChain
				// drop the root node, which has no interesting information
				val tipToWell_m = chain_l.map(node => node.state.action_?.get.item.tip -> node.state.action_?.get.well).toMap
				RsSuccess(tipToWell_m)
		}
	}
}
