package roboliq.devices.pipette.scheduler

import scala.collection.immutable.SortedSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.PriorityQueue

import roboliq.common._
import roboliq.commands._
import roboliq.commands.pipette._
import roboliq.commands.pipette.{L3A_PipetteItem => Item}
import roboliq.compiler._
import roboliq.devices.pipette._


class PipetteScheduler(
	val device: PipetteDevice,
	val ctx: CompilerContextL3
) {
	private val builderA = new GroupABuilder(device, ctx)
	private val builderB = new GroupBBuilder(device, ctx)
	private var lnScore: Array[Double] = null
	private var lGroupA: Array[GroupA] = null
	private var lGroupB: Array[GroupB] = null
	case class Score(iItem: Int, nPathCost: Double, nTotalCostMin: Double)
	private val queue = new PriorityQueue[Score]()(ScoreOrdering)
	
	def x(cmd: L3C_Pipette) {
		for {
			mLM <- builderA.tr1Items(cmd.args.items)
		} {
			val nTips = device.config.tips.size
			val nItems = cmd.args.items.size // TODO: need to break up large pipetting items into multiple items
			var iItem = 0
			lnScore = new Array[Double](nItems)
			lGroupA = new Array[GroupA](nItems)
			lGroupB = new Array[GroupB](nItems)
			queue.clear
			queue += Score(-1, 0, 0)
			val lItemAll = cmd.args.items.toList
			var bDone = false
			while (!queue.isEmpty && !bDone) {
				//println("lnScore: "+lnScore.toList)
				//println("queue: "+queue)
				val score = queue.dequeue
				//println("score: "+score)
				val iItemParent = score.iItem
				bDone = (iItemParent == nItems - 1)
				if (!bDone) {
					val lItem = lItemAll.drop(iItemParent + 1)
					x1(lItem, mLM, iItemParent)
				}
			}
			println("lnScore: "+lnScore.toList)
			//println("lGroupA: "+lGroupA.toList)
			//println("lGroupB: "+lGroupB.toList)
		}
	}
	
	private def x1(lItem: List[Item], mLM: Map[Item, LM], iItemParent: Int) {
		// New group with 0 items
		val g0 = {
			if (iItemParent < 0)
				builderA.createGroupA(ctx.states, mLM)
			else {
				val gParentA = lGroupA(iItemParent)
				builderA.createGroupA(gParentA)
			}
		}

		val lG = x2(g0, lItem, 0, Nil)

		val lTipCleanableParent = if (iItemParent < 0) SortedSet[TipConfigL2]() else lGroupB(iItemParent).lTipCleanable
		val nScoreParent = if (iItemParent < 0) 0 else lnScore(iItemParent)
		val nItemsRemaining = lItem.size
		
		for (g <- lG) {
			builderB.tr_groupB(g, lTipCleanableParent) match {
				case Success(gB) =>
					val nScore = nScoreParent + gB.nScore
					val iItem = iItemParent + g.lItem.size
					if (lnScore(iItem) == 0 || nScore < lnScore(iItem)) {
						val nItemsAfter = nItemsRemaining - g.lItem.size
						lnScore(iItem) = nScore
						lGroupA(iItem) = g
						lGroupB(iItem) = gB
						val nHeuristic = {
							if (nItemsAfter == 0)
								0.0
							else {
								4 + // switch to aspirate
								2 + // a single aspirate batch
								4 + // switch to dispense
								(nItemsRemaining - g.lItem.size) // all remaining dispenses from the air
							}
						}
						val nTotalCostMin = nScore + nHeuristic
						queue += Score(iItem, nScore, nTotalCostMin)
					}
				case err =>
					println("err: " + err)
			}
		}
	}
	
	private def x2(g: GroupA, lItem: List[Item], nTips0: Int, acc: List[GroupA]): List[GroupA] = lItem match {
		case Nil =>
			if (g.lItem.isEmpty) acc
			else g :: acc
		case item :: rest =>
			builderA.addItemToGroup(g, item) match {
				case builderA.GroupSuccess(g2) =>
					val nTips = g2.mTipToLM.size
					val acc2 = if (g.lItem.isEmpty || nTips == nTips0) acc else g :: acc
					x2(g2, rest, nTips, acc2)
				case _ =>
					println("g*:"+g)
					// FIXME: for debug only
					//Seq[Int]().head
					acc
			}
	}
	
	private def ScoreOrdering = new Ordering[Score] {
		def compare(a: Score, b: Score): Int = -a.nTotalCostMin.compare(b.nTotalCostMin)
	}
}
