package roboliq.commands.pipette.scheduler

import roboliq.core._
import roboliq.devices.pipette._

object TipModelChooser {

	private case class TipModelRank(tipModel: TipModel, nTotal: Int, map: Map[Int, Int]) extends Ordered[TipModelRank] {
		def incRank(rank: Int, count: Int): TipModelRank = {
			val map2 = map.+((rank, count + map.getOrElse(rank, 0)))
			new TipModelRank(tipModel, nTotal + count, map2)
		}
		
		override def compare(that: TipModelRank): Int = {
			if (nTotal != that.nTotal) that.nTotal - nTotal
			else {
				val rank_l = map.keySet.union(that.map.keySet).toList.sorted
				for (rank <- rank_l) {
					(map.get(rank), that.map.get(rank)) match {
						case (None, None) =>
						case (None, _) => return 1
						case (_, None) => return -1
						case (Some(a), Some(b)) =>
							if (a != b) return b - a
					}
				}
				0
			}
		}
	}

	private class CommonData(
		val itemToModels_m: Map[Item, Seq[TipModel]],
		val itemToModelInfos_m: Map[Item, Seq[(TipModel, Int, Int)]]
	)
	
	private def getCommonData(
		device: PipetteDevice,
		item_l: Seq[Item],
		mItemToState: Map[Item, ItemState]
	): Result[CommonData] = {
		val itemToModels_m = item_l.map(item => {
			val liquid = mItemToState(item).srcContent.liquid
			val tipModel_l = device.getDispenseAllowableTipModels(liquid, item.volume)
	
			if (tipModel_l.isEmpty)
				return Error(s"Cannot find a tip model for dispensing ${item.volume} of liquid `${liquid.id}`.")
			
			(item, tipModel_l)
		}).toMap

		val itemToModelInfos_m = itemToModels_m.mapValues(model_l => {
			model_l.zipWithIndex.map(pair => {
				val (model, rank) = pair
				(model, rank, 1)
			})
		})

		Success(new CommonData(itemToModels_m, itemToModelInfos_m))
	}
	
	def chooseTipModels_OneForAll(
		device: PipetteDevice,
		item_l: Seq[Item],
		mItemToState: Map[Item, ItemState]
	): Result[Map[Item, TipModel]] = {
		if (item_l.isEmpty)
			return Success(Map())

		for {
			common <- getCommonData(device, item_l, mItemToState)
		} yield {
			val tipModelInfos_l = common.itemToModelInfos_m.values.flatten.toList
			val rank_l = makeRankList(tipModelInfos_l, Map())
			val tipModelRank = rank_l.head
			if (tipModelRank.nTotal != item_l.size)
				return Error("Could not find a single tip model which can be used for all pipetting steps.")
				
			val liquidToItems_m = item_l.groupBy(item => {
				mItemToState(item).srcContent.liquid
			})
			val tipModel = tipModelRank.tipModel
			item_l.map(item => (item, tipModel)).toMap
		}
	}

	def chooseTipModels_OnePerLiquid(
		device: PipetteDevice,
		item_l: Seq[Item],
		mItemToState: Map[Item, ItemState]
	): Result[Map[Item, TipModel]] = {
		if (item_l.isEmpty)
			return Success(Map())

		for {
			common <- getCommonData(device, item_l, mItemToState)
		} yield {
			val liquidToItems_m = item_l.groupBy(item => {
				mItemToState(item).srcContent.liquid
			}).mapValues(_.toSet)
			
			val x = common.itemToModels_m.groupBy(pair => mItemToState(pair._1).srcContent.liquid)
			val liquidToModel_m = for ((liquid, itemToModels_l) <- x) yield {
				// Get the intersection of tip models which can be used for all items dispensing the given liquid
				val tipModel_ll = itemToModels_l.map(_._2.toSet)
				val tipModel_l = tipModel_ll.reduceLeft(_.intersect(_))
				if (tipModel_l.isEmpty)
					return Error(s"Cannot find a tip model which is compatible for dispensing `${liquid.id}` for the given range of volumes.")
				
				// Get the tipModel rankings for the current liquid and tips in tipModel_l
				val item_l = itemToModels_l.map(_._1)
				val info_l = item_l.flatMap(common.itemToModelInfos_m)
				val rank_l = makeRankList(info_l.toList).filter(x => tipModel_l.contains(x.tipModel))
				val rank = rank_l.head
				
				(liquid, rank.tipModel)
			}
			
			liquidToModel_m.toList.flatMap(pair => {
				liquidToItems_m(pair._1).map(_ -> pair._2)
			}).toMap
		}
	}

	private def makeRankList(l: List[(TipModel, Int, Int)]): List[TipModelRank] = {
		makeRankList(l, Map())
	}
	
	private def makeRankList(l: List[(TipModel, Int, Int)], acc: Map[TipModel, TipModelRank]): List[TipModelRank] = {
		l match {
			case Nil => acc.toList.map(_._2).sorted
			case (tipModel, rank, count) :: rest =>
				val rank0 = acc.getOrElse(tipModel, new TipModelRank(tipModel, 0, Map()))
				val rank1 = rank0.incRank(rank, count)
				val acc2 = acc + ((tipModel, rank1))
				makeRankList(rest, acc2)
		}
	}
}
