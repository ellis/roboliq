package roboliq.devices.pipette

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._


class L2P_Pipette(robot: PipetteDevice) extends CommandCompilerL2 {
	type CmdType = L2C_Pipette
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL2, cmd: CmdType): CompileResult = {
		val x = new L2P_Pipette_Sub(robot, ctx, cmd)
		x.translation match {
			case Right(translation) =>
				CompileTranslation(cmd, translation)
			case Left(e) =>
				println("e: "+e)
				e
		}
	}
}

private class L2P_Pipette_Sub(robot: PipetteDevice, ctx: CompilerContextL2, cmd: L2C_Pipette) {
	val states = ctx.states
	val args = cmd.args
	
	case class SrcTipDestVolume(src: WellConfigL1, tip: TipConfigL1, dest: WellConfigL1, nVolume: Double)
	
	class CycleState(val tips: SortedSet[TipConfigL1], val state0: RobotState) {
		val cleans = new ArrayBuffer[L2C_Clean]
		val aspirates = new ArrayBuffer[L1C_Aspirate]
		val dispenses = new ArrayBuffer[L1C_Dispense]
		
		var ress: Seq[CompileFinal] = Nil

		def toTokenSeq: Seq[Command] = cleans ++ aspirates ++ dispenses
	}
	
	val helper = new PipetteHelper

	val dests = SortedSet[WellConfigL1]() ++ args.items.map(_.dest)
	val mapDestToItem = args.items.map(t => t.dest -> t).toMap

	val translation: Either[CompileError, Seq[Command]] = {
		// Need to split into tip groups (e.g. large tips, small tips, all tips)
		// For each group, perform the pipetting and score the results
		// Pick the strategy with the best score
		var winner = Seq[Command]()
		var nWinnerScore = Int.MaxValue
		var lsErrors = new ArrayBuffer[String]
		for (tipGroup <- robot.config.tipGroups) {
			val tips = robot.config.tips.filter(tip => tipGroup.contains(tip.index)).map(tip => tip.getConfigL1(states).get)
	
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
			
			val srcs0 = tws0.map(tw => tw.tip -> mapDestToItem(tw.well).srcs).toMap
			
			// Associate source liquid with tip
			val tipStates = new HashMap[TipConfigL1, TipStateL1]
			tws0.foreach(tw => {
				val item = mapDestToItem(tw.well)
				val srcState = item.srcs.head.state(stateCycle0)
				val tipState = tw.tip.createState0()
				tipStates(tw.tip) = tipState
			})

			//
			// First dispense
			//
			val sError_? = dispense(cycle, tipStates, srcs0, tws0)
			if (sError_?.isDefined) {
				return Left(Seq(sError_?.get))
			}
			
			// Add as many tip/dest groups to this cycle as possible, and return list of remaining groups
			val twssRest = twss.tail.dropWhile(tws => {
				// each tip still has the same set of source wells
				tws.forall(tw => mapDestToItem(tw.well).srcs == srcs0(tw.tip)) &&
				// dispense does not require cleaning
				checkNoCleanRequired(cycle, tipStates, tws) &&
				dispense(cycle, tipStates, srcs0, tws).isEmpty
			})

			// Tuples of tip to clean degree required by source liquid
			val tcs: Seq[Tuple2[TipConfigL1, CleanDegree.Value]] = srcs0.map(pair => {
				val (tip, srcs) = pair
				val tipState = tip.state(stateCycle0)
				val srcState = srcs.head.state(stateCycle0)
				val srcLiquid = srcState.liquid
				val cleanDegree = helper.getCleanDegreeAspirate(tipState, srcLiquid)
				(tip, cleanDegree)
			}).toSeq
			clean(cycle, tcs)

			val src00 = srcs0.head._2
			val bAllSameSrcs = srcs0.values.forall(_ == src00)
			val sError2_? = {
				if (bAllSameSrcs)
					aspirateLiquid(cycle, tipStates, src00)
				else
					aspirateDirect(cycle, tipStates, srcs0)
			}
			if (sError2_?.isDefined) {
				return Left(Seq(sError_?.get))
			}

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
	
	private def dispense(cycle: CycleState, tipStates: HashMap[TipConfigL1, TipStateL1], srcs: Map[TipConfigL1, Set[WellConfigL1]], tws: Seq[TipWell]): Option[String] = {
		dispense_checkVols(tipStates, srcs, tws) match {
			case None =>
			case e @ Some(sError) => return e
		}
		
		val twvpDispenses_? = dispense_createTwvps(tws, tipStates)
		if (twvpDispenses_?.isLeft)
			twvpDispenses_?.left
		dispense_createTwvps(tws, tipStates) match {
			case Left(sError) => Some(sError)
			case Right(twvps) =>
				// Create L1 dispense commands
				dispense_addCommands(cycle, twvps)
				dispense_updateTipStates(twvps, tipStates)
				None
		}
	}
	
	// Check for appropriate volumes
	private def dispense_checkVols(tipStates: HashMap[TipConfigL1, TipStateL1], srcs: Map[TipConfigL1, Set[WellConfigL1]], tws: Seq[TipWell]): Option[String] = {
		assert(!tws.isEmpty)
		var sError_? : Option[String] = None
		
		def isVolOk(tw: TipWell): Boolean = {
			val tip = tw.tip
			val dest = tw.well
			val item = mapDestToItem(dest)
			val src = srcs(tip).head
			val liquidSrc = src.state(state0).liquid
			val nMin = robot.getTipAspirateVolumeMin(tip, liquidSrc)
			val nMax = robot.getTipHoldVolumeMax(tip, liquidSrc)
			val nTipVolume = -tipStates(tip).nVolume
			sError_? = {
				if (item.nVolume < nMin)
					Some("Cannot aspirate "+item.nVolume+"ul into tip "+(tip.index+1)+": require >= "+nMin+"ul")
				else if (item.nVolume + nTipVolume > nMax)
					Some("Cannot aspirate "+item.nVolume+"ul into tip "+(tip.index+1)+": require <= "+nMax+"ul")
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

	private def dispense_createTwvps(tws: Seq[TipWell], tipStates: collection.Map[TipConfigL1, TipStateL1]): Either[String, Seq[TipWellVolumePolicy]] = {
		// get pipetting policy for each dispense
		val policies_? = tws.map(tw => {
			val item = mapDestToItem(tw.well)
			val tipState = tipStates(tw.tip)
			val wellState = tw.well.state(state0)
			robot.getDispensePolicy(tipState, wellState, item.nVolume)
		})
		if (policies_?.forall(_.isDefined)) {
			val twvps = (tws zip policies_?.map(_.get)).map(pair => {
				val (tw, policy) = pair
				val item = mapDestToItem(tw.well)
				new TipWellVolumePolicy(tw.tip, item.dest, item.nVolume, policy)
			})
			Right(twvps)
		}
		else {
			Left("No appropriate dispense policy available")
		}
	}

	private def dispense_addCommands(cycle: CycleState, twvps0: Seq[TipWellVolumePolicy]) {
		val twvps = twvps0.sortBy(_.tip)
		val twvpss = robot.batchesForDispense(twvps)
		// Create dispense tokens
		cycle.dispenses ++= twvpss.map(twvps => new L1C_Dispense(twvps))
	}
	
	private def dispense_updateTipStates(twvps: Seq[TipWellVolumePolicy], tipStates: HashMap[TipConfigL1, TipStateL1]) {
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

	private def aspirateLiquid(cycle: CycleState, tipStates: collection.Map[TipConfigL1, TipStateL1], srcs: Set[WellConfigL1]): Option[String] = {
		// Get list of tips which require aspiration	
		var tips = SortedSet[TipConfigL1]() ++ tipStates.values.map(_.conf)

		// sort the sources by volume descending (secondary sort key is index order)
		def order(well1: WellConfigL1, well2: WellConfigL1): Boolean = {
			val a = well1.state(state0)
			val b = well2.state(state0)
			(a.nVolume > b.nVolume) || (a.nVolume == b.nVolume && well1.index < well2.index)
		}
		// keep the top tips.size() entries ordered by index
		val srcs2 = SortedSet[WellConfigL1](srcs.toSeq.sortWith(order).take(tips.size) : _*)
		val pairs = srcs2.toSeq zip tips.toSeq
	
		val twss0 = helper.chooseTipSrcPairs(map31, tips, srcs2)
		var sError_? : Option[String] = None
		twss0.forall(tws => {
			aspirate_createTwvps(tipStates, tws) match {
				case Left(sError) => sError_? = Some(sError); false
				case Right(twvps) =>
					val twvpss = robot.batchesForAspirate(twvps)
					cycle.aspirates ++= twvpss.map(twvs => new L1C_Aspirate(twvps))
					true
			}
		})
		
		sError_?
	}

	private def aspirateDirect(cycle: CycleState, tipStates: collection.Map[TipConfigL1, TipStateL1], srcs: collection.Map[TipConfigL1, Set[WellConfigL1]]): Option[String] = {
		val tips = tipStates.values.map(_.conf).toSeq.sortBy(tip => tip)
		val tws = tips.map(tip => new TipWell(tip, srcs(tip).head))
		aspirate_createTwvps(tipStates, tws) match {
			case Left(sError) => Some(sError)
			case Right(twvps) =>
				val twvpss = robot.batchesForAspirate(twvps)
				cycle.aspirates ++= twvpss.map(twvs => new L1C_Aspirate(twvps))
				None
		}
	}
	
	private def aspirate_createTwvps(tipStates: collection.Map[TipConfigL1, TipStateL1], tws: Seq[TipWell]): Either[String, Seq[TipWellVolumePolicy]] = {
		// get pipetting policy for each dispense
		val policies_? = tws.map(tw => {
			val tipState = tipStates(tw.tip)
			val srcState = tw.well.state(state0)
			robot.getAspiratePolicy(tipState, srcState)
		})
		if (policies_?.forall(_.isDefined)) {
			val twvps = (tws zip policies_?.map(_.get)).map(pair => {
				val (tw, policy) = pair
				val tipState = tipStates(tw.tip)
				new TipWellVolumePolicy(tw.tip, tw.well, -tipState.nVolume, policy)
			})
			Right(twvps)
		}
		else {
			Left("No appropriate aspirate policy available")
		}
	}
	
	private def getUpdatedState(cycle: CycleState): Either[Seq[String], RobotState] = {
		val cmds1 = cycle.toTokenSeq
		println("cmds1: "+cmds1)
		compiler.compileL1(state0, cmds1) match {
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
