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
	private val lTipAll: SortedSet[TipConfigL2] = device.config.tips.map(_.state(ctx.states).conf)
	private val builderA = new GroupABuilder(device, ctx)
	private val builderB = new GroupBBuilder(device, ctx)
	private var lnScore: Array[Double] = null
	private var lGroupA: Array[GroupA] = null
	private var lGroupB: Array[GroupB] = null
	case class Score(iItem: Int, nPathCost: Double, nTotalCostMin: Double)
	private val queue = new PriorityQueue[Score]()(ScoreOrdering)
	
	def x(cmd: L3C_Pipette): Result[Seq[Command]] = {
		for {
			items <- builderA.filterItems(cmd.args.items)
			mLM <- builderA.tr1Items(items)
		} yield {
			if (items.isEmpty)
				return Success(Nil)
			
			val nTips = device.config.tips.size
			val nItems = items.size // TODO: need to break up large pipetting items into multiple items
			lnScore = new Array[Double](nItems)
			lGroupA = new Array[GroupA](nItems)
			lGroupB = new Array[GroupB](nItems)
			queue.clear
			queue += Score(-1, 0, 0)
			val lItemAll = items.toList
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
			
			// Reconstruct the optimal path
			val lB = getPath(nItems - 1, Nil)
			val rB = lB.reverse
			val lmTipToClean = optimizeCleanSpec(rB, Map(), Nil)
			println("lmTipToClean: "+lmTipToClean)
			
			val lClean = lmTipToClean.map(toCleanCommand)
			println("lClean: "+lClean)
			
			val lCommand = getCommands(lClean, lB, lGroupA.last.states1)
			lCommand
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
						// Calculate lower bounds on cost to finish pipetting after this group
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
	
	/**
	 * Gets the path of GroupB objects from start to finish
	 */
	private def getPath(iItem: Int, acc: List[GroupB]): List[GroupB] = {
		if (iItem < 0)
			acc
		else {
			println("gA: "+lGroupA(iItem))
			val gB = lGroupB(iItem)
			if (gB == null)
				Nil // ERROR!
			else
				getPath(iItem - gB.lItem.size, gB :: acc)
		}
	}
	
	private def optimizeCleanSpec(
		rB: List[GroupB],
		mTipToCleanPending: Map[TipConfigL2, CleanSpec2],
		acc: List[Map[TipConfigL2, CleanSpec2]]
	): List[Map[TipConfigL2, CleanSpec2]] = {
		rB match {
			case Nil => acc
			case gB :: rest =>
				val (mTipToClean, mTipToCleanPending2) = {
					if (gB.cleans.isEmpty)
						(Map[TipConfigL2, CleanSpec2](), mTipToCleanPending ++ gB.precleans)
					else
						(gB.cleans ++ mTipToCleanPending, gB.precleans)
				}
				optimizeCleanSpec(rest, mTipToCleanPending2, mTipToClean :: acc)
		}
	}

	/*
	private def getCommandList(gB: GroupB, mTipToClean: Map[TipConfigL2, CleanSpec2]): List[Command] = {
		
			case TipsDrop(tips) =>
				Seq(L3C_TipsDrop(tips))
				
			case TipsGet(mapTipToModel) =>
				val items = mapTipToModel.toSeq.sortBy(_._1).map(pair => new L3A_TipsReplaceItem(pair._1, Some(pair._2)))
				Seq(L3C_TipsReplace(items))
			
			case TipsWash(mapTipToSpec) =>
				val intensity = mapTipToSpec.values.foldLeft(WashIntensity.None) { (acc, spec) => WashIntensity.max(acc, spec.washIntensity) }
				val items = mapTipToSpec.toSeq.sortBy(_._1).map(pair => new L3A_TipsWashItem(pair._1, pair._2.contamInside, pair._2.contamOutside))
				if (items.isEmpty) Seq() else Seq(L3C_TipsWash(items, intensity))
	*/
	
	private def toCleanCommand(mTipToClean: Map[TipConfigL2, CleanSpec2]): Seq[Command] = {
		val mTipToModel = new HashMap[TipConfigL2, Option[TipModel]]
		val mTipToWash = new HashMap[TipConfigL2, WashSpec]
		for ((tip, cleanSpec) <- mTipToClean) {
			cleanSpec match {
				case ReplaceSpec2(tip, model) =>
					mTipToModel += (tip -> Some(model))
				case WashSpec2(tip, spec) =>
					mTipToWash(tip) = spec
				case DropSpec2(tip) =>
					mTipToModel += (tip -> None)
			}
		}
		
		val lReplace = {
			if (mTipToModel.isEmpty) Seq()
			else {
				val items = mTipToModel.toSeq.sortBy(_._1).map(pair => new L3A_TipsReplaceItem(pair._1, pair._2))
				Seq(L3C_TipsReplace(items))
			}
		}
		
		val lWash = {
			if (mTipToWash.isEmpty) Seq()
			else {
				val intensity = mTipToWash.values.foldLeft(WashIntensity.None) { (acc, spec) => WashIntensity.max(acc, spec.washIntensity) }
				val items = mTipToWash.toSeq.sortBy(_._1).map(pair => new L3A_TipsWashItem(pair._1, pair._2.contamInside, pair._2.contamOutside))
				if (items.isEmpty) Seq() else Seq(L3C_TipsWash(items, intensity))
			}
		}

		lReplace ++ lWash
	}
	
	private def getCommands(lClean: List[Seq[Command]], lB: List[GroupB], statesLast: RobotState): Seq[Command] = {
		val lCommand0 = (lClean zip lB).flatMap(pair => pair._1 ++ pair._2.lAspirate ++ pair._2.lDispense)
		//println("lCommand0:")
		//lCommand0.foreach(cmd => println(cmd.toDebugString))
		
		val lCommand = lCommand0 ++ finalClean(statesLast)
		println("lCommand:")
		lCommand.foreach(cmd => println(cmd.toDebugString))
		lCommand
	}
	
	private def finalClean(states: StateMap): Seq[Command] = {
		val tipOverrides = TipHandlingOverrides()
		if (device.areTipsDisposable) {
			tipOverrides.replacement_? match {
				case Some(TipReplacementPolicy.KeepAlways) =>
					Seq()
				case _ =>
					toCleanCommand((lTipAll.toSeq).map(tip => tip -> DropSpec2(tip)).toMap)
			}
		}
		else {
			//println("finalClean:")
			//tips.toSeq.foreach(tip => println("state: "+tip.state(states)+", pending: "+tip.state(states).cleanDegreePending))
			val intensity = lTipAll.foldLeft(WashIntensity.None)((acc, tip) => WashIntensity.max(acc, tip.state(states).cleanDegreePending))
			val items = lTipAll.toSeq.map(tip => tip -> createWashSpec(states, tip, intensity))
			//println("items: "+items)
			toCleanCommand(items.map(pair => pair._1-> WashSpec2(pair._1, pair._2)).toMap)
		}
	}
	
	private def createWashSpec(states: StateMap, tip: TipConfigL2, intensity: WashIntensity.Value): WashSpec = {
		val tipState = tip.state(states)
		new WashSpec(intensity, tipState.contamInside, tipState.contamOutside)
	}
}
