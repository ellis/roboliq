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
		item_l.map(item => createGroup(item, mItemToState, mLM, device, ctx))
		Success(Nil)
	}
	
	private def createGroup(
		item: Item,
		mItemToState: Map[Item, ItemState],
		mLM: Map[Item, LM],
		device: PipetteDevice,
		ctx: ProcessorContext
	): Result[PipetteGroupData] = {
		Success(PipetteGroupData(
			group: PipetteGroup,
			itemToTip_m: Map[Item, Tip],
			tipToSrc_m: Map[Tip, Well2],
			tipToVolume_m: Map[Tip, LiquidVolume],
			aspiratePolicy_m: Map[Tip, PipettePolicy],
			dispensePolicy_m: Map[Well2, PipettePolicy],
			preMixSpec_m: Map[Tip, MixSpec],
			postMixSpec_m: Map[Well2, MixSpec]
		))

	}
}