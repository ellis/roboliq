package roboliq.pipette

import ailib.ch03
import ailib.ch03._
import roboliq.core._
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap
import ch.ethz.reactivesim.RsResult
import ch.ethz.reactivesim.RsError
import ch.ethz.reactivesim.RsSuccess


class TipModelSearcher1[Item, Liquid, TipModel <: AnyRef] {
	case class MyState(l: List[(Item, TipModel)], rest: List[Item]) {
		def +(action: MyAction): MyState = MyState((rest.head, action.tipModel) :: l, rest.tail)
		override def toString = l.reverse.map(_._2).mkString(",")
	}

	case class MyAction(tipModel: TipModel)
	
	case class MyNode(
		val state: MyState, 
		val parentOpt: Option[MyNode], 
		val pathCost: Int, 
		val heuristic: Int
	) extends ch03.Node[MyState] with NodeCost[Int] with NodeHeuristic[Int] {
		override def getPrintString: String = {
			state.toString + ": g=" + pathCost.toString + ", f=" + heuristic
		}
	}
	
	class MyProblem(
		item_l: List[Item],
		itemToLiquid_m: Map[Item, Liquid],
		itemToModels_m: Map[Item, Seq[TipModel]]
	) extends ch03.Problem[MyState, MyAction, MyNode] {
		val state0 = new MyState(Nil, item_l)
		
		val root = new MyNode(state0, None, 0, calcHeuristic(state0))
		
		def goalTest(state: MyState): Boolean = state.rest.isEmpty
		
		def actions(state: MyState): Iterable[MyAction] =
			itemToModels_m(state.rest.head).map(MyAction)
		
		def childNode(parent: MyNode, action: MyAction): MyNode = {
			val state = parent.state + action
			val g = calcCost(state.l)
			val h = calcHeuristic(state)
			val f = g + h
			new MyNode(state, Some(parent), g, f)
		}
		
		def calcCost(l: List[(Item, TipModel)]): Int = {
			val tipModel_l = new HashSet[TipModel]
			val liquidToTipModels_m = new HashMap[Liquid, Set[TipModel]]
			
			var itemCost = 0
			for ((item, tipModel) <- l) {
				// Cost of 1 for distance from top choice of tip model
				itemCost += itemToModels_m(item).takeWhile(_ ne tipModel).length
				
				tipModel_l += tipModel

				val liquid = itemToLiquid_m(item)
				liquidToTipModels_m(liquid) = liquidToTipModels_m.getOrElse(liquid, Set()) + tipModel
			}
			
			val tipModelCost = tipModel_l.size - 1
			val liquidCost = liquidToTipModels_m.map(_._2.size - 1).sum
			itemCost + 10000 * tipModelCost + 1000000 * liquidCost
		}
		
		def calcHeuristic(state: MyState): Int = {
			// Only keep items which can be pipetted by the tip models we already have
			val tipModel_l = state.l.map(_._2).toSet
			val item_l = state.rest.filter(item => {
				tipModel_l.intersect(itemToModels_m(item).toSet).isEmpty
			})
			val tipModel_n = tipModelCountRequired(item_l)
			10000 * tipModel_n
		}
		
		def tipModelCountRequired(item_l: List[Item]): Int = {
			val items = item_l.toSet
			val n = items.size
			val l = item_l.zipWithIndex
			val m = itemToModels_m.filterKeys(item_l.contains).mapValues(_.toSet)
			val tipModel_l = m.values.toSet.flatten
			val tipModelToItems_m: Map[TipModel, Set[Item]] =
				tipModel_l.map(v => (v, m.keySet.filter(m(_).contains(v)))).toMap
			
			for (model_n <- 1 until tipModel_l.size) {
				if (tipModelCountRequired_sub(tipModelToItems_m, model_n))
					return model_n
			}
			tipModel_l.size
		}
		
		/**
		 * See whether model_n combinations of tip models can be used to pipette all items
		 */
		private def tipModelCountRequired_sub(
			tipModelToItems_m: Map[TipModel, Set[Item]],
			model_n: Int
		): Boolean = {
			if (model_n <= 0)
				false
			else {
				for ((tipModel, item_l) <- tipModelToItems_m) {
					val tipModelToItems2_m = tipModelToItems_m.mapValues(_ -- item_l).filterNot(_._2.isEmpty)
					if (tipModelToItems2_m.isEmpty)
						return true
					else if (tipModelCountRequired_sub(tipModelToItems2_m, model_n - 1))
						return true
				}
				false
			}
		}
	}

	def searchGraph(
		item_l: List[Item],
		itemToLiquid_m: Map[Item, Liquid],
		itemToModels_m: Map[Item, Seq[TipModel]]
	): RsResult[Map[Item, TipModel]] = {
		val problem = new MyProblem(item_l, itemToLiquid_m, itemToModels_m)
		val search = new GraphSearch[MyState, MyAction, MyNode]
		//val searchBFS = new BreadthFirstSearch[State, Action]
		//val searchDFS = new DepthFirstSearch[State, Action]
		val frontier = new MinFirstFrontier[MyState, Int, MyNode] {
			def compareNodes(a: MyNode, b: MyNode): Int = b.heuristic - a.heuristic 
		}
		val debug = new DebugSpec(printFrontier = false, printExpanded = true)
		println("A*:")
		
		search.run(problem, frontier, debug) match {
			case None => RsError("Tip models could not be assigned to items")
			case Some(node) =>
				println("chain:", node)
				node.printChain
				return RsSuccess(node.state.l.toMap)
		}
	}
}
