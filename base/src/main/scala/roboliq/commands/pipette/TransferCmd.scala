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
	tipModel_? : Option[TipModel] = None,
	pipettePolicy_? : Option[String] = None,
	sterilityBefore_? : Option[CleanIntensity.Value] = None,
	sterilityBetween_? : Option[CleanIntensity.Value] = None,
	sterilityAfter_? : Option[CleanIntensity.Value] = None,
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

				val tipModel_? = cmd.tipModel_? match {
					case Some(tipModel) => RqSuccess(tipModel)
					case None =>
						val itemToLiquid_m = item_l.map(item => item -> item.dst.liquid).toMap
						val itemToModels_m = item_l.map(item => item -> device.getDispenseAllowableTipModels(tipModel_l, item.src.liquid, item.volume)).toMap
						val tipModelSearcher = new scheduler.TipModelSearcher1[TransferPlanner.Item, Liquid, TipModel]
						val itemToTipModel_m_? = tipModelSearcher.searchGraph(item_l, itemToLiquid_m, itemToModels_m)
						itemToTipModel_m_?.map(_.values.toSet.head)
				}
				
				// FIXME: need to make PipettePolicy and entity to be loaded from database
				val pipettePolicy = cmd.pipettePolicy_?.map(s => PipettePolicy(s, PipettePosition.Free)).getOrElse(PipettePolicy("POLICY", PipettePosition.Free))
				for {
					tipModel <- tipModel_?
					group_l <- TransferPlanner.searchGraph(
						device,
						SortedSet(tip_l.map(_.conf).toSeq : _*),
						tipModel,
						pipettePolicy,
						item_l
					)
					cmd_l = makeGroups(device, cmd, item_l, group_l, tip_l, pipettePolicy)
					ret <- output(
						cmd_l
					)
				} yield ret
			}
		}
	}
	
	private def makeGroups(
		device: PipetteDevice,
		cmd: TransferCmd,
		item_l: List[TransferPlanner.Item],
		group_l: List[Int],
		tip_l: List[TipState],
		policy: PipettePolicy
	): List[Cmd] = {
		var rest = item_l
		group_l.flatMap(n => {
			val item_l_# = rest.take(n)
			rest = rest.drop(n)
			// Create TipWellVolumePolicy lists from item and tip lists
			val twvpA_l = (item_l_# zip tip_l).map(pair => {
				val (item, tip) = pair
				TipWellVolumePolicy(tip, item.src, item.volume, policy)
			})
			val twvpD_l = (item_l_# zip tip_l).map(pair => {
				val (item, tip) = pair
				TipWellVolumePolicy(tip, item.dst, item.volume, policy)
			})
			// Group the TWVPs into groups that can be performed simultaneously
			val twvpA_ll = device.groupSpirateItems(twvpA_l)
			val twvpD_ll = device.groupSpirateItems(twvpD_l)
			
			val cleanA_m = twvpA_l.map(twvp => twvp.tip -> twvp.well.liquid.tipCleanPolicy).toMap
			val cleanD_m: Map[TipState, TipCleanPolicy] = {
				if (policy.pos == PipettePosition.WetContact)
					twvpD_l.map(twvp => twvp.tip -> twvp.well.liquid.tipCleanPolicy).toMap
				else
					Map()
			}
			val clean_m = cleanA_m |+| cleanD_m
			val preclean_m: Map[TipState, CleanIntensity.Value] = clean_m.mapValues(_.enter)
			val postclean_m: Map[TipState, CleanIntensity.Value] = clean_m.mapValues(_.enter)
			
			// Create mixing commands
			val premix_l = makeMixCmds(cmd.preMixSpec_?, twvpA_ll)
			val postmix_l = makeMixCmds(cmd.postMixSpec_?, twvpD_ll)
			// Create aspriate and dispense commands
			val asp_l = twvpA_ll.map(twvp_l => low.AspirateCmd(None, twvp_l))
			val disp_l = twvpD_ll.map(twvp_l => low.DispenseCmd(None, twvp_l)) 

			premix_l ++
			asp_l ++
			disp_l ++
			postmix_l
		})
	}

	private def makeSterilizeBeforeCmds(
		sterilizeBefore_? : Option[CleanIntensity.Value],
		twvpA_l: List[TipWellVolumePolicy]
	): List[TipsCmd] = {
		val cleanA_m = twvpA_l.map(twvp => twvp.tip -> twvp.well.liquid.tipCleanPolicy).toMap
		val cleanD_m: Map[TipState, TipCleanPolicy] = {
			if (policy.pos == PipettePosition.WetContact)
				twvpD_l.map(twvp => twvp.tip -> twvp.well.liquid.tipCleanPolicy).toMap
			else
				Map()
		}
		val clean_m = cleanA_m |+| cleanD_m
		val preclean_m: Map[TipState, CleanIntensity.Value] = clean_m.mapValues(_.enter)
		val postclean_m: Map[TipState, CleanIntensity.Value] = clean_m.mapValues(_.enter)
	}
	
	private def makeMixCmds(mixSpecOpt_? : Option[MixSpecOpt], twvp_ll: List[List[TipWellVolumePolicy]]): List[low.MixCmd] = {
		mixSpecOpt_? match {
			case None => Nil
			case _ =>
				twvp_ll.map(twvp_l => {
					val items = twvp_l.map(twvp => low.MixItem(twvp.tip, twvp.well, None))
					low.MixCmd(None, items, mixSpecOpt_?)
				})
		}
	}
}
