package roboliq.commands.pipette.scheduler

import scala.collection.immutable.SortedSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import roboliq.core._
import roboliq.commands._
import roboliq.commands.pipette._
import roboliq.devices.pipette._


object Preprocessor {
	/**
	 * Remove items with nVolume <= 0
	 */
	def filterItems(items: Seq[Item]): Result[Seq[Item]] = {
		Success(items.filter(!_.nVolume.isEmpty))
	}

	private def createItemState(item: Item, builder: StateBuilder): ItemState = {
		val src = item.srcs.head
		val content = src.wellState(builder).get.content
		val dest = item.dest
		val state0 = dest.wellState(builder).get
		
		// Remove from source and add to dest
		src.stateWriter(builder).remove(item.nVolume)
		dest.stateWriter(builder).add(content, item.nVolume)
		val state1 = dest.wellState(builder).get
		
		ItemState(item, content, state0, state1)
	}
	
	def getItemStates(items: Seq[Item], state0: RobotState): Map[Item, ItemState] = {
		val builder = state0.toBuilder
		items.map(item => item -> createItemState(item, builder)).toMap
	}
	
	/*
	 * TODO: Change from Liquid->TipModel to Item->TipModel for cases where the volumes to be
	 * dispensed cannot be dispensed by a single tip model.  Still try to use a single tip model, though.
	 * FIXME: need to preserve preference order for tips in this function -- ellis, 2012-08-19
	 * Given a list [(tipModel, liquid, rank, count)] sorted by count, rank (count = number of items)
	 * Each liquid needs to be assigned a single tip model. 
	 * pick the tip models in two steps:
	 * 	check if any tip model is abl
	 * (liquid, tipModel,
	 */
	private def chooseTipModels(device: PipetteDevice, items: Seq[Item], mItemToState: Map[Item, ItemState]): Result[Map[Liquid, TipModel]] = {
		if (items.isEmpty)
			return Success(Map())
			
		val mapLiquidToVolumes: Map[Liquid, MinMaxOption[LiquidVolume]] = {
			items.map(item => {
				val itemState = mItemToState(item)
				val liquid = itemState.srcContent.liquid
				liquid -> item.nVolume
			}).groupBy(_._1).mapValues(_.map(_._2).foldLeft(MinMaxOption[LiquidVolume](None))((a,v) => a + v))
		}
		val mapLiquidToModels: Map[Liquid, Seq[TipModel]] = for ((liquid, volumes) <- mapLiquidToVolumes) yield {
			val (volume1, volume2) = volumes.option.get
			val tipModels1 = device.getDispenseAllowableTipModels(liquid, volume1)
			val tipModels2 = device.getDispenseAllowableTipModels(liquid, volume2)
			// Find the tip models which can be used for both minimum and maximum volumes
			val tipModels = tipModels1.intersect(tipModels2)

			if (tipModels1.isEmpty)
				return Error("Cannot find a tip model for pipetting liquid `"+liquid.id+"` at volume "+volume1)
			else if (tipModels2.isEmpty)
				return Error("Cannot find a tip model for pipetting liquid `"+liquid.id+"` at volume "+volume2)
			else if (tipModels.isEmpty)
				return Error(s"Cannot find a tip model for pipetting liquid `${liquid.id}` between volumes $volume1 and $volume2")
			
			(liquid, tipModels)
		}
		val lLiquidAll = mapLiquidToModels.keySet
		val lTipModelAll = mapLiquidToModels.values.flatten.toSet
		val lTipModelOkForAll = device.getTipModels.filter(tipModel => lTipModelAll.contains(tipModel) && mapLiquidToModels.forall(pair => pair._2.contains(tipModel)))
		if (device.areTipsDisposable && !lTipModelOkForAll.isEmpty) {
			val tipModel = lTipModelOkForAll.head
			Success(lLiquidAll.map(_ -> tipModel).toMap)
		}
		else {
			val mapLiquidToModel = new HashMap[Liquid, TipModel]
			val lLiquidsUnassigned = new HashSet() ++ lLiquidAll
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
				val lModelToCount = mapModelToCount.toList.sortBy(pair => pair._2)
				// List of the tip models with the highest frequency
				val lTipModel = lModelToCount.takeWhile(_._2 == lModelToCount.head._2).map(_._1)
				val tipModel = mapModelToCount.toList.sortBy(pair => pair._2).head._1
				val liquids = lLiquidsUnassigned.filter(liquid => mapLiquidToModels(liquid).contains(tipModel))
				///println("liquids: "+liquids)
				mapLiquidToModel ++= liquids.map(_ -> tipModel)
				// FIXME: for debug only
				//println("mapModelToCount: "+mapModelToCount)
				if (liquids.isEmpty) {
					println("DEBUG:")
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
			Success(mapLiquidToModel.toMap)
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
	 * For each item, find the source liquid and choose a tip model, 
	 * and split the item if its volume is too large for its tip model.
	 * Assumes that items have already been run through filterItems().
	 * 
	 * @return list of items with guarantee of sufficiently small volumes, new map from item to state, and map from item to LM (liquid and tip model).
	 */
	def assignLMs(
		items: Seq[Item],
		mItemToState: Map[Item, ItemState],
		device: PipetteDevice,
		state0: RobotState
	): Result[Tuple3[Seq[Item], Map[Item, ItemState], Map[Item, LM]]] = {
		for {
			mapLiquidToTipModel <- chooseTipModels(device, items, mItemToState)
		} yield {
			var bRebuild = false
			val lLM = items.flatMap(item => {
				val itemState = mItemToState(item)
				val liquid = itemState.srcContent.liquid
				// FIXME: for debug only
				if (!mapLiquidToTipModel.contains(liquid))
					println("mapLiquidToTipModel: "+mapLiquidToTipModel)
				// ENDFIX
				val tipModel = mapLiquidToTipModel(liquid)
				bRebuild |= (item.nVolume > tipModel.nVolume)
				// Update destination liquid (volume doesn't actually matter)
				//item.dest.obj.stateWriter(states).add(liquid, item.nVolume)
				// result
				splitBigVolumes(item, tipModel).map(item => (item, LM(liquid, tipModel)))
			})
			
			val items1 = lLM.map(_._1)
			val mLM = lLM.toMap
			
			// Need to create ItemState objects for any items which were split due to large volumes
			val builder = state0.toBuilder
			val mItemToState1 = items1.map(item => {
				mItemToState.get(item) match {
					case Some(itemState) => item -> itemState
					case _ => item -> createItemState(item, builder)
				}
			}).toMap
			
			(items1, mItemToState1, mLM)
		}
	}

	/**
	 * Split an item into a list of items with volumes which can be pipetted by the given tipModel.
	 */
	private def splitBigVolumes(item: Item, tipModel: TipModel): Seq[Item] = {
		if (item.nVolume <= tipModel.nVolume) Seq(item)
		else {
			val n = math.ceil((item.nVolume.ul / tipModel.nVolume.ul).toDouble).asInstanceOf[Int]
			val nVolume = item.nVolume / n
			val l = List.tabulate(n)(i => new Item(item.srcs, item.dest, nVolume, item.premix_?, if (i == n - 1) item.postmix_? else None))
			l
		}
	}
}
