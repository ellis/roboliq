package roboliq.commands.pipette.scheduler

import scala.collection.mutable.HashMap
import scala.collection.immutable.SortedSet
import scala.collection.JavaConversions._

import scalaz._
import Scalaz._

import roboliq.core._
import roboliq.commands.pipette._
import roboliq.devices.pipette.PipetteDevice

sealed trait PipetteStep
case class PipetteStep_Clean(mTipToSpec: Map[Tip, CleanSpec2]) extends PipetteStep
case class PipetteStep_PreMix(item_l: Seq[Item]) extends PipetteStep
case class PipetteStep_Aspirate(item_l: Seq[Item]) extends PipetteStep
case class PipetteStep_Dispense(item_l: Seq[Item]) extends PipetteStep
case class PipetteStep_PostMix(item_l: Seq[Item]) extends PipetteStep


/**
 * The steps in a step group are performed in the following order:
 * all pre-cleans, all pre-mixes, all aspirates, all dispenses, all post-mixes, all post-cleans.
 */
case class PipetteGroup(
	clean_l: Seq[PipetteStep_Clean],
	preMix_l: Seq[PipetteStep_PreMix],
	aspirate_l: Seq[PipetteStep_Aspirate],
	dispense_l: Seq[PipetteStep_Dispense],
	postMix_l: Seq[PipetteStep_PostMix]
)

object PipetteStepGroupMonoid extends Monoid[PipetteGroup] {
	def zero = PipetteGroup(Nil, Nil, Nil, Nil, Nil)
	def append(a: PipetteGroup, b: PipetteGroup): PipetteGroup = {
		PipetteGroup(
			a.clean_l ++ b.clean_l,
			a.preMix_l ++ b.preMix_l,
			a.aspirate_l ++ b.aspirate_l,
			a.dispense_l ++ b.dispense_l,
			a.postMix_l ++ b.postMix_l
		)
	}
}

/*
private class PipettePlanner(
	val device: PipetteDevice,
	val ctx: ProcessorContext,
	val cmd: L3C_Pipette
) {
	def run {
		val res = for {
			items0 <- Preprocessor.filterItems(cmd.args.items)
			mItemToState0 = Preprocessor.getItemStates(items0, ctx.states)
			tuple <- Preprocessor.assignLMs(items0, mItemToState0, device, ctx.states)
			(items, mItemToState, mLM) = tuple
			group_l = itemsToGroupList(items)
		} yield group_l
	}
	
	private def itemsToGroupList(item_l: Seq[Item]): Seq[PipetteGroup] = {
		item_l map { item =>
			PipetteGroup(
				Seq(),
				Seq(PipetteStep_PreMix(Seq(item))), // Only if source should pre-mixed 
				Seq(PipetteStep_Aspirate(Seq(item))),
				Seq(PipetteStep_Dispense(Seq(item))),
				Seq(PipetteStep_PostMix(Seq(item))) // Only if dest should post-mixed
			)
		}
	}
	
	def itemsToOptimizedGroups(
		item_l: Seq[Item],
		mItemToState: Map[Item, ItemState],
		mLM: Map[Item, LM]
	): Result[Seq[PipetteGroup]] = {
		val group0_l = itemsToGroupList(item_l)
		for {
			group1_l <- initialGroupsToJoinedGroups(group0_l, mItemToState, mLM)
			group2_l <- joinedGroupsToOptimizedGroups(group1_l, mItemToState, mLM)
		} yield group2_l
	}
	
	def initialGroupsToJoinedGroups(
		group_l: Seq[PipetteGroup],
		mItemToState: Map[Item, ItemState],
		mLM: Map[Item, LM]
	): Result[Seq[PipetteGroup]]
	
	def joinedGroupsToOptimizedGroups(
		group_l: Seq[PipetteGroup],
		mItemToState: Map[Item, ItemState],
		mLM: Map[Item, LM]
	): Result[Seq[PipetteGroup]]
}
*/

case class PipetteCmdData(
	item_l: Seq[Item],
	itemToState_m: Map[Item, ItemState],
	itemToLM_m: Map[Item, LM]
)


abstract class PipetteItemGrouper {
	def groupItems(
		item_l: Seq[Item],
		mItemToState: Map[Item, ItemState],
		mLM: Map[Item, LM],
		device: PipetteDevice,
		ctx: ProcessorContext
	): Result[
		Tuple2[
			Seq[PipetteGroup],
			Map[Item, Tip]
		]
	]
}

abstract class PipetteSearcher {
	def optimizeGroup(group: PipetteGroup): Result[PipetteGroup]
	def optimizeGroups(group_l: Seq[PipetteGroup]): Result[Seq[PipetteGroup]]
	/**
	 * Join the groups and determine which tip to use for each item
	 */
	def joinGroups(
		group_l0: Seq[PipetteGroup],
		data: PipetteCmdData,
		device: PipetteDevice,
		ctx: ProcessorContext
	): Result[Tuple2[Seq[PipetteGroup], Map[Item, Int]]]
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
/*
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
*/

object PipettePlanner {
	def run(
		bean: PipetteCmdBean,
		grouper: PipetteItemGrouper,
		device: PipetteDevice,
		ctx: ProcessorContext
	): Result[Seq[CmdBean]] = {
		val state0 = ctx.states
		for {
			cmd <- PipetteCmd.fromBean(bean, state0)
			items0 <- Preprocessor.filterItems(cmd.items)
			mItemToState0 = Preprocessor.getItemStates(items0, state0)
			tuple <- Preprocessor.assignLMs(items0, mItemToState0, device, state0)
			(item_l, mItemToState, mLM) = tuple
			(group_l, mItemToTip) <- grouper.groupItems(item_l, mItemToState, mLM, device, ctx)
			cmd_l <- createCommands(group_l, mItemToTip)
		} yield cmd_l
	}
	
	def createCommands(
		group_l: Seq[PipetteGroup],
		mItemToTip: Map[Item, Tip]
	): Result[Seq[CmdBean]] = {
		group_l flatMap groupToCommands
	}
	
	private def groupToCommands(
		mItemToTip: Map[Item, Tip],
		device: PipetteDevice
	)(
		group: PipetteGroup
	): Result[Seq[CmdBean]] = {
		group.clean_l.map(step => createCleanCommand(step, device)) ++
		group.preMix_l.map(cc) ++
		group.aspirate_l.map(cc) ++
		group.dispense_l.map(cc) ++
		group.postMix_l.map(cc)
	}

	/*
	def createCommand(
		mItemToTip: Map[Item, Tip]
	)(
		step: PipetteStep,
	): Result[CmdBean] = {
		step match {
			case PipetteStep_PreClean(item_l) => createCleanCommand
			case PipetteStep_PreMix(item_l)
			case PipetteStep_Aspirate(item_l)
			case PipetteStep_Dispense(item_l)
			case PipetteStep_PostMix(item_l)
			case PipetteStep_PostClean(item_l)
		}
		
	}
	*/

	private def createCleanCommand(
		step: PipetteStep_Clean,
		device: PipetteDevice
	): Seq[CmdBean] = {
		val mTipToClean = step.mTipToSpec
		val mTipToModel = new HashMap[Tip, Option[TipModel]]
		val mTipToWash = new HashMap[Tip, WashSpec]
		for ((tip, cleanSpec) <- mTipToClean) {
			cleanSpec match {
				case ReplaceSpec2(tip, model) =>
					mTipToModel += (tip -> Some(model))
				case WashSpec2(tip, spec) =>
					mTipToWash(tip) = spec
				case DropSpec2(tip) =>
					mTipToModel += (tip -> None)
			}
		}
		
		val lReplace = Seq[CmdBean]() /* FIXME: {
			if (mTipToModel.isEmpty) Seq()
			else {
				val items = mTipToModel.toSeq.sortBy(_._1).map(pair => new L3A_TipsReplaceItem(pair._1, pair._2))
				Seq(L3C_TipsReplace(items))
			}
		}*/
		
		val lWash = {
			if (mTipToWash.isEmpty) Seq()
			else {
				val llTip = device.batchCleanTips(SortedSet(mTipToWash.keys.toSeq : _*))
				llTip.flatMap(lTip => {
					val intensity = lTip.foldLeft(WashIntensity.None)((acc, tip) => {
						val spec = mTipToWash(tip)
						WashIntensity.max(acc, spec.washIntensity)
					})
					val bean = new TipsWashCmdBean
					bean.tips = lTip.toList.map(_.id)
					bean.intensity = intensity.toString()
					Some(bean)
				})
			}
		}
		
		//println("mTipToClean: "+mTipToClean)
		//println("lWash: "+lWash)

		lReplace ++ lWash
	}
	
	private def createMixCommand(
		item_l: Seq[Item],
	): Seq[CmdBean] = {
		val bean = new MixCmdBean
		bean.items = (item_l.map)
	@BeanProperty var tip: String = null
	@BeanProperty var well: String = null
	@BeanProperty var mixSpec: MixSpecBean = null
		@BeanProperty var items: java.util.List[MixCmdItemBean] = null
		@BeanProperty var mixSpec: MixSpecBean = null
	}
	
	// QUESTION: At what level should commands be made into units
	
	//private def create MixSpec
}

object PipetteUtils {
	def createPipetteGroup(item: Item): PipetteGroup = {
		val item_l = Seq(item)
		PipetteGroup(
			Seq(),
			Seq(PipetteStep_PreMix(item_l)),
			Seq(PipetteStep_Aspirate(item_l)),
			Seq(PipetteStep_Dispense(item_l)),
			Seq(PipetteStep_PostMix(item_l))
		)
	}
	
	def createPipetteGroups(item_l: Seq[Item]): Seq[PipetteGroup] = {
		item_l map createPipetteGroup
	}
}
