package roboliq.devices.pipette

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._


class L2P_Mix(robot: PipetteDevice) extends CommandCompilerL2 {
	type CmdType = L2C_Mix
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL2, cmd: CmdType): CompileResult = {
		val x = new L2P_Mix_Sub(robot, ctx, cmd)
		x.translation match {
			case Right(translation) =>
				CompileTranslation(cmd, translation)
			case Left(e) =>
				println("e: "+e)
				e
		}
	}
}

private class L2P_Mix_Sub(robot: PipetteDevice, ctx: CompilerContextL2, cmd: L2C_Mix) {
	val compiler = ctx.compiler
	val args = cmd.args
	
	case class SrcTipDestVolume(src: WellConfigL1, tip: TipConfigL1, dest: WellConfigL1, nVolume: Double)
	
	class CycleState(val tips: SortedSet[TipConfigL1], val state0: RobotState) {
		val cleans = new ArrayBuffer[L2C_Clean]
		val mixes = new ArrayBuffer[L1C_Mix]
		
		var ress: Seq[CompileFinal] = Nil

		def toTokenSeq: Seq[Command] = cleans ++ mixes
	}
	
	val helper = new PipetteHelper

	val dests = SortedSet[WellConfigL1](args.wells.toSeq : _*)

	val translation: Either[CompileError, Seq[Command]] = {
		// Need to split into tip groups (e.g. large tips, small tips, all tips)
		// For each group, perform the pipetting and score the results
		// Pick the strategy with the best score
		var winner = Seq[Command]()
		var nWinnerScore = Int.MaxValue
		var lsErrors = new ArrayBuffer[String]
		for (tipGroup <- robot.config.tipGroups) {
			val tips = robot.config.tips.filter(tip => tipGroup.contains(tip.index)).map(tip => tip.getConfigL1(map31).get)
	
			pipette(tips) match {
				case Left(lsErrors2) =>
					lsErrors ++= lsErrors2
				case Right(Seq()) =>
				case Right(cycles) =>
					val cmds1 = cycles.flatMap(_.toTokenSeq)
					compiler.compileL1(state0, cmds1) match {
						case Right(ress) =>
							compiler.score(state0, ress) match {
								case Some(nScore) =>
									if (nScore < nWinnerScore) {
										winner = cmds1
										nWinnerScore = nScore
									}
								case _ =>
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
	
	type Errors = Seq[String]
	
	private def pipette(tips: SortedSet[TipConfigL1]): Either[Errors, Seq[CycleState]] = {
		// For each dispense, pick the top-most destination wells available in the next column
		// Break off dispense batch if any tips cannot fully dispense volume
		val cycles = new ArrayBuffer[CycleState]
		//val state = new RobotStateBuilder(state0)
		
		// Pair up all tips and wells
		val twss0 = helper.chooseTipWellPairsAll(map31, tips, dests)

		def createCycles(twss: List[Seq[TipWell]], stateCycle0: RobotState): Either[Errors, Unit] = {
			if (twss.isEmpty)
				return Right()
			
			val cycle = new CycleState(tips, stateCycle0)
			// First tip/dest pairs for dispense
			val tws0 = twss.head
			
			// Associate source liquid with tip
			val tipStates = new HashMap[TipConfigL1, TipStateL1]
			tws0.foreach(tw => {
				val srcState = tw.well.state(stateCycle0)
				val tipState = tw.tip.createState0()
				tipStates(tw.tip) = tipState
			})

			//
			// First dispense
			//
			val sError_? = mix(cycle, tipStates, tws0)
			if (sError_?.isDefined) {
				return Left(Seq(sError_?.get))
			}
			
			// Add as many tip/dest groups to this cycle as possible, and return list of remaining groups
			val twssRest = twss.tail.dropWhile(tws => {
				// dispense does not require cleaning
				checkNoCleanRequired(cycle, tipStates, tws) &&
				mix(cycle, tipStates, tws).isEmpty
			})
			
			// Tuples of tip to clean degree required by dest liquid
			val tcs: Seq[Tuple2[TipConfigL1, CleanDegree.Value]] = tws0.map(tw => {
				val tipState = tw.tip.state(stateCycle0)
				val srcState = tw.well.state(stateCycle0)
				val srcLiquid = srcState.liquid
				val cleanDegree = helper.getCleanDegreeAspirate(tipState, srcLiquid)
				(tw.tip, cleanDegree)
			}).toSeq
			clean(cycle, tcs)

			getUpdatedState(cycle) match {
				case Right(stateNext) => 
					cycles += cycle
					createCycles(twssRest, stateNext)
				case Left(lsErrors) =>
					Left(lsErrors)
			}			
		}

		createCycles(twss0.toList, state0) match {
			case Left(e) =>
				Left(e)
			case Right(()) =>
				// TODO: optimize aspiration
				Right(cycles)
		}
	}
	
	private def mix(cycle: CycleState, tipStates: HashMap[TipConfigL1, TipStateL1], tws: Seq[TipWell]): Option[String] = {
		dispense_checkVols(tipStates, tws) match {
			case None =>
			case e @ Some(sError) => return e
		}
		
		val twvpDispenses_? = dispense_createTwvpcs(tws, tipStates)
		if (twvpDispenses_?.isLeft)
			twvpDispenses_?.left
		dispense_createTwvpcs(tws, tipStates) match {
			case Left(sError) => Some(sError)
			case Right(twvpcs) =>
				// Create L1 dispense commands
				dispense_addCommands(cycle, twvpcs)
				dispense_updateTipStates(twvpcs, tipStates)
				None
		}
	}
	
	// Check for appropriate volumes
	private def dispense_checkVols(tipStates: HashMap[TipConfigL1, TipStateL1], tws: Seq[TipWell]): Option[String] = {
		assert(!tws.isEmpty)
		var sError_? : Option[String] = None
		
		def isVolOk(tw: TipWell): Boolean = {
			val tip = tw.tip
			val dest = tw.well
			val liquid = dest.state(state0).liquid
			val nMin = robot.getTipAspirateVolumeMin(tip, liquid)
			val nMax = robot.getTipHoldVolumeMax(tip, liquid)
			val nTipVolume = -tipStates(tip).nVolume
			sError_? = {
				val nVolume = args.mixSpec.nVolume
				if (nVolume < nMin)
					Some("Cannot aspirate "+nVolume+"ul into tip "+(tip.index+1)+": require >= "+nMin+"ul")
				else if (nVolume + nTipVolume > nMax)
					Some("Cannot aspirate "+nVolume+"ul into tip "+(tip.index+1)+": require <= "+nMax+"ul")
				else
					None
			}
			// TODO: make sure that source well is not over-aspirated
			// TODO: make sure that destination well is not over-filled
			sError_?.isEmpty
		}

		tws.forall(isVolOk)
		sError_?
	}

	private def dispense_createTwvpcs(tws: Seq[TipWell], tipStates: collection.Map[TipConfigL1, TipStateL1]): Either[String, Seq[L1A_MixItem]] = {
		// get pipetting policy for each dispense
		val policies_? = tws.map(tw => {
			val tipState = tipStates(tw.tip)
			val wellState = tw.well.state(state0)
			robot.getDispensePolicy(tipState, wellState, args.mixSpec.nVolume)
		})
		if (policies_?.forall(_.isDefined)) {
			val twvps = (tws zip policies_?.map(_.get)).map(pair => {
				val (tw, policy) = pair
				new L1A_MixItem(tw.tip, tw.well, args.mixSpec.nVolume, policy, args.mixSpec.nCount)
			})
			Right(twvps)
		}
		else {
			Left("No appropriate dispense policy available")
		}
	}

	private def dispense_addCommands(cycle: CycleState, twvpcs0: Seq[L1A_MixItem]) {
		val twvpcs = twvpcs0.sortBy(_.tip)
		val twvpcss = robot.batchesForMix(twvpcs)
		// Create dispense tokens
		cycle.mixes ++= twvpcss.map(twvpcs => L1C_Mix(twvpcs))
	}
	
	private def dispense_updateTipStates(twvps: Seq[L1A_MixItem], tipStates: HashMap[TipConfigL1, TipStateL1]) {
		// Add volumes to amount required in tips
		for (twvp <- twvps) {
			val wellState = twvp.well.state(state0)
			val tipWriter = twvp.tip.stateWriter(tipStates)
			tipWriter.dispense(twvp.nVolume, wellState.liquid, twvp.policy.pos)
		}
	}
	
	/** Would a cleaning be required before a subsequent dispense from the same tip? */
	private def checkNoCleanRequired(cycle: CycleState, tipStates: collection.Map[TipConfigL1, TipStateL1], tws: Seq[TipWell]): Boolean = {
		def step(tipWell: TipWell): Boolean = {
			val tipState = tipStates(tipWell.tip)
			helper.getCleanDegreeDispense(tipState) == CleanDegree.None
		}
		tws.forall(step)
	}
	
	private def clean(cycle: CycleState, tcs: Seq[Tuple2[TipConfigL1, CleanDegree.Value]]) {
		// Add tokens
		val tcss = robot.batchesForClean(tcs)
		for (tcs <- tcss) {
			val tips = tcs.map(_._1).toSet
			cycle.cleans += L2C_Clean(tips, tcs.head._2)
		}
	}
	
	private def getUpdatedState(cycle: CycleState): Either[Seq[String], RobotState] = {
		val cmds1 = cycle.toTokenSeq
		println("cmds1: "+cmds1)
		compiler.compileL1(cycle.state0, cmds1) match {
			case Right(Seq()) =>
				Left(Seq("compileL1 failed"))
			case Right(ress) =>
				cycle.ress = ress
				println("cycle.ress: "+cycle.ress)
				Right(cycle.ress.last.state1)
			case Left(e) =>
				Left(e.errors)
		}
	}
}
