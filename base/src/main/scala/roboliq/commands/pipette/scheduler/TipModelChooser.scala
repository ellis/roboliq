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

	def chooseTipModels_OneForAll(
		device: PipetteDevice,
		item_l: Seq[Item],
		mItemToState: Map[Item, ItemState]
	): Result[Map[Liquid, TipModel]] = {
		if (item_l.isEmpty)
			return Success(Map())

		println("----- TipModelChooser")
		val liquidToItems_m = item_l.groupBy(item => {
			mItemToState(item).srcContent.liquid
		})
		val liquidToItemCount_m = liquidToItems_m.mapValues(_.size)
		val liquidToVolumes_m: Map[Liquid, MinMaxOption[LiquidVolume]] = liquidToItems_m.mapValues(item_l => {
			item_l.foldLeft(MinMaxOption[LiquidVolume](None))((a,item) => a + item.nVolume)
		})
		val liquidToModels_m: Map[Liquid, Seq[TipModel]] = for ((liquid, volumes) <- liquidToVolumes_m) yield {
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
				return Error(s"Cannot find a tip model for pipetting liquid `${liquid.id}` for both volumes $volume1 and $volume2")
			
			(liquid, tipModels)
		}
		val liquidToModelInfos_m: Map[Liquid, List[(TipModel, Int, Int)]] = liquidToModels_m.map(pair => {
			val (liquid, model_l) = pair
			liquid -> model_l.toList.zipWithIndex.map(pair => {
				val (model, rank) = pair
				val count = liquidToItemCount_m.getOrElse(liquid, 0)
				(model, rank, count)
			})
		})
		val tipModelInfos_l = liquidToModelInfos_m.values.flatten.toList
		val rank_l = makeRankList(tipModelInfos_l, Map())
		val tipModelRank = rank_l.head
		if (tipModelRank.nTotal != item_l.size)
			return Error("Could not find a single tip model which can be used for all pipetting steps.")
		
		println(s"liquidToVolumes_m: ${liquidToVolumes_m}")
		println(s"liquidToModels_m: ${liquidToModels_m}")
		println(s"liquidToModelInfos_m: ${liquidToModelInfos_m}")
		println(s"tipModelRank: $tipModelRank")
			
		val tipModel = tipModelRank.tipModel
		Success(liquidToItems_m.mapValues(_ => tipModel))
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
