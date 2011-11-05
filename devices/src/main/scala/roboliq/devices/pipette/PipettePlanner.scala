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
				lDispense.map(twvpString).mkString("lDispense:\n    ", "\n    ", ""),
				lAspirate.map(twvpString).mkString("lAspirate:\n    ", "\n    ", "")
			).mkString("GroupZ(\n  ", "\n  ", ")\n")
		}
		
		private def twvpString(twvp: TipWellVolumePolicy): String = {
			List(twvp.tip, Command.getWellsDebugString(Seq(twvp.well)), twvp.nVolume, twvp.policy.id).mkString(", ")			
		}
	}
	case class GroupB(
		lItem: Seq[Item],
		bClean: Boolean,
		premixes: Seq[TipWellVolume], 
		lAspirate: Seq[L2C_Aspirate], 
		lDispense: Seq[L2C_Dispense], 
		postmixes: Seq[TipWellVolume]
	)
	case class GroupC(
		nItems: Int,
		cmds: Seq[Command],
		nScore: Double
	)
	
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
		val groupZ0 = GroupZ(mLM, states0, Map(), Nil, Nil, Map(), Map(), Map(), Map(), Map(), Map(), Map(), Nil, Nil, false)
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
		//val mDestToTip = new HashMap[Item, TipConfigL2]
		GroupSuccess(g0.copy(
			mDestToTip = g0.lLM.flatMap(lm => {
				val lItem = g0.mLMToItems(lm)
				val lTip = g0.mLMToTips(lm).map(_.state(ctx.states).conf)
				val lDest: SortedSet[WellConfigL2] = SortedSet(lItem.map(_.dest) : _*)
				val ltw = PipetteHelper.chooseTipWellPairsAll(ctx.states, lTip, lDest).flatten
				println(lTip, lDest, ltw)
				(lItem zip ltw).map(pair => pair._1 -> pair._2.tip)
			}).toMap)
		)
	}
	
	def updateGroupZ6_mTipToVolume(g0: GroupZ): GroupResult = {
		GroupSuccess(g0.copy(
			mTipToVolume = g0.mDestToTip.toSeq.groupBy(_._2).mapValues(_.foldLeft(0.0)((acc, pair) => acc + pair._1.nVolume)).toMap
		))
	}

	def updateGroupZ7_lDispense(g0: GroupZ): GroupResult = {
		val builder = new StateBuilder(g0.states0)
		val lDispense = for (item <- g0.lItem) yield {
			val tip = g0.mDestToTip(item)
			val liquid = g0.mLM(item).liquid
			val nVolume = item.nVolume
			val nVolumeDest = item.dest.obj.state(builder).nVolume
			// TODO: allow for policy override
			val policy_? = device.getDispensePolicy(liquid, tip, nVolume, nVolumeDest)
			if (policy_?.isEmpty)
				return GroupError(g0, Seq("Could not find dispense policy for item "+item))
			item.dest.obj.stateWriter(builder).add(liquid, nVolume)
			new TipWellVolumePolicy(tip, item.dest, nVolume, policy_?.get)
		}
		// TODO: check whether any of the dispenses force a new cleaning, and if so, return GroupStop(g0)
		GroupSuccess(g0.copy(
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
	
	def tr_groupB(
		groupZ: GroupZ
	): Result[GroupB] = {
		val lAspirate = groupSpirateItems(groupZ, groupZ.lAspirate).map(items => L2C_Aspirate(items))
		val lDispense = groupSpirateItems(groupZ, groupZ.lAspirate).map(items => L2C_Dispense(items))
			
		val groupB = GroupB(
			groupZ.lItem,
			groupZ.bClean,
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
	}
}
