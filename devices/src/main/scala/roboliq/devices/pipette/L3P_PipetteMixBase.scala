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

	val robot: PipetteDevice
	val ctx: CompilerContextL3
	val cmd: CmdType

	trait CycleState {
		val tips: SortedSet[TipConfigL2]
		val state0: RobotState
		
		val cleans: ArrayBuffer[L3C_Clean]
		val mixes: ArrayBuffer[L2C_Mix]
		
		var ress: Seq[CompileFinal] = Nil

		def toTokenSeq: Seq[Command]
	}
	
	val helper = new PipetteHelper

	val dests: SortedSet[WellConfigL2]

	def translation: Either[CompileError, Seq[Command]] = {
		// Need to split into tip groups (e.g. large tips, small tips, all tips)
		// For each group, perform the pipetting and score the results
		// Pick the strategy with the best score
		var winner = Seq[Command]()
		var nWinnerScore = Int.MaxValue
		var lsErrors = new ArrayBuffer[String]
		for (tipGroup <- robot.config.tipGroups) {
			val tips = robot.config.tips.filter(tip => tipGroup.contains(tip.index)).map(tip => tip.state(ctx.states).conf)
	
			translateCommand(tips) match {
				case Left(lsErrors2) =>
					lsErrors ++= lsErrors2
				case Right(Seq()) =>
				case Right(cycles) =>
					val cmds1 = cycles.flatMap(_.toTokenSeq)
					ctx.compiler.compile(ctx.states, cmds1) match {
						case Right(nodes) =>
							ctx.compiler.scoreNodes(ctx.states, nodes) match {
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
	
	protected def translateCommand(tips: SortedSet[TipConfigL2]): Either[Errors, Seq[CycleState]]
	
	protected def dispense_updateTipStates(cycle: CycleState, twvps: Seq[L2A_SpirateItem], tipStates: HashMap[Tip, TipStateL2]) {
		// Add volumes to amount required in tips
		for (twvp <- twvps) {
			val wellState = twvp.well.obj.state(cycle.state0)
			val tipWriter = twvp.tip.obj.stateWriter(tipStates)
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
	protected def checkNoCleanRequired(cycle: CycleState, tipStates: collection.Map[Tip, TipStateL2], tws: Seq[TipWell]): Boolean = {
		def step(tipWell: TipWell): Boolean = {
			val tipState = tipStates(tipWell.tip.obj)
			helper.getCleanDegreeDispense(tipState) == WashIntensity.None
		}
		tws.forall(step)
	}
	
	protected def clean(cycle: CycleState, tcs: Seq[Tuple2[TipConfigL2, WashIntensity.Value]]) {
		// Add tokens
		val tcss = robot.batchesForClean(tcs)
		for (tcs <- tcss) {
			val tips = tcs.map(_._1).toSet
			cycle.cleans += L3C_Clean(tips, tcs.head._2)
		}
	}
	
	protected def getUpdatedState(cycle: CycleState): Either[Seq[String], RobotState] = {
		val cmds1 = cycle.toTokenSeq
		//println("cmds1: "+cmds1)
		ctx.compiler.compile(cycle.state0, cmds1) match {
			case Left(e) =>
				Left(e.errors)
			case Right(nodes) =>
				cycle.ress = nodes.flatMap(_.collectFinal())
				//println("cycle.ress: "+cycle.ress)
				Right(cycle.ress.last.state1)
		}
	}
}
