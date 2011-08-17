package roboliq.devices.pipette

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
//import roboliq.parts._
//import roboliq.tokens._
//import roboliq.robot._
//import roboliq.parts._
//import roboliq.level2.tokens._
import roboliq.level3._


class Compiler_PipetteCommandL2(robot: PipetteDevice) extends CommandCompilerL2 {
	
}

private class Compiler_PipetteCommandL2_Sub(compiler: Compiler, robot: PipetteDevice, state0: RobotState, cmd: L2_PipetteCommand) {
	val args = cmd.args
	
	case class SrcTipDestVolume(src: WellConfigL1, tip: Tip, dest: WellConfigL1, nVolume: Double)
	
	class CycleState(val tips: SortedSet[Tip], val state0: RobotState) {
		val cleans = new ArrayBuffer[L1_Clean]
		val aspirates = new ArrayBuffer[L1_Aspirate]
		val dispenses = new ArrayBuffer[L1_Dispense]
		
		var ress: Seq[CompileFinal] = Nil

		def toTokenSeq: Seq[Command] = cleans ++ aspirates ++ dispenses
	}
	
	val helper = new PipetteHelper

	val dests = SortedSet[WellConfigL1]() ++ args.items.map(_.dest)
	val mapDestToItem = args.items.map(t => t.dest -> t).toMap

	val translation: Seq[Command] = {
		// Need to split into tip groups (e.g. large tips, small tips, all tips)
		// For each group, perform the pipetting and score the results
		// Pick the strategy with the best score
		var winner = Seq[Command]()
		var nWinnerScore = Int.MaxValue
		for (tipGroup <- robot.config.tipGroups) {
			val tips = robot.config.tips.filter(tip => tipGroup.contains(tip.index))
	
			val cycles = pipette(tips)
			if (!cycles.isEmpty) {
				val cmds1 = cycles.flatMap(_.toTokenSeq)
				val x = compiler.compileL1(state0, cmds1)
				if (!x.isEmpty) {
					compiler.score(state0, x) match {
						case Some(nScore) =>
							if (nScore < nWinnerScore) {
								winner = cmds1
								nWinnerScore = nScore
							}
						case _ =>
					}
				}
			}
		}
		winner
	}
	
	private def pipette(tips: SortedSet[Tip]): Seq[CycleState] = {
		// For each dispense, pick the top-most destination wells available in the next column
		// Break off dispense batch if any tips cannot fully dispense volume
		val cycles = new ArrayBuffer[CycleState]
		//val state = new RobotStateBuilder(state0)
		
		// Pair up all tips and wells
		val twss0 = helper.chooseTipWellPairsAll(tips, dests)

		def createCycles(twss: List[Seq[TipWell]], stateCycle0: RobotState): Boolean = {
			if (twss.isEmpty)
				return true
			
			val cycle = new CycleState(tips, stateCycle0)
			// First tip/dest pairs for dispense
			val tws0 = twss.head
			
			val srcs0 = tws0.map(tw => tw.tip -> mapDestToItem(tw.well).srcs).toMap
			
			// Associate source liquid with tip
			val tipStates = new HashMap[Tip, TipState]
			tws0.foreach(tw => {
				val item = mapDestToItem(tw.well)
				val srcState = stateCycle0.getWellState(item.srcs.head)
				tipStates(tw.tip) = TipState(tw.tip).aspirate(srcState.liquid, 0)
			})

			//
			// First dispense
			//
			val sError_? = dispense(cycle, tipStates, srcs0, tws0)
			if (sError_?.isDefined) {
				println("ERROR: "+sError_?.get)
				return false
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
			val tcs: Seq[Tuple2[Tip, CleanDegree.Value]] = srcs0.map(pair => {
				val (tip, srcs) = pair
				val tipState = stateCycle0.getTipState(tip)
				val srcState = stateCycle0.getWellState(srcs.head)
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
				println("ERROR: "+sError_?.get)
				return false
			}

			val stateNext = getUpdatedState(cycle)
			
			cycles += cycle
			
			createCycles(twssRest, stateNext)
		}

		val bOk = createCycles(twss0.toList, state0)
		if (bOk) {
			// TODO: optimize aspiration
			cycles
		}
		else
			Nil
	}
	
	private def dispense(cycle: CycleState, tipStates: HashMap[Tip, TipState], srcs: Map[Tip, Set[WellConfigL1]], tws: Seq[TipWell]): Option[String] = {
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
	private def dispense_checkVols(tipStates: HashMap[Tip, TipState], srcs: Map[Tip, Set[WellConfigL1]], tws: Seq[TipWell]): Option[String] = {
		assert(!tws.isEmpty)
		var sError_? : Option[String] = None
		
		def isVolOk(tw: TipWell): Boolean = {
			val tip = tw.tip
			val dest = tw.well
			val item = mapDestToItem(dest)
			val src = srcs(tip).head
			val liquidSrc = state0.getWellState(src).liquid
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

	private def dispense_createTwvps(tws: Seq[TipWell], tipStates: collection.Map[Tip, TipState]): Either[String, Seq[TipWellVolumePolicy]] = {
		// get pipetting policy for each dispense
		val policies_? = tws.map(tw => {
			val item = mapDestToItem(tw.well)
			val tipState = tipStates(tw.tip)
			val wellState = state0.getWellState(tw.well)
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
		cycle.dispenses ++= twvpss.map(twvps => new L1_Dispense(twvps))
	}
	
	private def dispense_updateTipStates(twvps: Seq[TipWellVolumePolicy], tipStates: HashMap[Tip, TipState]) {
		// Add volumes to amount required in tips
		for (twvp <- twvps) {
			val tipState0 = tipStates(twvp.tip)
			val wellState = state0.getWellState(twvp.well)
			tipStates(twvp.tip) = tipState0.dispense(twvp.nVolume, wellState.liquid, twvp.policy.pos)
		}
	}
	
	/** Would a cleaning be required before a subsequent dispense from the same tip? */
	private def checkNoCleanRequired(cycle: CycleState, tipStates: collection.Map[Tip, TipState], tws: Seq[TipWell]): Boolean = {
		def step(tipWell: TipWell): Boolean = {
			val tipState = tipStates(tipWell.tip)
			helper.getCleanDegreeDispense(tipState) == CleanDegree.None
		}
		tws.forall(step)
	}
	
	private def clean(cycle: CycleState, tcs: Seq[Tuple2[Tip, CleanDegree.Value]]) {
		// Add tokens
		cycle.cleans ++= robot.batchesForClean(tcs)
	}

	private def aspirateLiquid(cycle: CycleState, tipStates: collection.Map[Tip, TipState], srcs: Set[WellConfigL1]): Option[String] = {
		// Get list of tips which require aspiration	
		var tips = SortedSet[Tip]() ++ tipStates.keys

		// sort the sources by volume descending (secondary sort key is index order)
		def order(well1: WellConfigL1, well2: WellConfigL1): Boolean = {
			val a = cycle.state0.getWellState(well1)
			val b = cycle.state0.getWellState(well2)
			(a.nVolume > b.nVolume) || (a.nVolume == b.nVolume && well1.index < well2.index)
		}
		// keep the top tips.size() entries ordered by index
		val srcs2 = SortedSet[WellConfigL1](srcs.toSeq.sortWith(order).take(tips.size) : _*)
		val pairs = srcs2.toSeq zip tips.toSeq
	
		val twss0 = helper.chooseTipSrcPairs(tips, srcs2)
		var sError_? : Option[String] = None
		twss0.forall(tws => {
			aspirate_createTwvps(tipStates, tws) match {
				case Left(sError) => sError_? = Some(sError); false
				case Right(twvps) =>
					val twvpss = robot.batchesForAspirate(twvps)
					cycle.aspirates ++= twvpss.map(twvs => new L1_Aspirate(twvps))
					true
			}
		})
		
		sError_?
	}

	private def aspirateDirect(cycle: CycleState, tipStates: collection.Map[Tip, TipState], srcs: collection.Map[Tip, Set[WellConfigL1]]): Option[String] = {
		val tips = tipStates.keys.toSeq.sortBy(tip => tip)
		val tws = tips.map(tip => new TipWell(tip, srcs(tip).head))
		aspirate_createTwvps(tipStates, tws) match {
			case Left(sError) => Some(sError)
			case Right(twvps) =>
				val twvpss = robot.batchesForAspirate(twvps)
				cycle.aspirates ++= twvpss.map(twvs => new L1_Aspirate(twvps))
				None
		}
	}
	
	private def aspirate_createTwvps(tipStates: collection.Map[Tip, TipState], tws: Seq[TipWell]): Either[String, Seq[TipWellVolumePolicy]] = {
		// get pipetting policy for each dispense
		val policies_? = tws.map(tw => {
			val tipState = tipStates(tw.tip)
			val srcState = state0.getWellState(tw.well)
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
	
	private def getUpdatedState(cycle: CycleState): RobotState = {
		val cmds1 = cycle.toTokenSeq
		cycle.ress = compiler.compileL1(state0, cmds1)
		cycle.ress.last.state1
	}
}
