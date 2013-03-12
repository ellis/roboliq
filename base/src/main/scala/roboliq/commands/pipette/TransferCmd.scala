package roboliq.commands.pipette

import scala.collection.immutable.SortedSet
import scalaz._
import Scalaz._
import roboliq.core._,roboliq.entity._,roboliq.processor._,roboliq.events._
import roboliq.devices.pipette.PipetteDevice


case class TransferCmd(
	description_? : Option[String],
	source: List[Source],
	destination: List[Source],
	amount: List[LiquidVolume],
	pipettePolicy_? : Option[String] = None,
	// TODO: add tipPolicy_? too (tip handling overrides)
	preMixSpec_? : Option[MixSpecOpt] = None,
	postMixSpec_? : Option[MixSpecOpt] = None
)

class TransferHandler extends CommandHandler[TransferCmd]("pipette.transfer") {
	def handleCmd(cmd: TransferCmd): RqReturn = {
		val source_l = (cmd.source ++ cmd.destination).distinct
		val l1 = cmd.source.zip(cmd.destination).zip(cmd.amount)
		val l = l1.map(tuple => (tuple._1._1, tuple._1._2, tuple._2))
		fnRequireList(source_l.map(source => lookup[VesselSituatedState](source.id))) { vss_l =>
			val vss_m = vss_l.map(vss => vss.id -> vss).toMap
			fnRequire(
				lookup[PipetteDevice]("default"),
				lookupAll[TipModel],
				lookupAll[TipState]
			) { (device, tipModel_l, tip_l) =>
				val item_l = l.map(tuple => {
					val (src, dst, volume) = tuple
					TransferPlanner.Item(vss_m(src.id), vss_m(dst.id), volume)
				})
				
				val itemToLiquid_m = item_l.map(item => item -> item.dst.liquid).toMap
				val itemToModels_m = item_l.map(item => item -> device.getDispenseAllowableTipModels(tipModel_l, item.src.liquid, item.volume)).toMap
				
				val tipModelSearcher = new scheduler.TipModelSearcher1[TransferPlanner.Item, Liquid, TipModel]
				
				for {
					itemToTipModel_m <- tipModelSearcher.searchGraph(item_l, itemToLiquid_m, itemToModels_m)
					tipModel = itemToTipModel_m.values.toSet.head
					group_l <- TransferPlanner.searchGraph(
						device,
						SortedSet(tip_l.map(_.conf).toSeq : _*),
						tipModel,
						PipettePolicy("POLICY", PipettePosition.Free),
						item_l
					)
					cmd_l = makeGroups(cmd, item_l, group_l, tip_l)
					ret <- output(
						cmd_l
					)
				} yield ret
			}
		}
	}
	
	private def makeGroups(cmd: TransferCmd, item_l: List[TransferPlanner.Item], group_l: List[Int], tip_l: List[TipState]): List[Cmd] = {
		var rest = item_l
		group_l.flatMap(n => {
			val item_l_# = rest.take(n)
			rest = rest.drop(n)
			(item_l_# zip tip_l).map(pair => {
				val (item, tip) = pair
				val policy = PipettePolicy(cmd.pipettePolicy_?.get, PipettePosition.WetContact)
				val twvpA = TipWellVolumePolicy(tip, item.src, item.volume, policy)
				low.AspirateCmd(None, List(twvpA))
			}) ++
			(item_l_# zip tip_l).map(pair => {
				val (item, tip) = pair
				val policy = PipettePolicy(cmd.pipettePolicy_?.get, PipettePosition.WetContact)
				val twvpA = TipWellVolumePolicy(tip, item.dst, item.volume, policy)
				low.DispenseCmd(None, List(twvpA))
			})
		})
	}
}
