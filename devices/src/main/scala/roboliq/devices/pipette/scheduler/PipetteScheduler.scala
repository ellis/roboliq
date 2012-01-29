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
	val ctx: CompilerContextL3,
	val cmd: L3C_Pipette
) {
	private val lTipAll: SortedSet[TipConfigL2] = device.config.tips.map(_.state(ctx.states).conf)
	private val builderA = new GroupABuilder(device, ctx, cmd)
	private val builderB = new GroupBBuilder(device, ctx)
	private var lnScore: Array[Double] = null
	private var lGroupA: Array[GroupA] = null
	private var lGroupB: Array[GroupB] = null
	case class Score(iItem: Int, nPathCost: Double, nTotalCostMin: Double)
	private val queue = new PriorityQueue[Score]()(ScoreOrdering)
	
	def x(): Result[Seq[Command]] = {
		val states = new StateBuilder(ctx.states)
		for {
			items0 <- builderA.filterItems(cmd.args.items)
			mItemToState0 = builderA.getItemStates(items0)
			pair <- builderA.tr1Items(items0, mItemToState0)
			(items, mItemToState, mLM) = pair 
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
				println("score: "+score)
				val iItemParent = score.iItem
				bDone = (iItemParent == nItems - 1)
				if (!bDone) {
					val lItem = lItemAll.drop(iItemParent + 1)
					x1(lItem, mItemToState, mLM, iItemParent)
				}
			}
			println("lnScore: "+lnScore.toList)
			lGroupB.zipWithIndex.foreach(pair => {
				val (gB, i) = pair
				print((i+1).toString+":\t")
				if (gB == null) println("_")
				else {
					val nItems = gB.lItem.size
					val iItemParent = i - nItems
					val nScoreParent = if (iItemParent < 0) 0 else lnScore(iItemParent)
					val nScoreTotal = nScoreParent + gB.nScore
					println(nScoreTotal+"\t"
						+(iItemParent+2)+"->"+(i+1)+"\t"
						+gB.nScore+"\t"+gB.lItem.head.dest)
				}
			})
			//println("lGroupA: "+lGroupA.toList)
			//println("lGroupB: "+lGroupB.toList)
			
			// Reconstruct the optimal path
			printPathA(nItems - 1, Nil)
			val lB = getPath(nItems - 1, Nil)
			lB.foreach(gB => { println("gB:"); println(gB) })
			val rB = lB.reverse
			val lmTipToClean = optimizeCleanSpec(rB, Map(), Nil)
			//println("lmTipToClean: "+lmTipToClean)
			
			val lClean = lmTipToClean.map(toCleanCommand)
			//println("lClean: "+lClean)
			
			val lCommand = getCommands(lClean, lB, lGroupA.last.states1)
			lCommand
		}
	}
	
	private def x1(lItem: List[Item], mItemToState: Map[Item, ItemState], mLM: Map[Item, LM], iItemParent: Int) {
		// New group with 0 items
		val g0 = {
			if (iItemParent < 0)
				builderA.createGroupA(ctx.states, mItemToState, mLM)
			else {
				val gParentA = lGroupA(iItemParent)
				builderA.createGroupA(gParentA)
			}
		}

		val lG0 = x2R(lItem, List(g0)).reverse.tail // First one discarded because it's the empty g0
		val lGR = filterGroupAsR(lG0, Nil)
		// Remove last group if its last item is in a different column than the second-to-last and in the same column as the next pending item
		val lGR2 = if (!lGR.isEmpty) {
			val gLast = lGR.head
			val lItemNext = lItem.drop(gLast.lItem.size)
			if (!lItemNext.isEmpty) {
				// last two items in the group
				val lDest = gLast.lItem.map(_.dest).reverse.take(2).reverse
				val wellGroups = WellGroup(lDest).splitByCol
				val bItemsInSameCol = (wellGroups.size == 1)
				if (!bItemsInSameCol) {
					val itemNext = lItemNext.head
					val wellGroups2 = WellGroup(lDest ++ Seq(itemNext.dest)).splitByCol
					val bNextItemInDifferentCol = (wellGroups.size < wellGroups2.size)
					if (bNextItemInDifferentCol) {
						lGR
					}
					else {
						lGR.tail
					}
				}
				else
					lGR
			}
			else
				lGR
		}
		else {
			lGR
		}
		val lG = lGR2.reverse
		// FIXME: for debug only
		//if (lG0.length >= 11) {
			println("from: "+(iItemParent+2))
			println("item counts A: "+lG0.map(_.lItem.size))
			println("item counts B: "+lGR.map(_.lItem.size))
			println("item counts C: "+lGR2.map(_.lItem.size))
		//}
		// ENDFIX
		//println("lG:")
		//println(lG)
		println("from well: "+lItem.head.dest.index)
		println("well counts: "+lG.map(_.lItem.size))

		val lTipCleanableParent = if (iItemParent < 0) SortedSet[TipConfigL2]() else lGroupB(iItemParent).lTipCleanable
		val nScoreParent = if (iItemParent < 0) 0 else lnScore(iItemParent)
		val nItemsRemaining = lItem.size
		
		for (g <- lG) {
			builderB.tr_groupB(g, lTipCleanableParent) match {
				case Success(gB) =>
					val nScore = nScoreParent + gB.nScore
					val iItem = iItemParent + g.lItem.size
					if (lnScore(iItem) == 0 || nScore <= lnScore(iItem)) {
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
	
	private def x2R(lItem: List[Item], acc: List[GroupA]): List[GroupA] = lItem match {
		case Nil => acc
		case item :: rest =>
			builderA.addItemToGroup(acc.head, item) match {
				case err: builderA.GroupError =>
					println("Error in x2R:")
					println(err)
					acc
				case stop: builderA.GroupStop =>
					//println("stop:"+stop);
					//println("prev:"+acc.head)
					acc
				case builderA.GroupSuccess(g) =>
					x2R(rest, g :: acc)
			}
	}
	
	private def filterGroupAsR(lG: List[GroupA], acc: List[GroupA]): List[GroupA] = {
		lG match {
			case Nil => Nil
			// Always add last item to list
			case g :: Nil => g :: acc
			case g :: (lG2 @ (g2 :: _)) =>
				val acc2 = {
					val wellGroup = WellGroup(g2.lItem.takeRight(2).map(_.dest).distinct).splitByAdjacent()
					// If new item is not adjacent to previous one, save the preceding group
					if (wellGroup.size > 1) {
						//println("keep:")
						//println(g)
						// FIXME: for debug only
						val iWell0 = g2.lItem.head.dest.index
						val iWell1 = g2.lItem.last.dest.index
						// ENDFIX
						g :: acc
					}
					else
						acc
				}
				filterGroupAsR(lG2, acc2)
		}
	}
	
	private def ScoreOrdering = new Ordering[Score] {
		//def compare(a: Score, b: Score): Int = -a.nTotalCostMin.compare(b.nTotalCostMin)
		def compare(a: Score, b: Score): Int = -a.iItem.compare(b.iItem)
	}
	
	/**
	 * Gets the path of GroupB objects from start to finish
	 */
	private def getPath(iItem: Int, acc: List[GroupB]): List[GroupB] = {
		if (iItem < 0)
			acc
		else {
			//println("gA: "+lGroupA(iItem))
			val gB = lGroupB(iItem)
			if (gB == null) {
				println("lGroupA: "+lGroupA.toList)
				println(iItem, acc)
				Seq().head // This is here to force a crash!
				Nil // ERROR!
			}
			else
				getPath(iItem - gB.lItem.size, gB :: acc)
		}
	}
	
	private def printPathA(iItem: Int, acc: List[GroupA]) {
		if (iItem < 0) {
			println("GroupA Path:")
			acc.foreach(println)
		}
		else {
			val gA = lGroupA(iItem)
			if (gA == null) {
				println("lGroupA("+iItem+") == null")
			}
			else
				printPathA(iItem - gA.lItem.size, gA :: acc)
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
				val llTip = device.batchCleanTips(SortedSet(mTipToWash.keys.toSeq : _*))
				llTip.flatMap(lTip => {
					val intensity = lTip.foldLeft(WashIntensity.None)((acc, tip) => {
						val spec = mTipToWash(tip)
						WashIntensity.max(acc, spec.washIntensity)
					})
					val items = lTip.toSeq.map(tip => {
						val spec = mTipToWash(tip)
						new L3A_TipsWashItem(tip, spec.contamInside, spec.contamOutside)
					})
					if (items.isEmpty) None else Some(L3C_TipsWash(items, intensity))
				})
			}
		}
		
		println("mTipToClean: "+mTipToClean)
		println("lWash: "+lWash)

		lReplace ++ lWash
	}
	
	private def getCommands(lClean: List[Seq[Command]], lB: List[GroupB], statesLast: RobotState): Seq[Command] = {
		val lCommand0 = (lClean zip lB).flatMap(pair => pair._1 ++ pair._2.lPremix ++ pair._2.lAspirate ++ pair._2.lDispense ++ pair._2.lPostmix)
		//println("lCommand0:")
		//lCommand0.foreach(cmd => println(cmd.toDebugString))
		
		val lCommand = lCommand0 ++ finalClean(statesLast)
		//println("lCommand:")
		//lCommand.foreach(cmd => println(cmd.toDebugString))
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
			val llTip = device.batchCleanTips(lTipAll)
			llTip.flatMap(lTip => {
				val intensity = lTip.foldLeft(WashIntensity.None)((acc, tip) => WashIntensity.max(acc, tip.state(states).cleanDegreePending))
				val items = lTip.toSeq.map(tip => tip -> createWashSpec(states, tip, intensity))
				toCleanCommand(items.map(pair => pair._1-> WashSpec2(pair._1, pair._2)).toMap)
			})
		}
	}
	
	private def createWashSpec(states: StateMap, tip: TipConfigL2, intensity: WashIntensity.Value): WashSpec = {
		val tipState = tip.state(states)
		new WashSpec(intensity, tipState.contamInside, tipState.contamOutside)
	}
}
