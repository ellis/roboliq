package roboliq.commands.pipette.scheduler

import scalaz.{Success => _, _}
import scalaz.Scalaz._

import scala.collection.mutable.HashMap
import scala.collection.immutable.SortedSet
import scala.collection.JavaConversions._

import roboliq.core._
import roboliq.core.Core._
import roboliq.commands.pipette._
import roboliq.devices.pipette.PipetteDevice

sealed trait PipetteStep
case class PipetteStep_Clean(mTipToSpec: Map[Tip, CleanSpec2]) extends PipetteStep
case class PipetteStep_Aspirate(item_l: Seq[Item]) extends PipetteStep
case class PipetteStep_Dispense(item_l: Seq[Item]) extends PipetteStep


/**
 * The steps in a step group are performed in the following order:
 * all pre-cleans, all pre-mixes, all aspirates, all dispenses, all post-mixes, all post-cleans.
 */
case class PipetteGroup(
	clean_l: Seq[PipetteStep_Clean],
	aspirate_l: Seq[PipetteStep_Aspirate],
	dispense_l: Seq[PipetteStep_Dispense]
)

object PipetteGroupMonoid extends Monoid[PipetteGroup] {
	val zero = PipetteGroup(Nil, Nil, Nil)
	def append(a: PipetteGroup, b: => PipetteGroup): PipetteGroup = {
		PipetteGroup(
			a.clean_l ++ b.clean_l,
			a.aspirate_l ++ b.aspirate_l,
			a.dispense_l ++ b.dispense_l
		)
	}
}

case class PipetteGroupData(
	group: PipetteGroup,
	itemToTip_m: Map[Item, Tip],
	tipToSrc_m: Map[Tip, Well2],
	tipToVolume_m: Map[Tip, LiquidVolume],
	aspiratePolicy_m: Map[Tip, PipettePolicy],
	dispensePolicy_m: Map[Well2, PipettePolicy],
	preMixSpec_m: Map[Tip, MixSpec],
	postMixSpec_m: Map[Well2, MixSpec]
)


abstract class PipetteItemGrouper {
	def groupItems(
		item_l: Seq[Item],
		mItemToState: Map[Item, ItemState],
		mLM: Map[Item, LM],
		device: PipetteDevice,
		ctx: ProcessorContext
	): Result[Seq[PipetteGroupData]]
}

/*
case class PipetteCmdData(
	item_l: Seq[Item],
	itemToState_m: Map[Item, ItemState],
	itemToLM_m: Map[Item, LM]
)

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
*/

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
	): Result[(PipetteCmd, Seq[CmdBean])] = {
		val state0 = ctx.states
		for {
			cmd <- PipetteCmd.fromBean(bean, state0)
			items0 <- Preprocessor.filterItems(cmd.items)
			mItemToState0 = Preprocessor.getItemStates(items0, state0)
			tuple <- Preprocessor.assignLMs(items0, mItemToState0, device, state0)
			(item_l, mItemToState, mLM) = tuple
			groupData_l <- grouper.groupItems(item_l, mItemToState, mLM, device, ctx)
			// TODO: optimize cleaning
			cmd_l <- createCommands(groupData_l, device)
		} yield (cmd, cmd_l)
	}
	
	def createCommands(
		groupData_l: Seq[PipetteGroupData],
		device: PipetteDevice
	): Result[Seq[CmdBean]] = {
		roboliq.core.Success(groupData_l.flatMap{ groupData =>
			 groupToCommands(device)(groupData) match {
				 case roboliq.core.Success(l) => l
				 case roboliq.core.Error(ls) => return Error(ls)
			 }
		})
	}
	
	private def groupToCommands(
		device: PipetteDevice
	)(
		groupData: PipetteGroupData
	): Result[Seq[CmdBean]] = {
		val group = groupData.group
		val l1 = group.clean_l.flatMap(step => createCleanCommand(step, device))
		val l2 = group.aspirate_l.map(step => createAspirateCmd(step, groupData))
		val l3 = group.dispense_l.map(step => createDispenseCmd(step, groupData))
		Success(l1 ++ l2 ++ l3)
	}

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
	
	private def createAspirateCmd(
		step: PipetteStep_Aspirate,
		groupData: PipetteGroupData
	): CmdBean = {
		val bean = new AspirateCmdBean
		bean.items = step.item_l.map(item => createAspirateCmdItem(item, groupData))
		bean
	}
	
	
	private def createAspirateCmdItem(
		item: Item,
		groupData: PipetteGroupData
	): SpirateCmdItemBean = {
		val bean = new SpirateCmdItemBean
		val tip = groupData.itemToTip_m(item)
		val src = groupData.tipToSrc_m(tip)
		val mixSpec_? = groupData.preMixSpec_m.get(tip)
		bean.tip = tip.id
		bean.well = src.id
		bean.volume = groupData.tipToVolume_m(tip).l.bigDecimal
		bean.policy = groupData.aspiratePolicy_m(tip).id
		bean.mixSpec = mixSpec_?.map(MixSpec.toBean).orNull
		bean
	}
	
	private def createDispenseCmd(
		step: PipetteStep_Dispense,
		groupData: PipetteGroupData
	): CmdBean = {
		val bean = new DispenseCmdBean
		bean.items = step.item_l.map(item => createDispenseCmdItem(item, groupData))
		bean
	}
	
	private def createDispenseCmdItem(
		item: Item,
		groupData: PipetteGroupData
	): SpirateCmdItemBean = {
		val bean = new SpirateCmdItemBean
		val tip = groupData.itemToTip_m(item)
		val mixSpec_? = groupData.preMixSpec_m.get(tip)
		bean.tip = tip.id
		bean.well = item.dest.id
		bean.volume = item.nVolume.l.bigDecimal
		bean.policy = groupData.dispensePolicy_m(item.dest).id
		bean.mixSpec = mixSpec_?.map(MixSpec.toBean).orNull
		bean
	}
}

object PipetteUtils {
	def createPipetteGroup(item: Item): PipetteGroup = {
		val item_l = Seq(item)
		PipetteGroup(
			Seq(),
			Seq(PipetteStep_Aspirate(item_l)),
			Seq(PipetteStep_Dispense(item_l))
		)
	}
	
	def createPipetteGroups(item_l: Seq[Item]): Seq[PipetteGroup] = {
		item_l map createPipetteGroup
	}
}
