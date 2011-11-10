package roboliq.devices.pipette.scheduler

import scala.collection.immutable.SortedSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import roboliq.common._
import roboliq.commands._
import roboliq.commands.pipette._
import roboliq.commands.pipette.{L3A_PipetteItem => Item}
import roboliq.compiler._
import roboliq.devices.pipette._


class GroupABuilder(
	val device: PipetteDevice,
	val ctx: CompilerContextL3,
	val cmd: L3C_Pipette
) {
	/**
	 * Remove items with nVolume < 0
	 */
	def filterItems(items: Seq[Item]): Result[Seq[Item]] = {
		Success(items.filter(_.nVolume > 0))
	}
	
	def chooseTipModels(items: Seq[Item]): Map[Liquid, TipModel] = {
		if (items.isEmpty)
			return Map()
			
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
				val mapModelToCount: Map[TipModel, Int] = getNumberOfLiquidsPerModel(mapLiquidToModels, lLiquidsUnassigned)
				// FIXME: for debug only
				if (mapModelToCount.isEmpty) {
					println("DEBUG:")
					println(items)
					println(lTipModelAll)
					println(lLiquidAll)
					println(mapLiquidToModels)
					println(lLiquidsUnassigned)
				}
				// ENDFIX
				val tipModel = mapModelToCount.toList.sortBy(pair => pair._2).head._1
				val liquids = lLiquidsUnassigned.filter(liquid => mapLiquidToModels(liquid).contains(tipModel))
				mapLiquidToModel ++= liquids.map(_ -> tipModel)
				// FIXME: for debug only
				println("mapModelToCount: "+mapModelToCount)
				if (liquids.isEmpty) {
					println(items)
					println(mapLiquidToModels)
					println("tipModel: "+tipModel)
					println(lLiquidAll)
					println(lTipModelAll)
					Seq().head
				}
				// ENDFIX
				lLiquidsUnassigned --= liquids
			}
			mapLiquidToModel.toMap
		}
	}
	
	private def getNumberOfLiquidsPerModel(
		mLiquidToModels: collection.Map[Liquid, Seq[TipModel]],
		lLiquidsUnassigned: collection.Set[Liquid]
	): Map[TipModel, Int] = {
		val seq: Seq[Tuple2[TipModel, Liquid]] = lLiquidsUnassigned.toSeq.flatMap(liquid => {
			val lTipModel = mLiquidToModels(liquid)
			lTipModel.map(_ -> liquid)
		})
		seq.groupBy(_._1).mapValues(_.size)
	}
	
	/** 
	 * For each item, find the source liquid and choose a tip model
	 */
	def tr1Layers(layers: Seq[Seq[Item]]): Result[Map[Item, LM]] = {
		Result.flatMap(layers)(items => {
			val mapLiquidToTipModel = chooseTipModels(items)
			tr1Items(items).map(_.toSeq)
		}).map(_.toMap)
	}
	
	/** 
	 * For each item, find the source liquid and choose a tip model
	 * Assumes that items have already been run through filterItems()
	 */
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
		def flatMap(f: GroupA => GroupResult): GroupResult
		def map(f: GroupA => GroupA): GroupResult
		def isError: Boolean = false
		def isSuccess: Boolean = false
		def isStop: Boolean = false
		
		final def >>=(f: GroupA => GroupResult): GroupResult = flatMap(f)
		def foreach(f: GroupA => GroupA): Unit = {  
			map(f)  
			()
		}
	}
	case class GroupError(groupA: GroupA, lsError: Seq[String]) extends GroupResult {
		def flatMap(f: GroupA => GroupResult): GroupResult = this
		def map(f: GroupA => GroupA): GroupResult = this
		override def isError: Boolean = true
	}
	case class GroupSuccess(groupA: GroupA) extends GroupResult {
		def flatMap(f: GroupA => GroupResult): GroupResult = f(groupA)
		def map(f: GroupA => GroupA): GroupResult = GroupSuccess(f(groupA))
		override def isSuccess: Boolean = true
	}
	case class GroupStop(groupA: GroupA) extends GroupResult {
		def flatMap(f: GroupA => GroupResult): GroupResult = this
		def map(f: GroupA => GroupA): GroupResult = this
		override def isStop: Boolean = true
	}

	// Create the first group for this schedule
	def createGroupA(
		states0: RobotState,
		mLM: Map[Item, LM]
	): GroupA = {
		val groupA0 = GroupA(mLM, states0, Map(), Map(), Nil, Nil, Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Nil, Nil, false, states0)
		groupA0
	}
	
	/**
	 * Create a group with @ref g0 as it's predecessor
	 * @param g0 predecessor to this group 
	 */
	def createGroupA(
		g0: GroupA
	): GroupA = {
		val g = GroupA(
			g0.mLM, g0.states1, g0.mTipToLM, g0.mTipToCleanSpecPending, Nil, Nil, Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map(), Nil, Nil, false, g0.states1
		)
		g
	}
	
	def addItemToGroup(
		g0: GroupA,
		item: L3A_PipetteItem
	): GroupResult = {
		for {
			g <- GroupSuccess(g0) >>=
				updateGroupA1_mLMData(item) >>= 
				updateGroupA2_mLMTipCounts >>=
				updateGroupA3_mLMToTips >>=
				updateGroupA4_mItemToTip >>=
				updateGroupA5_mTipToVolume >>=
				updateGroupA6_mItemToPolicy >>=
				updateGroupA7_lDispense >>=
				updateGroupA8_lAspirate >>=
				updateGroupA9_mTipToCleanSpec >>=
				updateGroupA9_states1
		} yield {
			g
		}
	}

	/** Add the item's volume to mLMData to keep track of how many tips are needed for each LM */
	def updateGroupA1_mLMData(item: Item)(g0: GroupA): GroupResult = {
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
	def updateGroupA2_mLMTipCounts(g0: GroupA): GroupResult = {
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
		//println("mLMToAdjacent: "+mLMToAdjacent)
		//println("lLMRang2: "+lLMRange2)
		
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
				updateGroupA2_mLMTipCounts(g0.copy(tipBindings0 = Map(), bClean = true))
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
	
	def updateGroupA3_mLMToTips(g0: GroupA): GroupResult = {
		val lTipAll: SortedSet[TipConfigL2] = device.config.tips.map(_.state(g0.states0).conf)
		val lTipFree = HashSet(lTipAll.toSeq : _*)
		val mLMToBoundTips: Map[LM, Seq[TipConfigL2]] = g0.tipBindings0.toSeq.groupBy(_._2).mapValues(_.map(_._1))
		val llTip: Seq[SortedSet[TipConfigL2]] = g0.lLM.map(lm => {
			val nTipTotal = g0.mLMTipCounts(lm)
			val lTipBound = mLMToBoundTips.getOrElse(lm, Seq[TipConfigL2]())
			val lTipAvailable = SortedSet((lTipFree ++ lTipBound).toSeq : _*)
			//println("lTipAvailable: "+lTipAvailable)
			device.assignTips(lTipAvailable, lm.tipModel, nTipTotal) match {
				case Error(lsError) => return GroupError(g0, lsError)
				case Success(lTip) =>
					//println("lTip: "+lTip)
					lTipFree --= lTip
					lTip
			}
		})
		//println("llTip: " + llTip)
		
		val mLMToTips = (g0.lLM zip llTip).toMap
		val mTipToLM = mLMToTips.flatMap(pair => pair._2.toSeq.map(_.state(g0.states0).conf -> pair._1)) 
		GroupSuccess(g0.copy(
			mLMToTips = mLMToTips,
			mTipToLM = mTipToLM
		))
	}
	
	def updateGroupA4_mItemToTip(g0: GroupA): GroupResult = {
		//println("Z5: ", g0.lLM, g0.mLMToTips)
		val lItemToTip = g0.lLM.flatMap(lm => {
			val lItem = g0.mLMToItems(lm)
			val lTip = g0.mLMToTips(lm).map(_.state(ctx.states).conf)
			val lDest: SortedSet[WellConfigL2] = SortedSet(lItem.map(_.dest) : _*)
			val ltw = PipetteHelper.chooseTipWellPairsAll(ctx.states, lTip, lDest).flatten
			//println(lTip, lDest, ltw)
			(lItem zip ltw).map(pair => pair._1 -> pair._2.tip)
		})
		GroupSuccess(g0.copy(
			mItemToTip = lItemToTip.toMap
		))
	}
	
	def updateGroupA5_mTipToVolume(g0: GroupA): GroupResult = {
		GroupSuccess(g0.copy(
			mTipToVolume = g0.mItemToTip.toSeq.groupBy(_._2).mapValues(_.foldLeft(0.0)((acc, pair) => acc + pair._1.nVolume)).toMap
		))
	}

	def updateGroupA6_mItemToPolicy(g0: GroupA): GroupResult = {
		val mTipToLiquidGroups = new HashMap[TipConfigL2, LiquidGroup]

		val lItemToPolicy = for (item <- g0.lItem) yield {
			val tip = g0.mItemToTip(item)
			// TODO: need to keep track of well liquid as we go, since we might dispense into a single well multiple times
			val destState = item.dest.state(g0.states0)
			val liquid = g0.mLM(item).liquid
			val nVolume = item.nVolume
			val nVolumeDest = destState.nVolume
			
			// TODO: allow for policy override
			val policy = cmd.args.pipettePolicy_?.getOrElse(
				device.getDispensePolicy(liquid, tip, nVolume, nVolumeDest) match {
					case None => return GroupError(g0, Seq("Could not find dispense policy for item "+item))
					case Some(p) => p
				}
			)
			
			item -> policy
		}
		
		GroupSuccess(g0.copy(
			mItemToPolicy = lItemToPolicy.toMap
		))
	}

	def updateGroupA7_lDispense(g0: GroupA): GroupResult = {
		val lDispense = g0.lItem.map(item => {
			val policy = g0.mItemToPolicy(item) 
			val tip = g0.mItemToTip(item)
			new TipWellVolumePolicy(tip, item.dest, item.nVolume, policy)
		})
		
		GroupSuccess(g0.copy(
			lDispense = lDispense
		))
	}
	
	def updateGroupA8_lAspirate(g0: GroupA): GroupResult = {
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

	case class LiquidGroups(pre: LiquidGroup, asperate: LiquidGroup, dispense: LiquidGroup)
	
	def updateGroupA9_mTipToCleanSpec(g0: GroupA): GroupResult = {
		// Liquid groups of destination wells with wet contact
		val mTipToLiquidGroups = new HashMap[TipConfigL2, Set[LiquidGroup]]
		val mTipToDestContams = new HashMap[TipConfigL2, Set[Contaminant.Value]]
		
		// Fill mTipToLiquidGroup; return GroupStop if trying to dispense into multiple liquid groups
		for ((item, policy) <- g0.mItemToPolicy) {
			// TODO: need to keep track of well liquid as we go, since we might dispense into a single well multiple times
			val liquidDest = item.dest.state(g0.states0).liquid
			// If we enter the destination liquid:
			if (policy.pos == PipettePosition.WetContact) {
				val tip = g0.mItemToTip(item)
				mTipToLiquidGroups.get(tip) match {
					case None =>
						mTipToLiquidGroups(tip) = Set(liquidDest.group)
						mTipToDestContams(tip) = liquidDest.contaminants
					case Some(lLiquidGroup0) =>
						if (!lLiquidGroup0.contains(liquidDest.group)) {
							// TODO: allow for override via tipOverrides
							// i.e. if overridden, set mTipToIntensity(tip) to max of two intensities
							return GroupStop(g0)
						}
				}
			}
			// TODO: what to do by default when we free dispense in a well with cells?
		}
		
		val l1 = g0.mTipToLM.map(pair => {
			val (tip, lm) = pair
			val policySrc = lm.liquid.group.cleanPolicy
			val lGroupCleanPolicyDest = mTipToLiquidGroups.getOrElse(tip, Set()).toSeq.map(_.cleanPolicy)
			val intensityDestEnter = WashIntensity.max(lGroupCleanPolicyDest.map(_.enter))
			val intensityDestExit = WashIntensity.max(lGroupCleanPolicyDest.map(_.exit))
			val cleanSpecPending_? = g0.mTipToCleanSpecPending0.get(tip)
			val intensityPending = cleanSpecPending_?.map(_.washIntensity).getOrElse(WashIntensity.None)
			val intensityPre = WashIntensity.max(List(policySrc.enter, intensityDestEnter, intensityPending))

			val cleanSpecPre = new WashSpec(
				washIntensity = intensityPre,
				contamInside = cleanSpecPending_?.map(_.contamInside).getOrElse(Set()),
				contamOutside = cleanSpecPending_?.map(_.contamOutside).getOrElse(Set()))
			
			// TODO: if the clean policy is overridden to not clean, then the intensityPending should be added to intensityPost
			val intensityPost = WashIntensity.max(List(policySrc.exit, intensityDestExit))
			// TODO: if the clean policy is overridden to not clean, then the intensityPending should be added to intensityPost
			// i.e. ++ (cleanSpecPending_?.map(_.contamInside).getOrElse(Set()))
			val contamInside = lm.liquid.contaminants
			val contamOutside = lm.liquid.contaminants ++ mTipToDestContams.getOrElse(tip, Set())
			val cleanSpecPost = new WashSpec(intensityPost, contamInside, contamOutside)
			
			((tip -> cleanSpecPre), (tip -> cleanSpecPost))
		})
		
		val (l2, l3) = l1.unzip(identity)
		
		GroupSuccess(g0.copy(
			mTipToCleanSpec = l2.toMap,
			mTipToCleanSpecPending = l3.toMap
		))
		
		/*
			
			
			val liquidSrc = g0.m
			liquid.group
			val tip = g0.mDestToTip(item)
			val destState = item.dest.state(g0.states0)
			val liquid = g0.mLM(item).liquid
			val nVolume = item.nVolume
			val nVolumeDest = destState.nVolume
			val liquidDest = destState.liquid
		}
		
		val lTipEnteredCells = new HashSet[TipConfigL2]
		val mTipToLiquidGroups = new HashMap[TipConfigL2, LiquidGroup]
		//val mTipToIntensity = new HashMap[TipConfigL2, WashIntensity.Value]
		val mTipToCleanSpec = new HashMap[TipConfigL2, WashSpec]
		val mTipToCleanSpecPending = new HashMap[TipConfigL2, WashSpec]
		val mDestToPolicy = new HashMap[Item, PipettePolicy]
		val builder = new StateBuilder(g0.states0)

		// For the tips in this group, add clean specs pending from the previous pipetting group
		mTipToCleanSpec ++= g0.mTipToCleanSpecPending0.filter(pair => g0.mTipToLM.contains(pair._1))
		// Add clean specs pending from the previous pipetting group
		// Below, we will overwrite the values for tips used in this group
		mTipToCleanSpecPending ++= g0.mTipToCleanSpecPending0

		val lDispense = for (item <- g0.lItem) yield {
			val tip = g0.mItemToTip(item)
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
			val (intensityEnter, intensityExit) = mTipToLiquidGroups.get(tip) match {
				case None =>
					mTipToLiquidGroups(tip) = liquidGroupDest
					(liquidGroupDest.cleanPolicy.enter, liquidGroupDest.cleanPolicy.exit)
				case Some(liquidGroup0) =>
					if (liquidGroupDest ne liquidGroup0) {
						// TODO: allow for override via tipOverrides
						// i.e. if overridden, set mTipToIntensity(tip) to max of two intensities
						return GroupStop(g0)
					}
					(liquidGroupDest.cleanPolicy.enter, liquidGroupDest.cleanPolicy.exit)
			}
			val tipState = tip.state(g0.states0)
			val intensityEnterMax = WashIntensity.max(intensityEnter, mTipToCleanSpec.get(tip).map(_.washIntensity).getOrElse(WashIntensity.None))
			val cleanSpecEnter = new WashSpec(intensityEnterMax, tipState.contamInside, tipState.contamOutside)
			val cleanSpecPending = new WashSpec(intensityExit, tipState.contamInside, tipState.contamOutside)
			mTipToCleanSpec(tip) = cleanSpecEnter
			mTipToCleanSpecPending(tip) = cleanSpecPending
			
			item.dest.obj.stateWriter(builder).add(liquid, nVolume)
			mDestToPolicy(item) = policy
			
			new TipWellVolumePolicy(tip, item.dest, nVolume, policy)
		}
		
		GroupSuccess(g0.copy(
			mTipToCleanSpec = mTipToCleanSpec.toMap,
			mTipToCleanSpecPending = mTipToCleanSpecPending.toMap,
			lDispense = lDispense
		))
		*/
	}
	
	def updateGroupA9_states1(g0: GroupA): GroupResult = {
		val builder = new StateBuilder(g0.states0)
		
		// TODO: handle tip replacement
		
		for ((tip, cleanSpec) <- g0.mTipToCleanSpec) {
			tip.obj.stateWriter(builder).clean(cleanSpec.washIntensity)
		}
		
		for (asp <- g0.lAspirate) {
			asp.tip.obj.stateWriter(builder).aspirate(asp.well.state(builder).liquid, asp.nVolume)
			asp.well.obj.stateWriter(builder).remove(asp.nVolume)
		}
		
		for (dis <- g0.lDispense) {
			dis.tip.obj.stateWriter(builder).dispense(dis.nVolume, dis.well.state(builder).liquid, dis.policy.pos)
			dis.well.obj.stateWriter(builder).add(dis.well.state(builder).liquid, dis.nVolume)
		}
		
		// TODO: handle mixes
		
		GroupSuccess(g0.copy(
			states1 = builder.toImmutable
		))
	}
}
