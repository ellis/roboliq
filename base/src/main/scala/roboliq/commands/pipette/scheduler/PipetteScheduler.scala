package roboliq.commands.pipette.scheduler

import scala.collection.JavaConversions._
import scala.collection.immutable.SortedSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.PriorityQueue
import roboliq.core._
//import roboliq.commands._
import roboliq.commands.pipette._
//import roboliq.compiler._
import roboliq.devices.pipette._
import java.io.FileWriter


object PipetteScheduler {
	def createL3C(cmd: PipetteCmdBean, query: StateQuery, node: CmdNodeBean): Result[L3C_Pipette] = {
		val messages = new CmdMessageWriter(node)
		
		def opt[A](id: String, fn: String => Result[A]): Result[Option[A]] = {
			if (id == null) {
				Success(None)
			}
			else {
				for {res <- fn(id)} yield Some(res)
			}
		}
		
		def zipit(
			ls: List[List[Well2]],
			ld: List[Well2],
			lv: List[LiquidVolume],
			acc: List[Tuple3[List[Well2], Well2, LiquidVolume]]
		): List[Tuple3[List[Well2], Well2, LiquidVolume]] = {
			//println("zipit:", ls, ld, lv)
			val (s::ss, d::ds, v::vs) = (ls, ld, lv)
			val sdv = (s, d, v)
			val acc2 = sdv :: acc
			if (ss == Nil && ds == Nil && vs == Nil)
				acc2.reverse
			else {
				val ls2 = if (ss.isEmpty) ls else ss
				val ld2 = if (ds.isEmpty) ld else ds
				val lv2 = if (vs.isEmpty) lv else vs
				zipit(ls2, ld2, lv2, acc2)
			}
		}
		
		val mixSpec_? : Option[MixSpec] = if (cmd.postmix == null) None else Some(MixSpec.fromBean(cmd.postmix))
		val pipettePolicy_? : Option[PipettePolicy] = if (cmd.policy == null) None else Some(PipettePolicy.fromName(cmd.policy))
		val volumes_? : Option[List[LiquidVolume]] = if (cmd.volume == null) None else Some(cmd.volume.map(n => LiquidVolume.l(n)).toList)
		val tipOverrides_? = Some(new TipHandlingOverrides(
			replacement_? = if (cmd.tipReplacement == null) None else Some(TipReplacementPolicy.withName(cmd.tipReplacement)),
			washIntensity_? = None,
			allowMultipipette_? = if (cmd.allowMultipipette == null) None else Some(cmd.allowMultipipette),
			contamInside_? = None,
			contamOutside_? = None
		))
		println("tipOverrides_?: "+tipOverrides_?)

		for {
			tipModel_? <- opt(cmd.tipModel, query.findTipModel _)
			srcs_? <- opt(cmd.src, query.mapIdsToWell2Lists _)
			// TODO: disallow liquids in destination
			dests_? <- opt(cmd.dest, query.mapIdsToWell2Lists _)
		} yield {
			// If only one entry is given, use it as the default
			val srcDefault_? = srcs_?.filter(_.tail.isEmpty).map(_.head)
			val destDefault_? = dests_?.filter(_.tail.isEmpty).map(_.head)
			val volumeDefault_? = volumes_?.filter(_.tail.isEmpty).map(_.head)
			
			val lnLen = List(
				srcs_?.map(_.length).getOrElse(0),
				dests_?.map(_.length).getOrElse(0),
				volumes_?.map(_.length).getOrElse(0)
			)
			
			val bLengthsOk = lnLen.filter(_ != 1) match {
				case Nil => true
				case x :: xs => xs.forall(_ == x)
			}
			
			if (!bLengthsOk) {
				return Error("arrays must have equal lengths")
			}
		
			val lsdv = zipit(srcs_?.get, dests_?.get.flatten, volumes_?.get, Nil)
			
			val items = lsdv.map(svd => new Item(
				SortedSet(svd._1 : _*),
				svd._2,
				//volumes_?.get(0),
				svd._3,
				None,
				None
			))
			val args = new L3A_PipetteArgs(items, mixSpec_?, tipOverrides_?, pipettePolicy_?, tipModel_?)
			
			new L3C_Pipette(args)
		}
	}
}

class PipetteScheduler(
	val device: PipetteDevice,
	val ctx: ProcessorContext,
	val cmd: L3C_Pipette
) {
	private val lTipAll: SortedSet[Tip] = device.getTips.map(_.state(ctx.states).conf)
	private val builderA = new GroupABuilder(device, ctx, cmd)
	private val builderB = new GroupBBuilder(device, ctx)
	private var lnScore: Array[Double] = null
	private var lGroupA: Array[GroupA] = null
	private var lGroupB: Array[GroupB] = null
	case class Score(iItem: Int, nPathCost: Double, nTotalCostMin: Double)
	private val queue = new PriorityQueue[Score]()(ScoreOrdering)
	
	private val fw = new FileWriter("PipetteScheduler.txt", false)//true)
	fw.write(cmd.toDebugString)
	fw.write("\n")
	
	def translate(): Result[Seq[CmdBean]] = {
		val states = ctx.states.toBuilder
		val res = for {
			items0 <- builderA.filterItems(cmd.args.items)
			mItemToState0 = builderA.getItemStates(items0)
			pair <- builderA.tr1Items(items0, mItemToState0)
			(items, mItemToState, mLM) = pair 
		} yield {
			if (items.isEmpty)
				return Success(Nil)
			
			val nTips = lTipAll.size
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
					x1(lItem, mItemToState, mLM, iItemParent)
				}
			}
			/*println("lnScore: "+lnScore.toList)
			lGroupB.zipWithIndex.foreach(pair => {
				val (gB, i) = pair
				//print((i+1).toString+":\t")
				if (gB == null) println("_")
				else {
					val nItems = gB.lItem.size
					val iItemParent = i - nItems
					val nScoreParent = if (iItemParent < 0) 0 else lnScore(iItemParent)
					val nScoreTotal = nScoreParent + gB.nScore
					/*println(nScoreTotal+"\t"
						+(iItemParent+2)+"->"+(i+1)+"\t"
						+gB.nScore+"\t"+gB.lItem.head.dest)*/
				}
			})
			println("lGroupA: "+lGroupA.toList)
			println("lGroupB: "+lGroupB.toList)
			*/
			
			// Reconstruct the optimal path
			logPathA(nItems - 1, Nil)
			//printPathA(nItems - 1, Nil)
			val lB = getPath(nItems - 1, Nil)
			//lB.foreach(gB => { println("gB:"); println(gB) })
			val rB = lB.reverse
			val lmTipToClean = optimizeCleanSpec(rB, Map(), Nil)
			//println("lmTipToClean: "+lmTipToClean)
			
			val lClean = lmTipToClean.map(toCleanCommand)
			//println("lClean: "+lClean)
			
			val lCommand = getCommands(lClean, lB, lGroupA.last.states1)
			lCommand
		}
		fw.close()
		res
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

		val lG0 = x2R(lItem, iItemParent, List(g0)).reverse.tail // First one discarded because it's the empty g0
		val lGR = filterGroupAsR(lG0, Nil)
		// Remove last group if its last item is in a different column than the second-to-last and in the same column as the next pending item
		val lGR2 = if (!lGR.isEmpty) {
			val gLast = lGR.head
			val lItemNext = lItem.drop(gLast.lItem.size)
			if (!lItemNext.isEmpty) {
				// last two items in the group
				val lDest = gLast.lItem.map(_.dest).reverse.take(2).reverse
				val wellGroups = WellGroup(gLast.states0, lDest).splitByCol
				val bItemsInSameCol = (wellGroups.size == 1)
				if (!bItemsInSameCol) {
					val itemNext = lItemNext.head
					val wellGroups2 = WellGroup(gLast.states0, lDest ++ Seq(itemNext.dest)).splitByCol
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
		/*// FIXME: for debug only
		if (lG0.length >= 11) {
			println("from: "+(iItemParent+2))
			println("item counts A: "+lG0.map(_.lItem.size))
			println("item counts B: "+lGR.map(_.lItem.size))
			println("item counts C: "+lGR2.map(_.lItem.size))
		}
		// ENDFIX*/
		//println("lG:")
		//println(lG)
		//println("from well: "+lItem.head.dest.index)
		//println("well counts: "+lG.map(_.lItem.size))

		val lTipCleanableParent = if (iItemParent < 0) SortedSet[Tip]() else lGroupB(iItemParent).lTipCleanable
		val nScoreParent = if (iItemParent < 0) 0 else lnScore(iItemParent)
		val nItemsRemaining = lItem.size
		
		for (g <- lG) {
			builderB.tr_groupB(g, lTipCleanableParent) match {
				case Success(gB) =>
					val nScore = nScoreParent + gB.nScore
					val iItem = iItemParent + g.lItem.size
					fw.write("SCORE: "+iItem.toString+": "+iItemParent+"+"+g.lItem.size+"\t+"+gB.nScore+" -> "+nScore+"\n")
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
	
	private def x2R(lItem: List[Item], iItemParent: Int, acc: List[GroupA]): List[GroupA] = lItem match {
		case Nil => acc
		case item :: rest =>
			val iItem = iItemParent + acc.length + 1
			// FIXME: for debug only
			if (iItemParent == 23 && iItem == 27) {
				iItemParent.toString()
			}
			// ENDIF
			fw.write(item.toString()+"\n")
			builderA.addItemToGroup(acc.head, item) match {
				case err: builderA.GroupError =>
					println("Error in x2R:")
					println(err)
					fw.write("error: "+err.lsError+"\n\n")
					acc
				case stop: builderA.GroupStop =>
					fw.write("stop: "+stop.sReason+"\n")
					fw.write(stop.groupA.toString+"\n\n")
					//println("stop:"+stop);
					//println("prev:"+acc.head)
					acc
				case builderA.GroupSuccess(g) =>
					fw.write("GROUP: "+iItemParent+" >-< "+g.lItem.size+" = @"+(iItemParent+g.lItem.size)+"\n")
					fw.write(g.toString()+"\n\n")
					x2R(rest, iItemParent, g :: acc)
			}
	}
	
	private def filterGroupAsR(lG: List[GroupA], acc: List[GroupA]): List[GroupA] = {
		lG match {
			case Nil => Nil
			// Always add last item to list
			case g :: Nil => g :: acc
			case g :: (lG2 @ (g2 :: _)) =>
				val acc2 = {
					val wellGroup = WellGroup(g2.states0, g2.lItem.takeRight(2).map(_.dest).distinct).splitByAdjacent()
					// If new item is not adjacent to previous one, save the preceding group
					if (wellGroup.size > 1) {
						//println("keep:")
						//println(g)
						// FIXME: for debug only
						//val iWell0 = g2.lItem.head.dest.index
						//val iWell1 = g2.lItem.last.dest.index
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
	
	private def logPathA(iItem: Int, acc: List[Int]) {
		if (iItem < 0) {
			acc.foreach(i => fw.write("PATH: "+i+"\n"))
		}
		else {
			val gA = lGroupA(iItem)
			if (gA == null) {
				println("lGroupA("+iItem+") == null")
			}
			else
				logPathA(iItem - gA.lItem.size, iItem :: acc)
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
		mTipToCleanPending: Map[Tip, CleanSpec2],
		acc: List[Map[Tip, CleanSpec2]]
	): List[Map[Tip, CleanSpec2]] = {
		rB match {
			case Nil => acc
			case gB :: rest =>
				val (mTipToClean, mTipToCleanPending2) = {
					if (gB.cleans.isEmpty)
						(Map[Tip, CleanSpec2](), mTipToCleanPending ++ gB.precleans)
					else
						(gB.cleans ++ mTipToCleanPending, gB.precleans)
				}
				optimizeCleanSpec(rest, mTipToCleanPending2, mTipToClean :: acc)
		}
	}

	/*
	private def getCommandList(gB: GroupB, mTipToClean: Map[Tip, CleanSpec2]): List[Command] = {
		
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
	
	private def toCleanCommand(mTipToClean: Map[Tip, CleanSpec2]): Seq[CmdBean] = {
		val mTipToModel = new HashMap[Tip, Option[TipModel]]
		val mTipToWash = new HashMap[Tip, WashSpec]
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
		
		val lReplace = Seq[CmdBean]() /* FIXME: {
			if (mTipToModel.isEmpty) Seq()
			else {
				val items = mTipToModel.toSeq.sortBy(_._1).map(pair => new L3A_TipsReplaceItem(pair._1, pair._2))
				Seq(L3C_TipsReplace(items))
			}
		}*/
		
		val lWash = {
			if (mTipToWash.isEmpty) Seq()
			else {
				val llTip = device.batchCleanTips(SortedSet(mTipToWash.keys.toSeq : _*))
				llTip.flatMap(lTip => {
					val intensity = lTip.foldLeft(WashIntensity.None)((acc, tip) => {
						val spec = mTipToWash(tip)
						WashIntensity.max(acc, spec.washIntensity)
					})
					val bean = new TipsWashCmdBean
					bean.tips = lTip.toList.map(_.id)
					bean.intensity = intensity.toString()
					Some(bean)
				})
			}
		}
		
		//println("mTipToClean: "+mTipToClean)
		//println("lWash: "+lWash)

		lReplace ++ lWash
	}
	
	private def getCommands(lClean: List[Seq[CmdBean]], lB: List[GroupB], statesLast: RobotState): Seq[CmdBean] = {
		val lCommand0 = (lClean zip lB).flatMap(pair => pair._1 ++ pair._2.lPremix ++ pair._2.lAspirate ++ pair._2.lDispense ++ pair._2.lPostmix)
		//println("lCommand0:")
		//lCommand0.foreach(cmd => println(cmd.toDebugString))
		
		val lCommand = lCommand0 ++ finalClean(statesLast)
		//println("lCommand:")
		//lCommand.foreach(cmd => println(cmd.toDebugString))
		lCommand
	}
	
	private def finalClean(states: StateMap): Seq[CmdBean] = {
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
	
	private def createWashSpec(states: StateMap, tip: Tip, intensity: WashIntensity.Value): WashSpec = {
		val tipState = tip.state(states)
		new WashSpec(intensity, tipState.contamInside, tipState.contamOutside)
	}
}
