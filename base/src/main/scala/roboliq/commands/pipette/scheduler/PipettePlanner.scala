package roboliq.commands.pipette.scheduler

import scalaz._
import Scalaz._

import roboliq.core.ProcessorContext
import roboliq.devices.pipette.PipetteDevice
import roboliq.core.RobotState
import roboliq.commands.pipette.PipetteCmdBean
import roboliq.core.StateQuery
import roboliq.core.Result
import roboliq.core.CmdBean

sealed trait PipetteStep
case class PipetteStep_PreClean(item_l: Seq[Item]) extends PipetteStep
case class PipetteStep_PreMix(item_l: Seq[Item]) extends PipetteStep
case class PipetteStep_Aspirate(item_l: Seq[Item]) extends PipetteStep
case class PipetteStep_Dispense(item_l: Seq[Item]) extends PipetteStep
case class PipetteStep_PostMix(item_l: Seq[Item]) extends PipetteStep
case class PipetteStep_PostClean(item_l: Seq[Item]) extends PipetteStep


/**
 * The steps in a step family are performed in the following order:
 * all pre-cleans, all pre-mixes, all aspirates, all dispenses, all post-mixes, all post-cleans.
 */
case class PipetteGroup(
	preClean_l: Seq[PipetteStep_PreClean],
	preMix_l: Seq[PipetteStep_PreMix],
	aspirate_l: Seq[PipetteStep_Aspirate],
	dispense_l: Seq[PipetteStep_Dispense],
	postMix_l: Seq[PipetteStep_PostMix],
	postClean_l: Seq[PipetteStep_PostClean]
)

object PipetteStepFamilyMonoid extends Monoid[PipetteGroup] {
	def zero = PipetteGroup(Nil, Nil, Nil, Nil, Nil, Nil)
	def append(a: PipetteGroup, b: PipetteGroup): PipetteGroup = {
		PipetteGroup(
			a.preClean_l ++ b.preClean_l,
			a.preMix_l ++ b.preMix_l,
			a.aspirate_l ++ b.aspirate_l,
			a.dispense_l ++ b.dispense_l,
			a.postMix_l ++ b.postMix_l,
			a.postClean_l ++ b.postClean_l
		)
	}
}

private class PipettePlanner(
	val device: PipetteDevice,
	val ctx: ProcessorContext,
	val cmd: L3C_Pipette
) {
	def help {
		val res = for {
			items0 <- Preprocessor.filterItems(cmd.args.items)
			mItemToState0 = Preprocessor.getItemStates(items0, ctx.states)
			tuple <- Preprocessor.assignLMs(items0, mItemToState0, device, ctx.states)
			(items, mItemToState, mLM) = tuple
			groups <- groupItems
		} yield {
		}
	}
}

case class PipetteCmdData(
	item_l: Seq[Item],
	itemToState_m: Map[Item, ItemState],
	itemToLM_m: Map[Item, LM]
)



abstract class PipetteSearcher {
	def optimizeGroup(group: PipetteGroup): Result[PipetteGroup]
	def optimizeGroups(group_l: Seq[PipetteGroup]): Result[Seq[PipetteGroup]]
	/**
	 * Join the groups and determine which 
	 */
	def joinGroups(
		group_l0: Seq[PipetteGroup],
		data: PipetteCmdData,
		device: PipetteDevice,
		ctx: ProcessorContext
	): Result[Tuple2[Seq[PipetteGroup], Map[Item, Int]]
}

/*
What I'm trying to do:

have a set of functions which optimize individual PipetteGroups,
e.g. join adjacent aspirates and dispenses into single aspirate and dispense commands.
-- probably better to have functions to optimize lists of specific PipetteSteps.

optimize a list of PipetteGroups.
e.g. join adjacent groups together if possible.
Some joins are possible but not desirable.
Complicating this is the fact that we need to work with concrete sets of tips.

optimize a list of PipetteGroups by moving steps around
e.g. move some cleans forward or backward in time.
*/

class PipettePlanner(
	val searcher: PipetteSearcher
) {
	def findPipetteSteps(bean: PipetteCmdBean, device: PipetteDevice, ctx: ProcessorContext): Result[Seq[PipetteStep]] = {
		for {
			data <- PipettePlanner.createData(bean, device, ctx.states)
			groups = PipettePlanner.createPipetteGroups(data.item_l)
			step_l <- searcher.findPipetteSteps(data, device, ctx)
		} yield step_l
	}
	
	def run(bean: PipetteCmdBean, device: PipetteDevice, ctx: ProcessorContext): Result[Seq[CmdBean]] = {
		for {
			step_l0 <- findPipetteSteps(bean, device, ctx)
			step_l = optimizeCleaning(step_l0)
			cmd_l <- stepsToCommands(step_l)
		} yield cmd_l
	}
	
	def optimizeCleaning(step_l0: Seq[PipetteStep]): Seq[PipetteStep] = {
		
	}
	
	def stepsToCommands(step_l: Seq[PipetteStep]): Result[Seq[CmdBean]] = {
		
	}
}

object PipettePlanner {
	def createData(
		bean: PipetteCmdBean,
		device: PipetteDevice,
		state0: RobotState
	): Result[PipetteCmdData] = {
		for {
			cmd <- PipetteCmd.fromBean(bean, state0)
			items0 <- Preprocessor.filterItems(cmd.items)
			mItemToState0 = Preprocessor.getItemStates(items0, state0)
			tuple <- Preprocessor.assignLMs(items0, mItemToState0, device, state0)
			(items, mItemToState, mLM) = tuple 
		} yield {
			PipetteCmdData(items, mItemToState, mLM)
		}
	}
	
	def createPipetteGroup(item: Item): PipetteGroup = {
		val item_l = Seq(item)
		PipetteGroup(
			Seq(PipetteStep_PreClean(item_l)),
			Seq(PipetteStep_PreMix(item_l)),
			Seq(PipetteStep_Aspirate(item_l)),
			Seq(PipetteStep_Dispense(item_l)),
			Seq(PipetteStep_PostMix(item_l)),
			Seq(PipetteStep_PostClean(item_l))
		)
	}
	
	def createPipetteGroups(item_l: Seq[Item]): Seq[PipetteGroup] = {
		item_l map createPipetteGroup
	}
}
