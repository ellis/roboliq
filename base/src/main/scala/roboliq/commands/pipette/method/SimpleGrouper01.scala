package roboliq.commands.pipette.method

import roboliq.core._
import roboliq.core.Core._
import roboliq.commands.pipette.scheduler._
import roboliq.devices.pipette.PipetteDevice


/**
 * Simplest possible grouping algorithm.
 * - One item per group: clean if necessary, aspirate, dispense
 * - Always use the first tip for a given tip model
 */
class SimpleGrouper01 extends PipetteItemGrouper {
	def groupItems(
		item_l: Seq[Item],
		mItemToState: Map[Item, ItemState],
		mLM: Map[Item, LM],
		device: PipetteDevice,
		ctx: ProcessorContext
	): Result[Seq[PipetteGroupData]] = {
		var states = ctx.states
		val builder = states.toBuilder
		val group_l = item_l map {item =>
			createGroup(item, mItemToState(item), mLM, device, ctx, builder) match {
				case Error(ls) => return Error(ls)
				case Success(group) => group
			}
		}
		Success(group_l)
	}
	
	private def createGroup(
		item: Item,
		itemState: ItemState,
		mLM: Map[Item, LM],
		device: PipetteDevice,
		ctx: ProcessorContext,
		builder: StateBuilder
	): Result[PipetteGroupData] = {
		val lm = mLM(item)
		val group = PipetteGroup(
			Nil,
			Seq(PipetteStep_Aspirate(Seq(item))),
			Seq(PipetteStep_Dispense(Seq(item)))
		)
		val volume = item.nVolume
		
		for {
			tip_l <- device.assignTips(device.getTips, lm.tipModel, 1)
			tip = tip_l.head
			src0_l <- builder.mapIdToWell2List(lm.liquid.id)
			src1 = src0_l.filter(src => 
				builder.findWellState(src.id).cata(st => st.nVolume > volume, false)
			)
			_ <- Result.assert(!src1.isEmpty, "source well(s) for `"+lm.liquid.id+"` do not contain enough liquid.")
			src = src1.head
			policy <- Result.get(device.getDispensePolicy(lm.liquid, lm.tipModel, volume, itemState.destState0), "Could not find pipette policy")
		} yield {
			tip.stateWriter(builder).aspirate(src, lm.liquid, volume)
			tip.stateWriter(builder).dispense(volume, itemState.destState0.liquid, policy.pos)
			src.stateWriter(builder).remove(volume)
			builder.map(item.dest.id) = itemState.destState1
			PipetteGroupData(
				group = group,
				itemToTip_m = Map(item -> tip),
				tipToSrc_m = Map(tip -> src),
				tipToVolume_m = Map(tip -> volume),
				aspiratePolicy_m = Map(tip -> policy),
				dispensePolicy_m = Map(item.dest -> policy),
				preMixSpec_m = Map(), // FIXME: handle pre-mix
				postMixSpec_m = Map() // FIXME: handle post-mix
			)
		}
	}
}