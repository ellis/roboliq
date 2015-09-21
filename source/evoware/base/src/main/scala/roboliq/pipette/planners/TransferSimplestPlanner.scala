package roboliq.pipette.planners

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scalaz._
import scalaz.Scalaz._
import grizzled.slf4j.Logger
import ailib.ch03
import ailib.ch03._
import roboliq.core._
import roboliq.entities._
import scala.collection.immutable.LinearSeq

/**
 * The order of items is fixed.
 * For the items as a whole group, we need to choose a single:
 * - pipette policy
 * - tip model
 * For each item, we need to choose:
 * - source well (rotate through available liquid source wells)
 * - next available tip (I'd like to still optimize this a bit with regard to washing)
 */
object TransferSimplestPlanner {
	
	private val logger = Logger[this.type]

	type Item = TransferPlanner.Item
	
	case class BatchItem(
		tip: Tip,
		src: Well,
		dst: Well,
		volume: LiquidVolume
	) {
		override def toString = s"BatchItem(${tip.key}, ${src.key}, ${dst.key}, $volume)"
	}
	
	case class Batch(item_l: List[BatchItem]) {
		override def toString = item_l.mkString("Batch(\n  ", "\n  ", "\n)")
	}
	
	case class MyState(
		n: Int,
		tipToState_m: Map[Tip, TipState]
	)
	
	case class MyAction(
		item_l: List[BatchItem]
	)
	
	case class MyNode(
		val id: Int,
		val state: MyState,
		val parentOpt: Option[MyNode], 
		val action: MyAction,
		val pathCost: Int, 
		val heuristic: Int
	) extends ch03.Node[MyState] with NodeCost[Int] with NodeHeuristic[Int] {
		override def getPrintString: String = {
			state.toString + ": g=" + pathCost.toString + ", f=" + heuristic
		}
		override def toString =
			s"MyNode($id, $state, ${parentOpt.map(_.id)}, $pathCost, $heuristic)"
	}
	
	/*
	class MyProblem(
		device: PipetteDevice,
		worldState: WorldState,
		tip_l: SortedSet[Tip],
		tipModel: TipModel,
		pipettePolicy: PipettePolicy,
		item_l: List[Item]
	) extends ch03.Problem[MyState, MyAction, MyNode] {
		assert(!tip_l.isEmpty)
		
		//private val tipForModel_l = tip_l.filter(tip => tip.permanent_?.isEmpty || tip.permanent_? == Some(tipModel))
		private val N = item_l.size
		private var idPrev = 0;

		val state0 = MyState(0, tip_l.toList.map(tip => tip -> worldState.tip_state_m.getOrElse(tip, TipState.createEmpty(tip))).toMap)
		
		val root = new MyNode(0, state0, None, MyAction(Nil), 0, calcHeuristic(state0))
		
		def goalTest(state: MyState): Boolean = (state.n == N)
		
		def actions(state: MyState): Iterable[MyAction] = {
			// Max number of items for a batch
			val item_l_# = item_l.drop(state.n).take(tip_l.size)
			// FIXME: HACK: Fake tip states...
			val tipState_l = tip_l.toList.map(TipState.createEmpty)

			// Loop through
			val action_l = (1 to tip_l.size).toList.map(n => {
				val item2_l = item_l_#.take(n)
				// Decide which source wells to assign the tips to
				// Create list of TipSourcePlanner.Items
				val tspItem_l: List[TipSourcePlanner.Item] = (tipState_l zip item2_l).map(pair => {
					val (tip, item) = pair
					TipSourcePlanner.Item(tip, item.src_l, item.volume, pipettePolicy)
				})
				// Get a mapping back from Tip to source Well
				val tipToSrcWell_m = TipSourcePlanner.searchGraph(device, worldState, tspItem_l).getOrElse(null)
				
				val batchItem_l = (tipState_l zip item2_l).map(pair => {
					val (tip, item) = pair
					BatchItem(tip.conf, tipToSrcWell_m(tip), item.dst, item.volume)
				})
				MyAction(batchItem_l)
			})
			action_l
		}
		
		def childNode(parent: MyNode, action: MyAction): MyNode = {
			// Get pending tip clean intensities after parent action:
			// Aspriate TipCleanPolicies
			val cleanA_m = parent.action.item_l.map(item => item.tip -> worldState.getWellTipCleanPolicy(item.src)).toMap
			// Dispense TipCleanPolicies
			val cleanD_m: Map[Tip, TipCleanPolicy] = {
				parent.action.item_l.map(item => item.tip -> (
					if (pipettePolicy.pos == PipettePosition.WetContact)
						worldState.getWellTipCleanPolicy(item.dst)
					else
						TipCleanPolicy.NN
				)).toMap
			}
			val tipToCleanIntensityPending_m: Map[Tip, CleanIntensity.Value] =
				(cleanA_m |+| cleanD_m).mapValues(_.exit)

			val state = MyState(parent.state.n + action.item_l.size, tipToCleanIntensityPending_m)
			val g = parent.pathCost + calcCost(action)
			val h = calcHeuristic(state)
			val f = g + h
			idPrev += 1
			new MyNode(idPrev, state, Some(parent), action, g, f)
		}
		
		def calcCost(action: MyAction): Int = {
			val twvpA_l = action.item_l.map(item => {
				val tipState = TipState.createEmpty(item.tip)
				TipWellVolumePolicy(tipState, item.src, item.volume, pipettePolicy)
			})
			val twvpD_l = action.item_l.map(item => {
				val tipState = TipState.createEmpty(item.tip)
				TipWellVolumePolicy(tipState, item.dst, item.volume, pipettePolicy)
			})
			val nA = device.groupSpirateItems(twvpA_l, worldState).size
			val nD = device.groupSpirateItems(twvpD_l, worldState).size
			(if (nA > 0) nA + 4 else 0) + (if (nD > 0) nD + 4 else 0)
		}
		
		def calcHeuristic(state: MyState): Int = {
			val item_n = N - state.n
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
	*/

	def searchGraph(
		device: PipetteDevice,
		worldState: WorldState,
		tip_l: SortedSet[Tip],
		tipModel: TipModel,
		pipettePolicy: PipettePolicy,
		item_l: List[Item]
	): RsResult[List[Batch]] = {
		val sourceUsed_l = mutable.Queue[Well]()
		val item_ll = item_l.grouped(tip_l.size)
		def pickNextSource(src_l: List[Well]): Well = {
			// Get index of these sources in the queue
			val index_l = src_l.map(well => sourceUsed_l.indexOf(well))
			// If any of the sources hasn't been used yet,
			val notyet = index_l.indexOf(-1)
			if (notyet != -1) {
				// return the first one of the unused sources
				src_l(notyet)
			}
			else {
				// otherwise, pick the source with the lowest index (hasn't been used for the longest)
				val l1 = src_l zip index_l
				val l2 = l1.sortBy(_._2)
				l2.head._1
			}
		}
		val batch_l = item_ll.toList.map(item_l => {
			// Map each Item to a BatchItem
			val batchItem_l = (tip_l.toList zip item_l).map(pair => {
				val (tip, item) = pair
				//val src = sourceUsed_l.dequeueFirst(item.src_l.contains).getOrElse(item.src_l.head)
				val src = pickNextSource(item.src_l)
				sourceUsed_l.dequeueFirst(_.eq(src))
				sourceUsed_l += src
				BatchItem(tip, src, item.dst, item.volume)
			})
			Batch(batchItem_l)
		})
		RqSuccess(batch_l)
	}
}
