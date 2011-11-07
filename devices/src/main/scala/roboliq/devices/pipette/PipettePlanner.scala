package roboliq.devices.pipette

import scala.collection.immutable.SortedSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.Queue

import roboliq.common._
import roboliq.commands._
import roboliq.commands.pipette._
import roboliq.compiler._


class PipettePlanner(
	val device: PipetteDevice,
	val ctx: CompilerContextL3
) {
	private class TipState(val tip: TipConfigL2) {
		var liquid: Liquid = null
		var nVolume: Double = 0
	}
	
	type Item = L3A_PipetteItem
	case class LM(liquid: Liquid, tipModel: TipModel) {
		override def toString: String = {
			"LM("+liquid.getName()+", "+tipModel.id+")"
		}
	}
	case class LMData(nTips: Int, nVolumeTotal: Double, nVolumeCurrent: Double)
	case class GroupZ(
		mLM: Map[Item, LM],
		states0: RobotState,
		tipBindings0: Map[TipConfigL2, LM],
		lItem: Seq[Item],
		lLM: Seq[LM],
		mLMToItems: Map[LM, Seq[Item]],
		mLMData: Map[LM, LMData],
		mLMTipCounts: Map[LM, Int],
		mLMToTips: Map[LM, SortedSet[TipConfigL2]],
		mTipToLM: Map[TipConfigL2, LM],
		mDestToTip: Map[Item, TipConfigL2],
		mTipToVolume: Map[TipConfigL2, Double],
		mTipToCleanSpec: Map[TipConfigL2, WashSpec],
		lDispense: Seq[TipWellVolumePolicy],
		lAspirate: Seq[TipWellVolumePolicy],
		bClean: Boolean
	) {
		override def toString: String = {
			List(
				//"mLMToItems:\n"+mLMToItems.toSeq.map(pair => pair._1.toString + " -> " + L3A_PipetteItem.toDebugString(pair._2)).mkString("    ", "\n    ", ""),
				"lItem:\n    "+L3A_PipetteItem.toDebugString(lItem),
				lLM.map(lm => lm.toString + " -> " + mLMData(lm)).mkString("mLMData:\n    ", "\n    ", ""),
				lLM.map(lm => lm.toString + " -> " + mLMTipCounts(lm)).mkString("mLMTipCounts:\n    ", "\n    ", ""),
				lLM.map(lm => lm.toString + " -> " + mLMToTips(lm)).mkString("mLMToTips:\n    ", "\n    ", ""),
				//lItem.map(item => Command.getWellsDebugString(Seq(item.dest)) + " -> " + mDestToTip(item)).mkString("mDestToTip:\n    ", "\n    ", ""),
				"mTipToWashIntensity:\n    "+mTipToCleanSpec.mapValues(_.washIntensity),
				lDispense.map(twvpString).mkString("lDispense:\n    ", "\n    ", ""),
				lAspirate.map(twvpString).mkString("lAspirate:\n    ", "\n    ", "")
			).mkString("GroupZ(\n  ", "\n  ", ")\n")
		}
		
		private def twvpString(twvp: TipWellVolumePolicy): String = {
			List(twvp.tip, Command.getWellsDebugString(Seq(twvp.well)), twvp.nVolume, twvp.policy.id).mkString(", ")			
		}
	}
	
	sealed abstract class CleanSpec2 { val tip: TipConfigL2 }
	case class ReplaceSpec2(tip: TipConfigL2, model: TipModel) extends CleanSpec2
	case class WashSpec2(tip: TipConfigL2, spec: WashSpec) extends CleanSpec2
	case class DropSpec2(tip: TipConfigL2) extends CleanSpec2

	case class GroupB(
		lItem: Seq[Item],
		bClean: Boolean,
		precleans: Map[TipConfigL2, CleanSpec2],
		cleans: Map[TipConfigL2, CleanSpec2],
		lTipCleanable: SortedSet[TipConfigL2],
		premixes: Seq[TipWellVolume],
		lAspirate: Seq[L2C_Aspirate],
		lDispense: Seq[L2C_Dispense],
		postmixes: Seq[TipWellVolume]
	) {
		override def toString: String = {
			List(
				//"mLMToItems:\n"+mLMToItems.toSeq.map(pair => pair._1.toString + " -> " + L3A_PipetteItem.toDebugString(pair._2)).mkString("    ", "\n    ", ""),
				"lItem:\n    "+L3A_PipetteItem.toDebugString(lItem),
				precleans.map(_.toString).mkString("precleans:\n    ", "\n    ", ""),
				cleans.map(_.toString).mkString("cleans:\n    ", "\n    ", ""),
				"lTipCleanable:\n    "+lTipCleanable,
				lAspirate.map(_.toDebugString).mkString("lAspirate:\n    ", "\n    ", ""),
				lDispense.map(_.toDebugString).mkString("lDispense:\n    ", "\n    ", "")
			).mkString("GroupB(\n  ", "\n  ", ")\n")
		}
	}
	case class GroupC(
		nItems: Int,
		cmds: Seq[Command],
		nScore: Double
	)
	
	val lTipAll: SortedSet[TipConfigL2] = device.config.tips.map(_.state(ctx.states).conf)
	
	def chooseTipModels(items: Seq[Item]): Map[Liquid, TipModel] = {
		val mapLiquidToModels = new HashMap[Liquid, Seq[TipModel]]
		val lLiquidAll = new HashSet[Liquid]
		val lTipModelAll = new HashSet[TipModel]
		for (item <- items) {
			val liquid = item.srcs.head.state(ctx.states).liquid
			val destState = item.dest.state(ctx.states)
			val tipModels = device.getDispenseAllowableTipModels(liquid, item.nVolume, destState.nVolume)
			lLiquidAll += liquid
			lTipModelAll ++= tipModels
			mapLiquidToModels(liquid) = mapLiquidToModels.getOrElse(liquid, Seq()) ++ tipModels
		}
		val lTipModelOkForAll = lTipModelAll.filter(tipModel => mapLiquidToModels.forall(pair => pair._2.contains(tipModel)))
		if (!lTipModelOkForAll.isEmpty) {
			val tipModel = lTipModelOkForAll.head
			lLiquidAll.map(_ -> tipModel).toMap
		}
		else {
			val mapLiquidToModel = new HashMap[Liquid, TipModel]
			val lLiquidsUnassigned = lLiquidAll.clone()
			while (!lLiquidsUnassigned.isEmpty) {
				// find most frequently allowed tip type and assign it to all allowable items
				val mapModelToCount = new HashMap[TipModel, Int]
				for ((liquid, tipModels) <- mapLiquidToModels) {
					for (tipModel <- tipModels) {
						mapModelToCount(tipModel) = mapModelToCount.getOrElse(tipModel, 0) + 1
					}
				}
				val tipModel = mapModelToCount.toList.sortBy(pair => pair._2).head._1
				val liquids = lLiquidsUnassigned.filter(liquid => mapLiquidToModels(liquid).contains(tipModel))
				mapLiquidToModel ++= liquids.map(_ -> tipModel)
				lLiquidsUnassigned --= liquids
			}
			mapLiquidToModel.toMap
		}
	}
	
	/** For each item, find the source liquid and choose a tip model */
	def tr1Layers(layers: Seq[Seq[Item]]): Result[Map[Item, LM]] = {
		Result.flatMap(layers)(items => {
			val mapLiquidToTipModel = chooseTipModels(items)
			tr1Items(items).map(_.toSeq)
		}).map(_.toMap)
	}
	
	/** For each item, find the source liquid and choose a tip model */
	def tr1Items(items: Seq[Item]): Result[Map[Item, LM]] = {
		val mapLiquidToTipModel = chooseTipModels(items)
		val mLM = items.map(item => {
			val liquid = item.srcs.head.state(ctx.states).liquid
			val tipModel = mapLiquidToTipModel(liquid)
			(item, LM(liquid, tipModel))
		}).toMap
		Success(mLM)
	}
	
	// TODO: After getting back mLM: Map[Item, LM], partition any items which require more volume than the TipModel can hold
	
	// Return:
	// - Error
	// - Can't continue
	// - Continue with new field
	
	sealed abstract class GroupResult {
		def flatMap(f: GroupZ => GroupResult): GroupResult
		def map(f: GroupZ => GroupZ): GroupResult
		def isError: Boolean = false
		def isSuccess: Boolean = false
		def isStop: Boolean = false
		
		final def >>=(f: GroupZ => GroupResult): GroupResult = flatMap(f)
		def foreach(f: GroupZ => GroupZ): Unit = {  
			map(f)  
			()
		}
	}
	case class GroupError(groupZ: GroupZ, lsError: Seq[String]) extends GroupResult {
		def flatMap(f: GroupZ => GroupResult): GroupResult = this
		def map(f: GroupZ => GroupZ): GroupResult = this
		override def isError: Boolean = true
	}
	case class GroupSuccess(groupZ: GroupZ) extends GroupResult {
		def flatMap(f: GroupZ => GroupResult): GroupResult = f(groupZ)
		def map(f: GroupZ => GroupZ): GroupResult = GroupSuccess(f(groupZ))
		override def isSuccess: Boolean = true
	}
	case class GroupStop(groupZ: GroupZ) extends GroupResult {
		def flatMap(f: GroupZ => GroupResult): GroupResult = this
		def map(f: GroupZ => GroupZ): GroupResult = this
		override def isStop: Boolean = true
	}

	def createGroupZ(
		states0: RobotState,
		mLM: Map[Item, LM]
	): GroupResult = {
		val groupZ0 = GroupZ(mLM, states0, Map(), Nil, Nil, Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Nil, Nil, false)
		GroupSuccess(groupZ0)
	}
	
	def addItemToGroup(
		g0: GroupZ,
		item: L3A_PipetteItem
	): GroupResult = {
		for {
			g <- GroupSuccess(g0) >>=
				updateGroupZ2_mLMData(item) >>= 
				updateGroupZ3_mLMTipCounts >>=
				updateGroupZ4_mLMToTips >>=
				updateGroupZ5_mDestToTip >>=
				updateGroupZ6_mTipToVolume >>=
				updateGroupZ7_lDispense >>=
				updateGroupZ8_lAspirate
		} yield {
			g
		}
	}

	/** Add the item's volume to mLMData to keep track of how many tips are needed for each LM */
	def updateGroupZ2_mLMData(item: Item)(g0: GroupZ): GroupResult = {
		val lItem = g0.lItem ++ Seq(item)
		// Get a list of LMs in the order defined by lItem
		val lLM = lItem.map(g0.mLM).toList.distinct
		val mLMToItems = lItem.groupBy(g0.mLM)
		val lm = g0.mLM(item)
		if (item.nVolume > lm.tipModel.nVolume)
			GroupError(g0, Seq("pipette volume exceeds volume of tip: "+item))
		
		val data = g0.mLMData.get(lm) match {
			case None =>
				LMData(1, item.nVolume, item.nVolume)
			case Some(data) =>
				val nVolumeCurrent = data.nVolumeCurrent + item.nVolume
				val nVolumeTotal = data.nVolumeTotal + item.nVolume
				if (data.nVolumeCurrent == 0)
					LMData(data.nTips + 1, nVolumeTotal, nVolumeCurrent)
				else if (nVolumeCurrent <= lm.tipModel.nVolume)
					LMData(data.nTips, nVolumeTotal, nVolumeCurrent)
				else
					LMData(data.nTips + 1, nVolumeTotal, item.nVolume)
		}
		
		// TODO: if a source of item is in the list of previous destinations, return GroupStop(g0)
		
		GroupSuccess(g0.copy(
			lItem = lItem,
			lLM = lLM, 
			mLMToItems = mLMToItems,
			mLMData = g0.mLMData.updated(lm, data)
		))
	}
	
	// Choose number of tips per LM, and indicate whether we need to clean the tips first 
	def updateGroupZ3_mLMTipCounts(g0: GroupZ): GroupResult = {
		// for i = 1 to max diff between min and max tips needed for any LM:
		//   create map of tipModel -> sum for each LM with given tip model of math.min(max tips, min tips + i)
		//   if device can't accommodate those tip counts, break and use the previous successful count
		//   try to assign tips, respecting the constraints in tipBindings0
		//   if we can't, use the previous successful count
		// if no successful counts were found
		//   if tipBindings0 is not empty,
		//     call chooseTips() again, but with no tipBindings0 and indicate on return that a cleaning was required
		
		// for each LM: find max number of adjacent wells for aspirate/dispense
		val mLMToAdjacent: Map[LM, Int] = g0.mLMToItems.mapValues(countMaxAdjacentWells)
		// list of tip count ranges: (LM, nTipsMin, nTipsMap)
		val lLMRange1: Seq[Tuple3[LM, Int, Int]] = g0.mLMData.toSeq.map(pair => {
			val lm = pair._1
			val nTipsMin = pair._2.nTips
			// nTipsMax = max(min tips needed to hold total liquid volume, max number of adjacent wells which will be dispensed into)
			val nTipsMax = math.max(nTipsMin, mLMToAdjacent(lm))
			(lm, nTipsMin, nTipsMax)
		})
		// Account for tipBindings0 in addition to lLMRange
		val lLMRange2: Seq[Tuple3[LM, Int, Int]] =
			lLMRange1 ++ g0.tipBindings0.toSeq.filter(pair => !g0.mLMData.contains(pair._2)).map(pair => (pair._2, 1, 1))
		// Maximum number of tips we might like to add to the minimum required for any LM
		val nDiffMax = lLMRange2.foldLeft(0)((acc, tuple) => math.max(acc, tuple._3 - tuple._2))
		// Map of tipModel to the corresponding lLMRange2 items
		val mMToRange: Map[TipModel, Seq[Tuple3[LM, Int, Int]]] = lLMRange2.groupBy(_._1.tipModel)
		println("mLMToAdjacent: "+mLMToAdjacent)
		println("lLMRang2: "+lLMRange2)
		
		// Find out how many tips we can add to the minimum number required by each LM
		// for i = 1 to max diff between min and max tips needed for any LM:
		def loop1(nAdd: Int): Result[Int] = {
			//val mLMToCount = lLMRange2.map(range => math.min(range._2 + nAdd, range._3))
			// Calculate number of tips needed for each tipModel:
			//   map of tipModel -> sum over each LM (with given tip model) of math.min(max tips, min tips + i)
			val mMToCount = mMToRange.mapValues(l => l.foldLeft(0)((acc, range) => acc + math.min(range._2 + nAdd, range._3)))
			// if device can't accommodate those tip counts, break and use the previous successful count
			device.supportTipModelCounts(mMToCount) match {
				case Error(lsError) => Error(lsError)
				case Success(false) => Success(nAdd -1)
				case Success(true) =>
					if (nAdd >= nDiffMax)
						Success(nAdd)
					else
						loop1(nAdd + 1)
			}
		}

		val nAdd = loop1(0) match {
			case Error(lsError) => return GroupError(g0, lsError)
			case Success(n) => n
		}

		// if no successful counts were found, call chooseTips() again, but with no tipBindings0 and indicate on return that a cleaning was required
		if (nAdd < 0) {
			if (g0.tipBindings0.isEmpty)
				GroupStop(g0)
			else
				updateGroupZ3_mLMTipCounts(g0.copy(tipBindings0 = Map(), bClean = true))
		}
		else {
			GroupSuccess(g0.copy(
				// Number of tips per LM
				mLMTipCounts = lLMRange2.map(range => range._1 -> math.min(range._2 + nAdd, range._3)).toMap
			))
		}
	}
	
	/** Get the maximum number of adjacent wells */
	def countMaxAdjacentWells(lItem: Seq[Item]): Int = {
		val lWell = lItem.map(_.dest)
		WellGroup(lWell).splitByAdjacent().foldLeft(0)((acc, group) => math.max(acc, group.set.size))
	}
	
	def updateGroupZ4_mLMToTips(g0: GroupZ): GroupResult = {
		val lTipAll: SortedSet[TipConfigL2] = device.config.tips.map(_.state(g0.states0).conf)
		val lTipFree = HashSet(lTipAll.toSeq : _*)
		val mLMToBoundTips: Map[LM, Seq[TipConfigL2]] = g0.tipBindings0.toSeq.groupBy(_._2).mapValues(_.map(_._1))
		val llTip: Seq[SortedSet[TipConfigL2]] = g0.lLM.map(lm => {
			val nTipTotal = g0.mLMTipCounts(lm)
			val lTipBound = mLMToBoundTips.getOrElse(lm, Seq[TipConfigL2]())
			val lTipAvailable = SortedSet((lTipFree ++ lTipBound).toSeq : _*)
			println("lTipAvailable: "+lTipAvailable)
			device.assignTips(lTipAvailable, lm.tipModel, nTipTotal) match {
				case Error(lsError) => return GroupError(g0, lsError)
				case Success(lTip) =>
					println("lTip: "+lTip)
					lTipFree --= lTip
					lTip
			}
		})
		/*
		val tipsFree = HashSet((device.config.tips -- g0.tipBindings0.map(_._1.obj)).toSeq : _*)
		val lMToCount = g0.lLM.map(lm => lm.tipModel -> g0.mLMTipCounts(lm))
		//println("lMToCount: " + lMToCount)
		val llTip = device.assignTips(device.config.tips, lMToCount) match {
			case Error(lsError) => return GroupError(g0, lsError)
			case Success(x) => x.map(_.map(_.state(g0.states0).conf))
		}
		*/
		println("llTip: " + llTip)
		
		val mLMToTips = (g0.lLM zip llTip).toMap
		val mTipToLM = mLMToTips.flatMap(pair => pair._2.toSeq.map(_.state(g0.states0).conf -> pair._1)) 
		GroupSuccess(g0.copy(
			mLMToTips = mLMToTips,
			mTipToLM = mTipToLM
		))
	}
	
	def updateGroupZ5_mDestToTip(g0: GroupZ): GroupResult = {
		println("Z5: ", g0.lLM, g0.mLMToTips)
		val lDestToTip = g0.lLM.flatMap(lm => {
			val lItem = g0.mLMToItems(lm)
			val lTip = g0.mLMToTips(lm).map(_.state(ctx.states).conf)
			val lDest: SortedSet[WellConfigL2] = SortedSet(lItem.map(_.dest) : _*)
			val ltw = PipetteHelper.chooseTipWellPairsAll(ctx.states, lTip, lDest).flatten
			println(lTip, lDest, ltw)
			(lItem zip ltw).map(pair => pair._1 -> pair._2.tip)
		})
		GroupSuccess(g0.copy(
			mDestToTip = lDestToTip.toMap
		))
	}
	
	def updateGroupZ6_mTipToVolume(g0: GroupZ): GroupResult = {
		GroupSuccess(g0.copy(
			mTipToVolume = g0.mDestToTip.toSeq.groupBy(_._2).mapValues(_.foldLeft(0.0)((acc, pair) => acc + pair._1.nVolume)).toMap
		))
	}

	def updateGroupZ7_lDispense(g0: GroupZ): GroupResult = {
		val lTipEnteredCells = new HashSet[TipConfigL2]
		val mTipToLiquidGroups = new HashMap[TipConfigL2, LiquidGroup]
		val mTipToIntensity = new HashMap[TipConfigL2, WashIntensity.Value]
		val mDestToPolicy = new HashMap[Item, PipettePolicy]
		val builder = new StateBuilder(g0.states0)

		// Add wash intensity pending from previous pipetting operations
		mTipToIntensity ++= g0.mTipToLM.keys.map(tip => tip -> tip.state(g0.states0).cleanDegreePending)

		val lDispense = for (item <- g0.lItem) yield {
			val tip = g0.mDestToTip(item)
			val destState = item.dest.state(g0.states0)
			val liquid = g0.mLM(item).liquid
			val nVolume = item.nVolume
			val nVolumeDest = destState.nVolume
			val liquidDest = destState.liquid
			
			// TODO: allow for policy override
			val policy = device.getDispensePolicy(liquid, tip, nVolume, nVolumeDest) match {
				case None => return GroupError(g0, Seq("Could not find dispense policy for item "+item))
				case Some(p) => p
			}
			
			// TODO: allow for override via tipOverrides
			// Tips should be washed after entering a well with cells
			if (lTipEnteredCells.contains(tip))
				return GroupStop(g0)
			if (liquidDest.contaminants.contains(Contaminant.Cell))
				lTipEnteredCells += tip
				
			// TODO: allow for override via tipOverrides
			// LiquidGroups
			val liquidGroupDest = liquidDest.group
			val intensity = mTipToLiquidGroups.get(tip) match {
				case None =>
					mTipToLiquidGroups(tip) = liquidGroupDest
					liquidGroupDest.cleanPolicy.enter
				case Some(liquidGroup0) =>
					if (liquidGroupDest ne liquidGroup0) {
						// TODO: allow for override via tipOverrides
						// i.e. if overridden, set mTipToIntensity(tip) to max of two intensities
						return GroupStop(g0)
					}
					liquidGroupDest.cleanPolicy.enter
			}
			mTipToIntensity(tip) = WashIntensity.max(intensity, mTipToIntensity.getOrElse(tip, WashIntensity.None))
			
			item.dest.obj.stateWriter(builder).add(liquid, nVolume)
			mDestToPolicy(item) = policy
			
			new TipWellVolumePolicy(tip, item.dest, nVolume, policy)
		}
		
		val mTipToCleanSpec = mTipToIntensity.map(pair => {
			val (tip, intensity) = pair
			val tipState = tip.state(g0.states0)
			tip -> new WashSpec(intensity, tipState.contamInside, tipState.contamOutside)
		})

		GroupSuccess(g0.copy(
			mTipToCleanSpec = mTipToCleanSpec.toMap,
			lDispense = lDispense
		))
	}
	
	def updateGroupZ8_lAspirate(g0: GroupZ): GroupResult = {
		val lAspirate = g0.lLM.flatMap(lm => {
			val tips = g0.mLMToTips(lm)
			val lItem = g0.lItem.filter(item => g0.mLM(item) == lm)
			val srcs = SortedSet(lItem.flatMap(_.srcs) : _*)
			val lltw: Seq[Seq[TipWell]] = PipetteHelper.chooseTipSrcPairs(g0.states0, tips, srcs)
			val ltw = lltw.flatMap(identity)
			ltw.map(tw => {
				val policy_? = device.getAspiratePolicy(tw.tip.state(g0.states0), tw.well.state(g0.states0))
				if (policy_?.isEmpty)
					return GroupError(g0, Seq("Could not find aspirate policy for "+tw.tip+" and "+tw.well))
				new TipWellVolumePolicy(tw.tip, tw.well, g0.mTipToVolume(tw.tip), policy_?.get)
			})
		})
		GroupSuccess(g0.copy(
			lAspirate = lAspirate
		))
	}
	
	/**
	 * @param lTipCleanable0 tip which can potentially be washed at an earlier stage
	 */
	def tr_groupB(
		groupZ: GroupZ,
		lTipCleanable0: SortedSet[TipConfigL2]
	): Result[GroupB] = {
		val lAspirate = groupSpirateItems(groupZ, groupZ.lAspirate).map(items => L2C_Aspirate(items))
		val lDispense = groupSpirateItems(groupZ, groupZ.lDispense).map(items => L2C_Dispense(items))
		
		val mTipToModel = groupZ.mTipToLM.mapValues(_.tipModel)
		val cleans0 = groupZ.mTipToCleanSpec.map(pair => {
			val (tip, cleanSpec) = pair
			tip -> getCleanSpec2(groupZ.states0, TipHandlingOverrides(), mTipToModel, tip, cleanSpec)
		}).collect({ case (tip, Some(cleanSpec2)) => tip -> cleanSpec2 })
		
		val (precleans, cleans): Tuple2[Map[TipConfigL2, CleanSpec2], Map[TipConfigL2, CleanSpec2]] = {
			if ((cleans0.keySet -- lTipCleanable0).isEmpty)
				(cleans0, Map())
			else
				(Map(), cleans0)
		}
		
		// Indicate which tips could be cleaned earlier for the NEXT group
		val lTipCleanable: SortedSet[TipConfigL2] = {
			if (precleans.size + cleans.size == 0)
				lTipCleanable0
			else if (cleans.isEmpty)
				lTipCleanable0 -- precleans.keySet
			else {
				val lTipCleaning = SortedSet(cleans.keys.toSeq : _*)
				device.getOtherTipsWhichCanBeCleanedSimultaneously(lTipAll, lTipCleaning) -- cleans.keySet
			}
		}
			
		val groupB = GroupB(
			groupZ.lItem,
			groupZ.bClean,
			precleans = precleans,
			cleans = cleans,
			lTipCleanable = lTipCleanable,
			Nil,
			lAspirate = lAspirate,
			lDispense = lDispense,
			Nil
		)
		Success(groupB)
	}
	
	def groupSpirateItems(
		groupZ: GroupZ,
		lTwvp: Seq[TipWellVolumePolicy]
	): Seq[Seq[L2A_SpirateItem]] = {
		/*
		// Group by: tipModel, pipettePolicy
		val ma: Map[Tuple2[TipModel, PipettePolicy], Seq[TipWellVolumePolicy]]
			= lTwvp.groupBy(twvp => (groupZ.mTipToLM(twvp.tip).tipModel, twvp.policy))
		// Order of (tipModel, pipettePolicy)
		val lMP: Seq[Tuple2[TipModel, PipettePolicy]]
			= lTwvp.map(twvp => (groupZ.mTipToLM(twvp.tip).tipModel, twvp.policy)).distinct
		// TODO: Make sure that the order of dispenses to a given well always remains the same
		// Create list of list of spirate items
		lMP.map(mp => {
			ma(mp).map(twvp => new L2A_SpirateItem(twvp.tip, twvp.well, twvp.nVolume, twvp.policy))
		})
		*/
		
		val x = lTwvp.foldLeft(List[List[TipWellVolumePolicy]]())(groupSpirateItems_add(groupZ))
		val y = x.reverse.map(_.reverse)
		y.map(_.map(twvp => new L2A_SpirateItem(twvp.tip, twvp.well, twvp.nVolume, twvp.policy)))
		//val x1 = new ArrayBuffer[Seq[L2A_SpirateItem]]
		//var x2 = new ArrayBuffer[L2A_SpirateItem]
	}
	
	def groupSpirateItems_add(groupZ: GroupZ)(
		acc: List[List[TipWellVolumePolicy]],
		twvp: TipWellVolumePolicy
	): List[List[TipWellVolumePolicy]] = {
		acc match {
			case (xs @ List(x0, _*)) :: rest  =>
				val tipModel = groupZ.mTipToLM(twvp.tip).tipModel
				val tipModel0 = groupZ.mTipToLM(x0.tip).tipModel
				val bTipAlreadyUsed = xs.exists(twvp.tip eq _.tip)
				val bWellAlreadyVisited = xs.exists(twvp.well eq _.well)
				// TODO: check which device about whether items can be batched together
				if (tipModel.eq(tipModel0) && twvp.policy == x0.policy && !bTipAlreadyUsed && !bWellAlreadyVisited)
					(twvp::xs) :: rest
				else
					List(twvp) :: (xs :: rest)
			case _ =>
				List(List(twvp))
		}
	}

	protected def getCleanSpec2(
		states: StateMap,
		overrides: TipHandlingOverrides,
		mapTipToModel: Map[TipConfigL2, TipModel],
		tip: TipConfigL2,
		cleanSpec: WashSpec
	): Option[CleanSpec2] = {
		if (cleanSpec.washIntensity == WashIntensity.None) {
			None
		}
		else if (device.areTipsDisposable) {
			val tipState = tip.obj.state(states)
			val bReplace = tipState.model_?.isEmpty || (overrides.replacement_? match {
				case Some(TipReplacementPolicy.ReplaceAlways) => true
				case Some(TipReplacementPolicy.KeepBetween) => false
				case Some(TipReplacementPolicy.KeepAlways) => false
				case None => false
			})
			if (bReplace)
				Some(ReplaceSpec2(tip, mapTipToModel.getOrElse(tip, tipState.model_?.get)))
			else
				None
		}
		else {
			val tipState = tip.obj.state(states)
			if (cleanSpec.washIntensity > WashIntensity.None) Some(WashSpec2(tip, cleanSpec))
			else None
		}
	}
}
