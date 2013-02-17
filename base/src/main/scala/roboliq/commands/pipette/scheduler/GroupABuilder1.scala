package roboliq.commands.pipette.scheduler

import scala.collection.immutable.SortedSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import roboliq.core._
import roboliq.commands._
import roboliq.commands.pipette._
import roboliq.devices.pipette._
//import roboliq.compiler._
//import roboliq.devices.pipette._


class GroupABuilder1(
	device: PipetteDevice,
	ctx: ProcessorContext,
	cmd: L3C_Pipette
) extends GroupABuilderBase(device, ctx, cmd) {
	
	def addItemToGroup(
		g0: GroupA,
		item: Item
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
			//println("A11 g.lPremix: "+g.lPremix)
			g
		}
	}

	/** Add the item's volume to mLMData to keep track of how many tips are needed for each LM */
	def updateGroupA1_mLMData(item: Item)(g0: GroupA): GroupResult = {
		// if a source of item is in the list of previous destinations, return GroupStop(g0)
		if (item.srcs.exists(src => g0.lItem.exists(item => item.dest eq src))) {
			return GroupStop(g0, "Item source is in list of previous destinations")
		}
		
		//println("updateGroupA1_mLMData: "+L3A_PipetteItem.toDebugString(item) + ", "+g0.lItem)
		val lItem = g0.lItem ++ Seq(item)
		// Get a list of LMs in the order defined by lItem
		val lLM = lItem.map(g0.mLM).toList.distinct
		val mLMToItems = lItem.groupBy(g0.mLM)
		val lm = g0.mLM(item)
		if (item.volume > lm.tipModel.volume)
			GroupError(g0, Seq("pipette volume exceeds volume of tip: "+item))
		
		val data = g0.mLMData.get(lm) match {
			case None =>
				LMData(1, item.volume, item.volume)
			case Some(data) =>
				val nVolumeCurrent = data.nVolumeCurrent + item.volume
				val nVolumeTotal = data.nVolumeTotal + item.volume
				//val bRequiresNewTip = requiresDifferentTip(g0, lm, item, mLMToItems(lm))
				/*// FIXME: for debug only
				if (nVolumeCurrent > 1000) {
					println(lm.tipModel, lm.tipModel.volume)
					Seq().head
				}
				// ENDFIX*/
				// If multipipetting is not allowed, use a new tip
				// FIXME: consider the total volume to be dispensed instead
				val bAllowMultipipette = lm.liquid.multipipetteThreshold <= 0 && cmdAllowsMultipipette
				if (!bAllowMultipipette)
					LMData(data.nTips + 1, nVolumeTotal, item.volume)
				// If current
				//else if (data.nVolumeCurrent.isEmpty)
				//	LMData(data.nTips + 1, nVolumeTotal, nVolumeCurrent)
				// If new volume does not exceed tip capacity
				else if (nVolumeCurrent <= lm.tipModel.volume)
					LMData(data.nTips, nVolumeTotal, nVolumeCurrent)
				// Otherwise use a new tip
				else
					LMData(data.nTips + 1, nVolumeTotal, item.volume)
		}
		// FIXME: for debug only
		if (data.nVolumeCurrent > LiquidVolume.ul(1000)) {
			println("updateLMData")
			println(data)
			println(lm.tipModel, lm.tipModel.volume)
			println(g0.mLMData)
			println(g0.mLMData.get(lm))
			Seq().head
		}

		GroupSuccess(g0.copy(
			lItem = lItem,
			lLM = lLM, 
			mLMToItems = mLMToItems,
			mLMData = g0.mLMData.updated(lm, data)
		))
	}
	
	private def cmdAllowsMultipipette: Boolean =
		cmd.args.tipOverrides_?.map(_.allowMultipipette_?.getOrElse(true)).getOrElse(true)

	/*
	private def requiresDifferentTip(g0: GroupA, lm: LM, item: Item, items: Seq[Item]): Boolean = {
		if (items.isEmpty)
			return true
			
		if (lm.liquid.multipipetteThreshold > 0)
			return true
		
		val item1 = items.last
		val itemState = g0.mItemToState(item)
		val liquid = itemState.srcContent.liquid
		val volume = item.volume
		
		val policy_? = getPolicy(g0, lm, item)
		val policy1_? = getPolicy(g0, lm, item1)
		
		if (policy_?.isEmpty || policy1_?.isEmpty) return true // Arbitrary error handling
		
		val policy = policy_?.get
		val policy1 = policy1_?.get
		
		if (policy != policy1)
			return true
		
		// TODO: we need to check liquid groups!
		if (policy.pos == PipettePosition.WetContact)
			return true
		
		return false
	}
	
	private def getPolicy(g0: GroupA, lm: LM, item: Item): Option[PipettePolicy] = {
		val itemState = g0.mItemToState(item)
		val liquid = itemState.srcContent.liquid
		cmd.args.pipettePolicy_?.orElse(
			device.getDispensePolicy(liquid, lm.tipModel, item.volume, itemState.destState0)
		)
	}
	*/
	
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
		val mLMToAdjacent: Map[LM, Int] = g0.mLMToItems.mapValues(countMaxAdjacentWells(g0.states0))
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
				GroupStop(g0, "Unable to allocate tips")
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
	private def countMaxAdjacentWells(query: StateQuery)(lItem: Seq[Item]): Int = {
		val lWell = lItem.map(_.dest)
		WellGroup(query, lWell).splitByAdjacent().foldLeft(0)((acc, group) => math.max(acc, group.set.size))
	}
	
	def updateGroupA3_mLMToTips(g0: GroupA): GroupResult = {
		//println("g0.mTipToCleanSpecPending0: "+g0.mTipToCleanSpecPending0)
		val llTip = {
			val lTipClean = lTipAll.filter(tip => {
				g0.mTipToCleanSpecPending0.get(tip) match {
					case None => true
					case Some(washSpec) => washSpec.washIntensity == CleanIntensity.None
				}
			})
			// FIXME: for debug only
			if (lTipClean.size == 1)
				lTipClean.toString()
			// ENDFIX
			updateGroupA3_mLMToTips_sub(g0, lTipClean) match {
				case Error(lsError) =>
					updateGroupA3_mLMToTips_sub(g0, lTipAll) match {
						case Error(lsError) => return GroupError(g0, lsError)
						case Success(llTip) => llTip
					}
				case Success(llTip) => llTip
			}
		}
		//println("llTip: " + llTip)
		
		val mLMToTips = (g0.lLM zip llTip).toMap
		val mTipToLM = mLMToTips.flatMap(pair => pair._2.toSeq.map(_.state(g0.states0).conf -> pair._1)) 
		GroupSuccess(g0.copy(
			mLMToTips = mLMToTips,
			mTipToLM = mTipToLM
		))
	}

	def updateGroupA3_mLMToTips_sub(g0: GroupA, lTipFree0: SortedSet[Tip]): Result[Seq[SortedSet[Tip]]] = {
		val lTipFree = HashSet(lTipFree0.toSeq : _*)
		val mLMToBoundTips: Map[LM, Seq[Tip]] = g0.tipBindings0.toSeq.groupBy(_._2).mapValues(_.map(_._1))
		val llTip: Seq[SortedSet[Tip]] = g0.lLM.map(lm => {
			val nTipTotal = g0.mLMTipCounts(lm)
			val lTipBound = mLMToBoundTips.getOrElse(lm, Seq[Tip]())
			val lTipAvailable = SortedSet((lTipFree ++ lTipBound).toSeq : _*)
			//println("lTipAvailable: "+lTipAvailable)
			device.assignTips(lTipAvailable, lm.tipModel, nTipTotal) match {
				case Error(lsError) => return Error(lsError)
				case Success(lTip) =>
					//println("lTip: "+lTip)
					lTipFree --= lTip
					lTip
			}
		})
		Success(llTip)
	}
	
	def updateGroupA4_mItemToTip(g0: GroupA): GroupResult = {
		//println("Z5: ", g0.lLM, g0.mLMToTips)
		val lItemToTip = g0.lLM.flatMap(lm => {
			val lItem = g0.mLMToItems(lm).toList
			val lTip = g0.mLMToTips(lm).map(_.state(ctx.states).conf)
			//val lDest: SortedSet[Well] = SortedSet(lItem.map(_.dest) : _*)
			val mDestToItems = lItem.groupBy(_.dest)
			val mItemToTip = updateGroupA4_sub(g0, lTip, mDestToItems, Map()) match {
				case Error(ls) => return GroupError(g0, ls)
				case Success(m) =>
					if (m.isEmpty) {
						if (g0.mItemToTip.isEmpty)
							return GroupError(g0, List("Tip volumes too small for even one aspiration"))
						else
							return GroupStop(g0, "Tip volumes no large enough for further aspiration")
					}
					m
			}
			mItemToTip.toSeq
			//val ltw = PipetteHelper.chooseTipWellPairsAll(g0.states0, lTip, lDest).flatten
			//println("A4:", lTip, lDest, ltw)
			//(lItem zip ltw).map(pair => pair._1 -> pair._2.tip)
		})
		GroupSuccess(g0.copy(
			mItemToTip = lItemToTip.toMap
		))
	}
	
	private def updateGroupA4_sub(g0: GroupA, lTip: SortedSet[Tip], mDestToItems: Map[Well, List[Item]], acc: Map[Item, Tip]): Result[Map[Item, Tip]] = {
		if (mDestToItems.isEmpty) Success(acc)
		else {
			val ltw0 = PipetteHelper.chooseTipWellPairsAll(g0.states0, lTip, SortedSet(mDestToItems.keySet.toSeq : _*)) match {
				case Error(ls) => return Error(ls)
				case Success(l) => l.flatten
			}
			// FIXME: I'm not sure whether this should really happen -- ellis, 2012-04-10
			if (ltw0.isEmpty) {
				//assert(!ltw0.isEmpty)
				return Error("PipetteHelper.chooseTipWellPairsAll() returned empty list")
			}
			// Make sure that max tip volume isn't exceeded when pipetting from a single liquid to multiple destinations
			val mTipToVolume = new HashMap[Tip, LiquidVolume]
			val ltw = ltw0.filter(tw => {
				val nVolumeTip0: LiquidVolume = mTipToVolume.getOrElse(tw.tip, LiquidVolume.empty)
				val item = mDestToItems(tw.well).head
				val lm = g0.mLM(item)
				val nVolumeTip = nVolumeTip0 + item.volume
				// If multipipetting is allowed:
				if (lm.liquid.multipipetteThreshold == 0 && cmdAllowsMultipipette) {
					if (nVolumeTip <= lm.tipModel.volume) {
						mTipToVolume(tw.tip) = nVolumeTip
						true
					}
					else {
						false
					}
				}
				else {
					if (nVolumeTip0.isEmpty) {
						mTipToVolume(tw.tip) = nVolumeTip
						true
					}
					else {
						false
					}
				}
			})
			// If all tips were full
			if (ltw.isEmpty) {
				return Success(Map())
			}
			val lTip2 = lTip -- ltw.map(_.tip)
			val acc2 = acc ++ ltw.map(tw => mDestToItems(tw.well).head -> tw.tip)
			// Remove processed items from mDestToItems
			val mDestToItems2 = mDestToItems.map(pair => {
				val (dest, items) = pair
				if (ltw.exists(_.well == dest))
					(dest, items.tail)
				else
					(dest, items)
			}).filterNot(_._2.isEmpty)
			//println("updateGroupA4_sub:", ltw0, ltw, lTip2, acc2, mDestToItems2)
			updateGroupA4_sub(g0, lTip2, mDestToItems2, acc2)
		}
	}
	
	def updateGroupA5_mTipToVolume(g0: GroupA): GroupResult = {
		val mTipToVolume = g0.mItemToTip.toSeq.groupBy(_._2).mapValues(_.foldLeft(LiquidVolume.empty)((acc, pair) => acc + pair._1.volume)).toMap
		//println("mTipToVolume: "+mTipToVolume)
		GroupSuccess(g0.copy(
			mTipToVolume = mTipToVolume
		))
	}

	def updateGroupA6_mItemToPolicy(g0: GroupA): GroupResult = {
		val mTipToLiquidGroups = new HashMap[Tip, LiquidGroup]

		val lItemToPolicy = for (item <- g0.lItem) yield {
			// FIXME: For debug only
			if (!g0.mItemToTip.contains(item)) {
				println("updatemItemToPolicy")
				println(item)
				println(g0.mItemToTip)
				println("g0:")
				println(g0)
			}
			// ENDFIX
			val tipModel = g0.mLM(item).tipModel
			val itemState = g0.mItemToState(item)
			val liquid = itemState.srcContent.liquid
			val volume = item.volume
			//val nVolumeDest = itemState.destState0.volume
			
			// TODO: allow for policy override
			val policy = cmd.args.pipettePolicy_?.getOrElse(
				device.getDispensePolicy(liquid, tipModel, volume, itemState.destState0) match {
					case None => return GroupError(g0, Seq("Could not find dispense policy for item "+item))
					case Some(p) => p
				}
			)
			
			item -> policy
		}
		
		// Get a list of policies associated with each tip 
		val mTipToPolicies: Map[Tip, Seq[PipettePolicy]]
			= lItemToPolicy.map(pair => g0.mItemToTip(pair._1) -> pair._2).groupBy(_._1).mapValues(_.map(_._2).distinct)
		
		// Each tip should have one unique policy
		val bUnique = mTipToPolicies.toList.forall(_._2.length == 1)
		if (bUnique) {
			val mTipToPolicy = mTipToPolicies.mapValues(l => l(0))
			GroupSuccess(g0.copy(
				mItemToPolicy = lItemToPolicy.toMap,
				mTipToPolicy = mTipToPolicy
			))
		}
		else {
			GroupStop(g0, "New policy required for tip")
		}
	}

	def updateGroupA7_lDispense(g0: GroupA): GroupResult = {
		val lDispense = g0.lItem.map(item => {
			val tip = g0.mItemToTip(item)
			val policy = g0.mItemToPolicy(item) 
			new TipWellVolumePolicy(tip, item.dest, item.volume, policy)
		})
		
		val lPostmix = g0.lItem.flatMap(item => {
			val tip = g0.mItemToTip(item)
			val policy = g0.mItemToPolicy(item) 
			val mixSpec_? = (item.postmix_?, cmd.args.mixSpec_?) match {
				case (None, None) => None
				case (Some(a), Some(b)) => Some(a + b)
				case (a, b) => if (a.isEmpty) b else a
			}
			
			if (mixSpec_?.isDefined) {
				val mixSpec = device.getMixSpec(tip.state(g0.states0), g0.mItemToState(item).destState1, mixSpec_?) match {
					case Error(lsError) => return GroupError(g0, lsError)
					case Success(o) => o
				}
				Seq(new TipWellMix(tip, item.dest, mixSpec))
			}
			else {
				Seq()
			}
		})
		
		GroupSuccess(g0.copy(
			lDispense = lDispense,
			lPostmix = lPostmix
		))
	}
	
	def updateGroupA8_lAspirate(g0: GroupA): GroupResult = {
		//val states = new StateBuilder(g0.states0)
		val lAspirate = g0.lLM.flatMap(lm => {
			val tips = g0.mLMToTips(lm)
			val lItem = g0.lItem.filter(item => g0.mLM(item) == lm)
			val srcs = SortedSet(lItem.flatMap(_.srcs) : _*)
			val lltw: Seq[Seq[TipWell]] = PipetteHelper.chooseTipSrcPairs(g0.states0, tips, srcs) match {
				case Error(ls) => return GroupError(g0, ls)
				case Success(ll) => ll
			}
			val ltw = lltw.flatMap(identity)
			// FIXME: If the tip is assigned to the well, then is SHOULD be used, but
			// don't insist for now...
			val ltw2 = ltw.filter(tw => g0.mTipToVolume.contains(tw.tip))
			// ENDFIX
			ltw2.map(tw => {
				// FIXME: for debug only
				if (!g0.mTipToVolume.contains(tw.tip)) {
					println("DEBUG:")
					println("g0: "+g0)
					println("tips: "+g0.mTipToVolume)
					println("mItemToTip: "+g0.mItemToTip)
					println("lItem: "+lItem)
					println("ltw: "+ltw)
				}
				// ENDFIX
				val volume = g0.mTipToVolume(tw.tip)
				/*val policy_? = device.getAspiratePolicy(tw.tip.state(g0.states0), volume, tw.well.state(g0.states0))
				// FIXME: for debug only
				if (policy_?.isEmpty) {
					println("getAspiratePolicy")
					println(tw.tip.state(g0.states0), tw.well.state(g0.states0))
				}
				// ENDFIX
				val policy = cmd.args.pipettePolicy_?.getOrElse(
					policy_? match {
						case None => return GroupError(g0, Seq("Could not find aspirate policy for "+tw.tip+" and "+tw.well))
						case Some(p) => p
					}
				)
				*/
				val policy = g0.mTipToPolicy(tw.tip)
				new TipWellVolumePolicy(tw.tip, tw.well, volume, policy)
			})
		})
		
		val lPremix = g0.lLM.flatMap(lm => {
			val tips = g0.mLMToTips(lm)
			val lItem = g0.lItem.filter(item => g0.mLM(item) == lm)
			val srcs = SortedSet(lItem.flatMap(_.srcs) : _*)
			val lltw: Seq[Seq[TipWell]] = PipetteHelper.chooseTipSrcPairs(g0.states0, tips, srcs) match {
				case Error(ls) => return GroupError(g0, ls)
				case Success(ll) => ll
			}
			val ltw = lltw.flatMap(identity)
			// Gather premix specs
			lItem.map(_.premix_?).flatten match {
				case Seq() => Seq()
				case Seq(premix, rest @ _*) =>
					if (!rest.forall(_ == premix)) {
						return GroupError(g0, Seq("premix specs must be the same for all items which refer to the same source"))
					}
					ltw.map(tw => {
						val mixSpec = device.getMixSpec(tw.tip.state(g0.states0), tw.well.wellState(g0.states0).get, Some(premix)) match {
							case Error(lsError) => return GroupError(g0, lsError)
							case Success(o) => o
						}
						new TipWellMix(tw.tip, tw.well, mixSpec)
					})
			}
		})

		//println("lPremix: "+lPremix)
		/*println("g0.copy: "+(g0.copy(
			lAspirate = lAspirate,
			lPremix = lPremix
		)))*/
		GroupSuccess(g0.copy(
			lAspirate = lAspirate,
			lPremix = lPremix
		))
	}

	//case class LiquidGroups(pre: LiquidGroup, asperate: LiquidGroup, dispense: LiquidGroup)
	
	def updateGroupA9_mTipToCleanSpec(g0: GroupA): GroupResult = {
		//println("A9 g0.lPremix: "+g0.lPremix)
		// Liquid groups of destination wells with wet contact
		val mTipToLiquidGroups = new HashMap[Tip, Set[LiquidGroup]]
		val mTipToDestContams = new HashMap[Tip, Set[Contaminant.Value]]
		
		// Fill mTipToLiquidGroup; return GroupStop if trying to dispense into multiple liquid groups
		for ((item, policyDisp) <- g0.mItemToPolicy) {
			val pos = if (item.postmix_?.isDefined || cmd.args.mixSpec_?.isDefined) PipettePosition.WetContact else policyDisp.pos
			// TODO: need to keep track of well liquid as we go, since we might dispense into a single well multiple times
			val liquidDest = item.dest.wellState(g0.states0).get.liquid
			// If we enter the destination liquid:
			if (pos == PipettePosition.WetContact) {
				val tip = g0.mItemToTip(item)
				mTipToLiquidGroups.get(tip) match {
					case None =>
						mTipToLiquidGroups(tip) = Set(liquidDest.group)
						mTipToDestContams(tip) = liquidDest.contaminants
					case Some(lLiquidGroup0) =>
						if (!lLiquidGroup0.contains(liquidDest.group)) {
							// FIXME: this is a hack!!!
							val bOverride = cmd.args.tipOverrides_? match {
								case None => false
								case Some(tipOverride) =>
									tipOverride.replacement_? match {
										case None => false
										case Some(_) => true
									}
							}
							if (!bOverride)
								// TODO: allow for override via tipOverrides
								// i.e. if overridden, set mTipToIntensity(tip) to max of two intensities
								return GroupStop(g0, "can only dispense into one liquid group")
						}
				}
			}
			// TODO: what to do by default when we free dispense in a well with cells?
		}
		
		val pre_post = g0.mTipToLM.map(pair => {
			val (tip, lm) = pair
			val policySrc = lm.liquid.group.cleanPolicy
			val lGroupCleanPolicyDest = mTipToLiquidGroups.getOrElse(tip, Set()).toSeq.map(_.cleanPolicy)
			val intensityDestEnter = CleanIntensity.max(lGroupCleanPolicyDest.map(_.enter))
			val intensityDestExit = CleanIntensity.max(lGroupCleanPolicyDest.map(_.exit))
			val cleanSpecPending_? = g0.mTipToCleanSpecPending0.get(tip)
			val intensityPending = cleanSpecPending_?.map(_.washIntensity).getOrElse(CleanIntensity.None)
			val intensityPre = CleanIntensity.max(List(policySrc.enter, intensityDestEnter, intensityPending))

			val cleanSpecPre = new WashSpec(
				washIntensity = intensityPre,
				contamInside = cleanSpecPending_?.map(_.contamInside).getOrElse(Set()),
				contamOutside = cleanSpecPending_?.map(_.contamOutside).getOrElse(Set()))
			
			// TODO: if the clean policy is overridden to not clean, then the intensityPending should be added to intensityPost
			val intensityPost = CleanIntensity.max(List(policySrc.exit, intensityDestExit))
			// TODO: if the clean policy is overridden to not clean, then the intensityPending should be added to intensityPost
			// i.e. ++ (cleanSpecPending_?.map(_.contamInside).getOrElse(Set()))
			val contamInside = lm.liquid.contaminants
			val contamOutside = lm.liquid.contaminants ++ mTipToDestContams.getOrElse(tip, Set())
			val cleanSpecPost = new WashSpec(intensityPost, contamInside, contamOutside)
			
			((tip -> cleanSpecPre), (tip -> cleanSpecPost))
		})
		
		val (l2, l3) = pre_post.unzip(identity)
		val mTipToCleanSpecA = l2.toMap
		val mTipToCleanSpecPendingA = g0.mTipToCleanSpecPending0 -- l2.map(_._1) ++ l3.toMap
		//val lTipsToClean = device.getTipsToCleanSimultaneously(lTipAll, SortedSet(l2.toSeq.map(_._1) : _*)).toSet
		val lCleanSpecToTips = device.batchCleanSpecs(lTipAll, mTipToCleanSpecA)
		val mTipToCleanSpec = lCleanSpecToTips.flatMap(pair => {
			val (cleanSpec, lTip) = pair
			(lTip.toSeq).map(_ -> cleanSpec)
		}).toMap
		/*val mTipToCleanSpec = lTipsToClean.map(tip => {
			tip -> mTipToCleanSpecA.getOrElse(tip,
					mTipToCleanSpecPendingA.getOrElse(tip, 
							new WashSpec(CleanIntensity.None, Set(), Set())))
		}).toMap*/
		//val mTipToCleanSpecPending = mTipToCleanSpecPendingA.filter(pair => !mTipToCleanSpec.contains(pair._1))
		val mTipToCleanSpecPending = (g0.mTipToCleanSpecPending0 -- mTipToCleanSpec.keys ++ l3.toMap).filter(_._2.washIntensity != CleanIntensity.None)
		
		//println("mTipToCleanSpec: "+mTipToCleanSpec)
		//println("mTipToCleanSpecPending: "+mTipToCleanSpecPending)
		
		GroupSuccess(g0.copy(
			mTipToCleanSpecA = mTipToCleanSpecA,
			mTipToCleanSpecPendingA = mTipToCleanSpecPendingA,
			mTipToCleanSpec = mTipToCleanSpec,
			mTipToCleanSpecPending = mTipToCleanSpecPending
		))
	}
	
	def updateGroupA9_states1(g0: GroupA): GroupResult = {
		//println("A10 g0.lPremix: "+g0.lPremix)
		val builder = g0.states0.toBuilder
		
		// TODO: handle tip replacement
		
		for ((tip, cleanSpec) <- g0.mTipToCleanSpec) {
			tip.stateWriter(builder).clean(cleanSpec.washIntensity)
		}

		//println("g0.lAspirate: "+g0.lAspirate)
		for (asp <- g0.lAspirate) {
			// FIXME: for debug only
			if ((asp.well.wellState(builder).get.liquid != g0.mTipToLM(asp.tip).liquid)) {
				println("DEBUG")
				println("asp.well.state(builder): "+asp.well.wellState(builder).get)
				println("g0.mTipToLM(asp.tip): "+g0.mTipToLM(asp.tip))
			}
			assert(asp.well.wellState(builder).get.liquid == g0.mTipToLM(asp.tip).liquid)
			//println("liquid, tip: ", asp.well.state(builder).liquid, asp.tip)
			// ENDFIX
			asp.tip.stateWriter(builder).aspirate(asp.well, asp.well.wellState(builder).get.liquid, asp.volume)
			asp.well.stateWriter(builder).remove(asp.volume)
		}
		
		for ((item, dis) <- g0.lItem zip g0.lDispense) {
			val itemState = g0.mItemToState(item)
			dis.tip.stateWriter(builder).dispense(item.volume, itemState.destState0.liquid, dis.policy.pos)
			builder.map(item.dest.id) = itemState.destState1
		}
		
		// TODO: handle mixes
		
		GroupSuccess(g0.copy(
			states1 = builder.toImmutable
		))
	}
}
