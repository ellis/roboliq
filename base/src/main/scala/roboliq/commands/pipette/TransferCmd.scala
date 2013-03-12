package roboliq.commands.pipette

import scala.collection.immutable.SortedSet
import scalaz._
import Scalaz._
import ailib.ch03
import ailib.ch03._
import roboliq.core._,roboliq.entity._,roboliq.processor._,roboliq.events._
import roboliq.devices.pipette.PipetteDevice


case class TransferCmd(
	description_? : Option[String],
	source: List[Source],
	destination: List[Source],
	amount: List[LiquidVolume],
	pipettePolicy_? : Option[String] = None,
	// TODO: add tipPolicy_? too (tip handling overrides)
	preMixSpec_? : Option[MixSpecOpt] = None,
	postMixSpec_? : Option[MixSpecOpt] = None
)

class TransferHandler extends CommandHandler[TransferCmd]("pipette.transfer") {
	def handleCmd(cmd: TransferCmd): RqReturn = {
		val source_l = (cmd.source ++ cmd.destination).distinct
		val l = cmd.source.zip(cmd.destination).zip(cmd.amount)
		fnRequireList(source_l.map(source => lookup[VesselSituatedState](source.id))) { vss_l =>
			val vss_m = vss_l.map(vss => vss.id -> vss).toMap
			fnRequire(lookupAll[TipState]) { tip_l =>
				output(
					l.flatMap(tuple => {
						val ((src, dst), volume) = tuple
						val policy = PipettePolicy(cmd.pipettePolicy_?.get, PipettePosition.WetContact)
						val twvpA = TipWellVolumePolicy(tip_l.head, vss_m(src.vessels.head.id), volume, policy)
						val twvpD = TipWellVolumePolicy(tip_l.head, vss_m(dst.vessels.head.id), volume, policy)
						low.AspirateCmd(None, List(twvpA)) ::
						low.DispenseCmd(None, List(twvpD)) :: Nil
					})
				)
			}
		}
	}
}

/*
case class Group(
	item: Item,
	tip: Tip,
)*/

/**
 * The order of items is fixed.
 * For the items as a whole group, we need to choose a single:
 * - pipette policy
 * - tip model
 * For each item, we need to choose:
 * - whether to wash before the item
 * - tip to assign to an item
 */
object TransferPlanner {

	case class Item(
		src: Well,
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
		pipetteDevice: PipetteDevice,
		tip_l: SortedSet[Tip],
		tipModel: TipModel,
		pipettePolicy: PipettePolicy,
		item_l: List[Item]
	) extends ch03.Problem[MyState, MyAction, MyNode] {
		assert(!tip_l.isEmpty)
		
		//private val tipForModel_l = tip_l.filter(tip => tip.permanent_?.isEmpty || tip.permanent_? == Some(tipModel))
		private val N = item_l.size

		val state0 = MyState(0, 0)
		
		val root = new MyNode(state0, None, 0, calcHeuristic(state0))
		
		def goalTest(state: MyState): Boolean = (state.n1 == N)
		
		def actions(state: MyState): Iterable[MyAction] = {
			val nMax = math.min(tip_l.size, N - state.n1)
			(1 to nMax).map(n => MyAction_Pipette(n))
		}
		
		def childNode(parent: MyNode, action: MyAction): MyNode = {
			val state = parent.state + action
			val g = parent.pathCost + calcCost(state)
			val h = calcHeuristic(state)
			val f = g + h
			new MyNode(state, Some(parent), g, f)
		}
		
		def calcCost(state: MyState): Int = {
			val tip2_l = tip_l.toList.take(state.n)
			val item2_l = item_l.drop(state.n0).take(state.n)
			val src_l = item2_l.map(_.src)
			val dst_l = item2_l.map(_.dst)
			// Aspirate items
			val twvpA_l = (tip2_l zip item2_l).map(pair => {
				val (tip, item) = pair
				val tipState = TipState.createEmpty(tip)
				TipWellVolumePolicy(tipState, item.src, item.volume, pipettePolicy)
			})
			// Dispense items
			val twvpD_l = (tip2_l zip item2_l).map(pair => {
				val (tip, item) = pair
				val tipState = TipState.createEmpty(tip)
				TipWellVolumePolicy(tipState, item.dst, item.volume, pipettePolicy)
			})
			val nA = pipetteDevice.groupSpirateItems(twvpA_l).size
			val nD = pipetteDevice.groupSpirateItems(twvpD_l).size
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
			node :: node.parentOpt.map(getChain).getOrElse(Nil)
		}
		step(node).reverse
	}

	def searchGraph(
		device: PipetteDevice,
		tip_l: SortedSet[Tip],
		tipModel: TipModel,
		pipettePolicy: PipettePolicy,
		item_l: List[Item]
	): RqResult[List[Int]] = {
		val problem = new MyProblem(device, tip_l, tipModel, pipettePolicy, item_l)
		val search = new GraphSearch[MyState, MyAction, MyNode]
		val frontier = new MinFirstFrontier[MyState, Int, MyNode] {
			def compareNodes(a: MyNode, b: MyNode): Int = b.heuristic - a.heuristic 
		}
		val debug = new DebugSpec(printFrontier = false, printExpanded = true)
		println("A*:")
		
		search.run(problem, frontier, debug) match {
			case None =>
				RqError("Tips could not be assigned to items")
			case Some(node) =>
				val chain_l = getChain(node)
				println("chain:", chain_l)
				node.printChain
				RqSuccess(chain_l.map(_.state.n))
		}
	}
}
