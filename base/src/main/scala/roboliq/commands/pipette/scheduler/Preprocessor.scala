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
			mapLiquidToTipModel <- TipModelChooser.chooseTipModels_OneForAll(device, items, mItemToState)
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
