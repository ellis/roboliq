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
		
		val volume = item.volume
		
		for {
			tip_l <- device.assignTips(device.getTips, lm.tipModel, 1)
			tip = tip_l.head
			tipState0 <- builder.findTipState(tip.id)
			src0_l <- builder.mapIdToWell2List(lm.liquid.id)
			src1 = src0_l.filter(src => 
				builder.findWellState(src.id).cata(st => st.nVolume > volume, false)
			)
			_ <- Result.assert(!src1.isEmpty, "source well(s) for `"+lm.liquid.id+"` do not contain enough liquid.")
			src = src1.head
			policyDisp <- Result.get(device.getDispensePolicy(lm.liquid, lm.tipModel, volume, itemState.destState0), "Could not find pipette policy")
			//_ = println("tipLiquid1: "+tip.state(builder).srcsEntered)
			intensityPre <- PipetteUtils.getWashIntensityPre(item, lm.liquid, policyDisp, builder, tip, lm.tipModel, postmix_? = None, tipOverrides_? = None)
			//_ = println("tipLiquid2: "+tip.state(builder).srcsEntered)
		} yield {
			val group = PipetteGroup(
				Seq(PipetteStep_Clean(Map(tip -> intensityPre))),
				Seq(PipetteStep_Aspirate(Seq(item))),
				Seq(PipetteStep_Dispense(Seq(item)))
			)
			
			// Update states
			tip.stateWriter(builder).clean(intensityPre)
			tip.stateWriter(builder).aspirate(src, lm.liquid, volume)
			//println("tipLiquid3: "+tip.state(builder).srcsEntered)
			tip.stateWriter(builder).dispense(volume, itemState.destState0.liquid, policyDisp.pos)
			//println("tipLiquid4: "+tip.state(builder).srcsEntered)
			src.stateWriter(builder).remove(volume)
			builder.map(item.dest.id) = itemState.destState1
			//println("tipLiquid5: "+tip.state(builder).srcsEntered)
			
			PipetteGroupData(
				group = group,
				itemToTip_m = Map(item -> tip),
				tipToSrc_m = Map(tip -> src),
				tipToVolume_m = Map(tip -> volume),
				aspiratePolicy_m = Map(tip -> policyDisp), // Use the same policy for aspiration
				dispensePolicy_m = Map(item.dest -> policyDisp),
				preMixSpec_m = Map(), // FIXME: handle pre-mix
				postMixSpec_m = Map() // FIXME: handle post-mix
			)
		}
	}
}