package roboliq.devices.pipette

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._


private trait L3P_PipetteMixBase {
	type CmdType <: Command
	type L3A_ItemType
	
	type Errors = Seq[String]

	trait Action
	case class Aspirate(items: Seq[L2A_SpirateItem]) extends Action
	case class Dispense(items: Seq[L2A_SpirateItem]) extends Action
	case class Mix(tws: Seq[TipWell]) extends Action
	case class Clean(tips: Set[TipConfigL2]) extends Action
	
	sealed abstract class CleanSpec2 { val tip: TipConfigL2 }
	case class ReplaceSpec2(tip: TipConfigL2) extends CleanSpec2
	case class WashSpec2(tip: TipConfigL2, spec: WashSpec) extends CleanSpec2

	val robot: PipetteDevice
	val ctx: CompilerContextL3
	val cmd: CmdType

	trait CycleState {
		val tips: SortedSet[TipConfigL2]
		val state0: RobotState
		
		val gets = new ArrayBuffer[L3C_TipsReplace]
		val washs = new ArrayBuffer[L3C_TipsWash]
		val mixes = new ArrayBuffer[L2C_Mix]
		
		var ress: Seq[CompileFinal] = Nil

		def toTokenSeq: Seq[Command]
	}
	
	val dests: SortedSet[WellConfigL2]

	def translation: Either[CompileError, Seq[Command]] = {
		// Need to split into tip groups (e.g. large tips, small tips, all tips)
		// For each group, perform the pipetting and score the results
		// Pick the strategy with the best score
		var winner = Seq[Command]()
		var nWinnerScore = Int.MaxValue
		var lsErrors = new ArrayBuffer[String]
		val mapIndexToTip = robot.config.tips.map(tip => tip.index -> tip).toMap
		for (tipGroup <- robot.config.tipGroups) {
			val mapTipToType = tipGroup.map(pair => mapIndexToTip(pair._1).state(ctx.states).conf -> pair._2.sName).toMap
			val tips = SortedSet(mapTipToType.keys.toSeq : _*)
			//val tipAndSpecs = robot.config.tips.filter(tip => tipGroup.contains(tip.index)).map(tip => tip.state(ctx.states).conf)
	
			translateCommand(tips, mapTipToType) match {
				case Left(lsErrors2) =>
					lsErrors ++= lsErrors2
				case Right(Seq()) =>
				case Right(cycles) =>
					val cmds1 = cycles.flatMap(_.toTokenSeq) ++ dropSeq(tips)
					ctx.subCompiler.compile(ctx.states, cmds1) match {
						case Right(nodes) =>
							ctx.subCompiler.scoreNodes(ctx.states, nodes) match {
								case Right(nScore) =>
									if (nScore < nWinnerScore) {
										winner = cmds1
										nWinnerScore = nScore
									}
								case Left(err) => lsErrors ++= err.errors
							}
						case _ =>
					}
				}
		}
		if (nWinnerScore < Int.MaxValue)
			Right(winner)
		else
			Left(CompileError(cmd, lsErrors))
	}
	
	protected def translateCommand(tips: SortedSet[TipConfigL2], mapTipToType: Map[TipConfigL2, String]): Either[Errors, Seq[CycleState]]

	/** Create temporary tip state objects and associate them with the source liquid */
	protected def cleanTipStates(builder: StateBuilder, mapTipToType: Map[TipConfigL2, String]) {
		for ((tip, sType) <- mapTipToType) {
			val tipWriter = tip.obj.stateWriter(builder)
			// Get proper tip
			if (robot.areTipsDisposable)
				tipWriter.get(sType)
			tipWriter.clean(WashIntensity.Decontaminate)
		}
	}
	
	protected def dispense_updateTipStates(cycle: CycleState, twvps: Seq[L2A_SpirateItem], builder: StateBuilder) {
		// Add volumes to amount required in tips
		for (twvp <- twvps) {
			val wellState = twvp.well.obj.state(cycle.state0)
			val tipWriter = twvp.tip.obj.stateWriter(builder)
			tipWriter.dispense(twvp.nVolume, wellState.liquid, twvp.policy.pos)
		}
	}
	
	protected def mix_updateTipStates(cycle: CycleState, twvps: Seq[L2A_MixItem], tipStates: HashMap[Tip, TipStateL2]) {
		// Add volumes to amount required in tips
		for (twvp <- twvps) {
			val wellState = twvp.well.obj.state(cycle.state0)
			val tipWriter = twvp.tip.obj.stateWriter(tipStates)
			tipWriter.mix(wellState.liquid, twvp.nVolume)
		}
	}
	
	/** Would a cleaning be required before a subsequent dispense from the same tip? */
	// REFACTOR: Remove this method -- ellis, 2011-08-30
	protected def checkNoCleanRequired(cycle: CycleState, tipStates: collection.Map[Tip, TipStateL2], tws: Seq[TipWell]): Boolean = {
		def step(tipWell: TipWell): Boolean = {
			val tipState = tipStates(tipWell.tip.obj)
			PipetteHelper.getCleanDegreeDispense(tipState) == WashIntensity.None
		}
		tws.forall(step)
	}
	
	/*protected def clean(cycle: CycleState, tcs: Seq[Tuple2[TipConfigL2, WashIntensity.Value]]) {
		// Add tokens
		val tcss = robot.batchesForClean(tcs)
		for (tcs <- tcss) {
			val tips = tcs.map(_._1).toSet
			cycle.cleans += L3C_Clean(tips, tcs.head._2)
		}
	}*/
	
	protected def getAspirateCleanSpec(
		builder: StateBuilder,
		mapTipToType: Map[TipConfigL2, String],
		overrides: TipHandlingOverrides,
		bFirst: Boolean,
		item: L2A_SpirateItem
	): Option[CleanSpec2] = {
		if (robot.areTipsDisposable) {
			val tipsToReplace = collection.mutable.Set[TipConfigL2]()
			// Replacement requires by aspirate
			val tipState = item.tip.obj.state(builder)
			val srcState = item.well.obj.state(builder)
			val srcLiquid = srcState.liquid
			val bReplace = overrides.replacement_? match {
				case None => PipetteHelper.choosePreAsperateReplacement(srcLiquid, tipState)
				case Some(TipReplacementPolicy.ReplaceAlways) => true
				case Some(TipReplacementPolicy.KeepBetween) => bFirst
				case Some(TipReplacementPolicy.KeepAlways) => false
			}
			if (bReplace)
				Some(ReplaceSpec2(item.tip))
			else
				None
		}
		else {
			val mapTipToWash = new HashMap[TipConfigL2, WashSpec]
			val washIntensityDefault = if (bFirst) WashIntensity.Thorough else WashIntensity.Light
			val tipState = item.tip.obj.state(builder)
			val srcState = item.well.obj.state(builder)
			val srcLiquid = srcState.liquid
			PipetteHelper.choosePreAsperateWashSpec(overrides, washIntensityDefault, srcLiquid, tipState) match {
				case Some(wash) => Some(WashSpec2(item.tip, wash))
				case None => None
			}
		}
	}
	
	protected def getDispenseCleanSpec(
		states: StateMap,
		mapTipToType: Map[TipConfigL2, String],
		overrides: TipHandlingOverrides,
		tip: TipConfigL2,
		dest: WellConfigL2,
		pos: PipettePosition.Value
	): Option[CleanSpec2] = {
		if (robot.areTipsDisposable) {
			val tipState = tip.obj.state(states)
			val bReplace = overrides.replacement_? match {
				case Some(TipReplacementPolicy.ReplaceAlways) => true
				case Some(TipReplacementPolicy.KeepBetween) => false
				case Some(TipReplacementPolicy.KeepAlways) => false
				case None =>
					if (pos == PipettePosition.Free || pos == PipettePosition.DryContact)
						false
					else
						PipetteHelper.choosePreDispenseReplacement(tipState)
			}
			if (bReplace)
				Some(ReplaceSpec2(tip))
			else
				None
		}
		else {
			val washIntensityDefault = WashIntensity.Light
			val tipState = tip.obj.state(states)
			val destState = dest.obj.state(states)
			val destLiquid = destState.liquid
			PipetteHelper.choosePreDispenseWashSpec(overrides, washIntensityDefault, destLiquid, tipState, pos) match {
				case None => None
				case Some(wash) =>
					Some(WashSpec2(tip, wash))
			}
		}
	}
	
	private def dropSeq(tips: SortedSet[TipConfigL2]): Seq[L3C_TipsDrop] = {
		if (robot.areTipsDisposable)
			Seq(L3C_TipsDrop(tips))
		else
			Seq()
	}
	
	protected def getUpdatedState(cycle: CycleState): Either[Seq[String], RobotState] = {
		val cmds1 = cycle.toTokenSeq
		//println("cmds1: "+cmds1)
		ctx.subCompiler.compile(cycle.state0, cmds1) match {
			case Left(e) =>
				Left(e.errors)
			case Right(nodes) =>
				cycle.ress = nodes.flatMap(_.collectFinal())
				//println("cycle.ress: "+cycle.ress)
				Right(cycle.ress.last.state1)
		}
	}
}
