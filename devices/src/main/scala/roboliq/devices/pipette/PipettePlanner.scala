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
		mLMToItems: Map[LM, Seq[Item]],
		states0: RobotState,
		tipBindings0: Map[TipConfigL2, LM],
		lItem: Seq[Item],
		lLM: Seq[LM],
		mLMData: Map[LM, LMData],
		mLMTipCounts: Map[LM, Int],
		mLMToTips: Map[LM, SortedSet[TipConfigL2]],
		mTipToLM: Map[TipConfigL2, LM],
		mDestToTip: Map[Item, TipConfigL2],
		mTipToVolume: Map[TipConfigL2, Double],
		lDispense: Seq[TipWellVolumePolicy],
		lAspirate: Seq[TipWellVolumePolicy],
		bClean: Boolean
		// FIXME: need to keep track of well contents too, in case we dispense more than once to a given well
	) {
		override def toString: String = {
			List(
				"mLMToItems:\n"+mLMToItems.toSeq.map(pair => pair._1.toString + " -> " + L3A_PipetteItem.toDebugString(pair._2)).mkString("    ", "\n    ", ""),
				lItem.mkString("lItem:\n    ", "\n    ", ""),
				lLM.map(lm => lm.toString + " -> " + mLMData(lm)).mkString("mLMData:\n    ", "\n    ", ""),
				lLM.map(lm => lm.toString + " -> " + mLMTipCounts(lm)).mkString("mLMCounts:\n    ", "\n    ", ""),
				lLM.map(lm => lm.toString + " -> " + mLMToTips(lm)).mkString("mLMToTips:\n    ", "\n    ", ""),
				lDispense.mkString("lDispense:\n    ", "\n    ", ""),
				lAspirate.mkString("lAspirate:\n    ", "\n    ", "")
			).mkString("GroupZ(\n  ", "\n  ", ")\n")
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

	/*def tr(
		items: Seq[Item],
		mLM: Map[Item, LM]
	): Result[Seq[GroupA]] = {
		items(i)
	}*/

	
/*
	case class GroupZ(
		states0: RobotState,
		lItem: Seq[Item],
		mLM: Map[Item, LM],
		mLMToItems: Map[LM, Seq[Item]],
		mLMData: Map[LM, LMData],
		mLMTipCounts: Map[LM, Int],
		mLMToTips: Map[LM, SortedSet[Tip]],
		mDestToTip: Map[Item, TipConfigL2],
		mTipToVolume: Map[TipConfigL2, Double],
		lSrcToTip: Seq[TipWellVolume],
		bClean: Boolean,
		tipBindings0: Map[TipConfigL2, LM]
		// FIXME: need to keep track of well contents too, in case we dispense more than once to a given well
	)
*/
	
	def createGroupZ(
		states0: RobotState,
		mLM: Map[Item, LM]
	): GroupResult = {
		val mLMToItems: Map[LM, Seq[Item]] = mLM.toSeq.groupBy(_._2).mapValues(_.map(_._1)).toMap
		createGroupZ(states0, mLM, mLMToItems)
	}
	
	def createGroupZ(
		states0: RobotState,
		mLM: Map[Item, LM],
		mLMToItems: Map[LM, Seq[Item]]
	): GroupResult = {
		val groupZ0 = GroupZ(mLM, mLMToItems, states0, Map(), Nil, Nil, Map(), Map(), Map(), Map(), Map(), Map(), Nil, Nil, false)
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
		val tipsFree = HashSet((device.config.tips -- g0.tipBindings0.map(_._1.obj)).toSeq : _*)
		val mMToLM = g0.lLM.groupBy(_.tipModel)
		// Number of tips required for each model
		val mMToCount = g0.mLMTipCounts.toSeq.groupBy(_._1.tipModel).mapValues(_.foldLeft(0)((acc, item) => acc + item._2)).toMap
		val lM = g0.lLM.map(_.tipModel).distinct
		val lMToCount = lM.map(m => m -> mMToCount(m))
		
		println("lM: " + lM)
		println("lMToCount: " + lMToCount)
		
		val llTip = device.assignTips(device.config.tips, lMToCount) match {
			case Error(lsError) => return GroupError(g0, lsError)
			case Success(x) => x.map(_.map(_.state(g0.states0).conf))
		}
		println("llTip: " + llTip)
		
		val mLMToTips = (g0.lLM zip llTip).toMap
		val mTipToLM = mLMToTips.flatMap(pair => pair._2.toSeq.map(_.state(g0.states0).conf -> pair._1)) 
		GroupSuccess(g0.copy(
			mLMToTips = mLMToTips,
			mTipToLM = mTipToLM
		))
	}
	
	def updateGroupZ5_mDestToTip(g0: GroupZ): GroupResult = {
		//val mDestToTip = new HashMap[Item, TipConfigL2]
		GroupSuccess(g0.copy(
			mDestToTip = g0.lLM.flatMap(lm => {
				val lItem = g0.mLMToItems(lm)
				val lTip = g0.mLMToTips(lm).map(_.state(ctx.states).conf)
				val lDest: SortedSet[WellConfigL2] = SortedSet(lItem.map(_.dest) : _*)
				val ltw = PipetteHelper.chooseTipWellPairsAll(ctx.states, lTip, lDest).flatten
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
			println("srcs: " + srcs)
			println("ltw: " + ltw)
			println("g0: " + g0)
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
		// Create list of list of spirate items
		lMP.map(mp => {
			ma(mp).map(twvp => new L2A_SpirateItem(twvp.tip, twvp.well, twvp.nVolume, twvp.policy))
		})
	}
	
	/*
	/*case class ItemC(item: L3A_PipetteItem, tip: TipConfigL2)
		val srcs: SortedSet[WellConfigL2],
		val dest: WellConfigL2,
		val nVolume: Double,
		val liquid: Liquid,
		val tip: TipConfigL2
	)
	
	case class TipSrcDestVol(
		val tip: TipConfigL2,
		val src: WellConfigL2,
		val dest: WellConfigL2,
		val nVolume: Double
	)*/
	
	
	def chooseTipModels(items: Seq[L3A_PipetteItem]): Map[Liquid, TipModel] = {
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
	def transformLayers(layers: Seq[Seq[L3A_PipetteItem]]): Seq[ItemB] = {
		layers.flatMap(items => {
			val mapLiquidToTipModel = chooseTipModels(items)
			transformItems(items)
		})
	}
	
	/** For each item, find the source liquid and choose a tip model */
	def transformItems(items: Seq[L3A_PipetteItem]): Seq[ItemB] = {
		val mapLiquidToTipModel = chooseTipModels(items)
		items.map(item => {
			val liquid = item.srcs.head.state(ctx.states).liquid
			val tipModel = mapLiquidToTipModel(liquid)
			ItemB(item, liquid, tipModel)
		})
	}
	
	/** Assign tips to items */
	def x1(layers: Seq[Seq[L3A_PipetteItem]]): Unit = {
		val tipStates = (for {
			tipObj <- device.config.tips.toSeq
			val tip = tipObj.state(ctx.states).conf
		} yield { tip -> new TipState(tip) }).toMap

		// For each item, choose a tip model
		val lData = toItemData1(layers)
		
		for {
			lGroup <- getNextGroup(lData, tipStates)
		} yield {
			
		}
	}
	
	/** Iterate through lData one item at a time */
	def x1(lData: List[ItemB], lScore: ): Unit = {
		lData match {
			case data :: rest =>
				create 
		}
		getNextGroup(lData)
	}
	
	/** Create pipette cycle for the given group */
	def createCommands(lData: List[ItemData], tipStates0: Map[TipConfigL2, TipState]): Result[Seq[Command]] = {
		chooseTips(lData, tipStates0)
		if tips couldn't be chosen, wash then and try again
		chooseSources()
		create PipetteItems
	}
	
	def addCommandsToTable() {
		calculate cost of those commands
		if prior path cost + commands cost less than what's already in table for the appropriate entry, add it to the table
	}
	
	/*case class State3LiquidInfo(
		val liquid: Liquid,
		val tipModel: TipModel,
		val nTips: Int,
		val lData: List[ItemData]
	)*/
	
	case class GroupState(
		val lrData: List[ItemData],
		val map: Map[Tuple2[Liquid, TipModel], List[L3A_PipetteItem]],
		val tipModelCounts: Map[TipModel, Int]
		//val liquids: Map[Liquid, State3LiquidInfo],
		//val tipModelCounts: Map[TipModel, Int]
	)
	
	case class Group(
		val lData: List[ItemData]
	)
	
	//class Cycle(items: Seq[L3A_PipetteItem], mapLiquidToTipModel: Map[Liquid, TipModel], )
	
	def createGroup(data: ItemData): Result[Group] = {
		
	}
	
	def createGroup(group0: Group, data: ItemData): Result[Option[Group]] = {
		addItem(group0, data) match {
			case Error(lsError) => return Error(lsError)
			case Success(None) => false
			case Success(Some(state1)) =>
				state = state1
				true
		}
	}
	
	//def x2(group0: Seq[Step2], items: Seq[Step2], tipStates: Map[TipConfigL2, TipState]): Seq[Step2] = {
	/** Get the longest possible list of items from the head of lData which can be pipetted at once */ 
	def getNextGroup(lDataAll: Seq[ItemData]): Result[Seq[ItemData]] = {
		if (lDataAll.isEmpty)
			return Success(Seq())
			
		val state0 = new State3(Nil, Map(), Map())
		var state = state0
		Success(lDataAll.takeWhile(data => {
			addItem(state, data) match {
				case Error(lsError) => return Error(lsError)
				case Success(None) => false
				case Success(Some(state1)) =>
					state = state1
					true
			}
		}))
	}
	
	/** Add item to state0.  If successful, return a new state object */
	def addItem(state0: State3, data: ItemData): Result[Option[State3]] = {
		val lt = (data.liquid -> data.tipModel)
		val items = data.item :: state0.map.getOrElse(lt, Nil)
		val map = state0.map.updated(lt, items)
			
		for {
			tipModelCounts <- checkTips(map)
		} yield {
			if (tipModelCounts.isEmpty)
				None
			else {
				val state = State3(
					lrData = data :: state0.lrData,
					map = map,
					tipModelCounts)
				Some(state)
			}
		} 
	}
	
	/** Check whether the robot has enough tips to accommodate the given list of liquid+tipModel+items */
	def checkTips(map: Map[Tuple2[Liquid, TipModel], List[L3A_PipetteItem]]): Result[Map[TipModel, Int]] = {
		val map0 = Map[TipModel, Int]()
		val tipModelCounts = map.foldLeft(map0)((acc, entry) => {
			val ((liquid, tipModel), items) = entry
			val nTips = countTips(tipModel, liquid, items)
			val nTipsTotal = nTips + acc.getOrElse(tipModel, 0)
			acc.updated(tipModel, nTipsTotal)
		})
		for { b <- device.supportTipModelCounts(tipModelCounts) }
		yield { if (b) tipModelCounts else Map() }
	}
	
	/** Count the number of tips required to pipette the given liquid to the given list of wells */
	def countTips(tipModel: TipModel, liquid: Liquid, items: List[L3A_PipetteItem]): Int = {
		// Fill up tips of the given model to see how many we need
		val acc0 = (0.0, 0) // (currently tip, 0 tips already filled)
		val (nCurrentVolume, nTips) = items.foldLeft(acc0)((acc, item) => {
			if (item.nVolume <= 0) {
				acc
			}
			else {
				assert(item.nVolume >= tipModel.nVolumeAspirateMin)
				val nTipVolume = acc._1 + item.nVolume
				if (nTipVolume > tipModel.nVolume)
					(item.nVolume, acc._2 + 1)
				else
					(nTipVolume, acc._2)
			}
		})
		if (nCurrentVolume == 0)
			nTips
		else
			nTips + 1
	}
	
	/*def translate(tipGroups: Seq[Seq[TipConfigL2]]): Result[Seq[Command]] = {
		
		lnCostMin = new Array(nItems)
		Success(Seq())
	}
	
	def x2(liItem: Array[Int], tipGroups: Seq[Seq[TipConfigL2]]) {
		val nStates = liItem.size
		val frontier = new Queue[Int] 
	}*/

	def chooseTips(cycle: State3, tipStates0: Map[TipConfigL2, TipState]): Result[Map[ItemData, Tip]] = {
		
		Error("")
	}
*/
}
