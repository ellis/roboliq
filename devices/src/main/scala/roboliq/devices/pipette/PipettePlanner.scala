/*package roboliq.devices.pipette

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
	
	case class ItemData(item: L3A_PipetteItem, liquid: Liquid, tipModel: TipModel)
	
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
	def toItemData1(layers: Seq[Seq[L3A_PipetteItem]]): Seq[ItemData] = {
		layers.flatMap(items => {
			val mapLiquidToTipModel = chooseTipModels(items)
			toItemData2(items)
		})
	}
	
	/** For each item, find the source liquid and choose a tip model */
	def toItemData2(items: Seq[L3A_PipetteItem]): Seq[ItemData] = {
		val mapLiquidToTipModel = chooseTipModels(items)
		items.map(item => {
			val liquid = item.srcs.head.state(ctx.states).liquid
			val tipModel = mapLiquidToTipModel(liquid)
			ItemData(item, liquid, tipModel)
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
	def x1(lData: List[ItemData]): Unit = {
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
	
	case class State3(
		val lrData: List[ItemData],
		val map: Map[Tuple2[Liquid, TipModel], List[L3A_PipetteItem]]
		//val liquids: Map[Liquid, State3LiquidInfo],
		//val tipModelCounts: Map[TipModel, Int]
	)
	
	//class Cycle(items: Seq[L3A_PipetteItem], mapLiquidToTipModel: Map[Liquid, TipModel], )
	
	//def x2(group0: Seq[Step2], items: Seq[Step2], tipStates: Map[TipConfigL2, TipState]): Seq[Step2] = {
	/** Get the longest possible list of items from the head of lData which can be pipetted at once */ 
	def getNextGroup(lDataAll: Seq[ItemData]): Result[Seq[ItemData]] = {
		if (lDataAll.isEmpty)
			return Success(Seq())
			
		val state0 = new State3(Nil, Map())
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
		val state = State3(
			lrData = data :: state0.lrData,
			map = state0.map.updated(lt, items))
			
		for { bCanAdd <- checkTips(state.map) }
		yield { if (bCanAdd) Some(state) else None } 
	}
	
	/** Check whether the robot has enough tips to accommodate the given list of liquid+tipModel+items */
	def checkTips(map: Map[Tuple2[Liquid, TipModel], List[L3A_PipetteItem]]): Result[Boolean] = {
		val map0 = Map[TipModel, Int]()
		val tipModelCounts = map.foldLeft(map0)((acc, entry) => {
			val ((liquid, tipModel), items) = entry
			val nTips = countTips(tipModel, liquid, items)
			val nTipsTotal = nTips + acc.getOrElse(tipModel, 0)
			acc.updated(tipModel, nTipsTotal)
		})
		device.supportTipModelCounts(tipModelCounts)
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

	def chooseTips(lData: List[ItemData], tipStates0: Map[TipConfigL2, TipState]): Result[Map[ItemData, Tip]] = {
		
	}
}
*/