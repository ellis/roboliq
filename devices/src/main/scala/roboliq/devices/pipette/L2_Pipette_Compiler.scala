package roboliq.devices.pipette

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

//import roboliq.parts._
import roboliq.tokens._
import roboliq.robot._
import roboliq.parts._
//import roboliq.level2.tokens._
import roboliq.devices.PipetteDeviceUtil


class T2_Pipette_Compiler(robot: Robot, state0: RobotState, cmd: L2_PipetteCommand) {
	val args = cmd.args
	
	case class SrcTipDestVolume(src: Well, tip: Tip, dest: Well, nVolume: Double)
	
	class CycleState(val tips: SortedSet[Tip]) {
		val cleans = new ArrayBuffer[T1_Clean]
		val aspirates = new ArrayBuffer[T1_Aspirate]
		val dispenses = new ArrayBuffer[T1_Dispense]

		def toTokenSeq: Seq[T1_Token] = cleans ++ aspirates ++ dispenses
	}
	
	val helper = new PipetteHelper

	val dests = SortedSet[Well]() ++ args.items.map(_.dest)
	val mapDestToItem = args.items.map(t => t.dest -> t).toMap

	val tokens: Seq[T1_TokenState] = {
		// Need to split into tip groups (e.g. large tips, small tips, all tips)
		// For each group, perform the pipetting and score the results
		// Pick the strategy with the best score
		var winner = Seq[T1_Token]()
		var nWinnerScore = Int.MaxValue
		for (tipGroup <- robot.config.tipGroups) {
			val tips = robot.config.tips.filter(tip => tipGroup.contains(tip.index))
	
			val cycles = pipette(tips)
			if (!cycles.isEmpty) {
				val tokens = cycles.flatMap(_.toTokenSeq)
				val nScore = robot.score(tokens)
				if (nScore < nWinnerScore) {
					winner = tokens
					nWinnerScore = nScore
				}
			}
		}
		PipetteDeviceUtil.getTokenStates(state0, winner)
	}
	
	private def pipette(tips: SortedSet[Tip]): Seq[CycleState] = {
		// For each dispense, pick the top-most destination wells available in the next column
		// Break off dispense batch if any tips cannot fully dispense volume
		val cycles = new ArrayBuffer[CycleState]
		val state = new RobotStateBuilder(state0)
		
		// Pair up all tips and wells
		val twss0 = helper.chooseTipWellPairsAll(tips, dests)

		def createCycles(twss: List[Seq[TipWell]]): Boolean = {
			if (twss.isEmpty)
				return true
			
			val cycle = new CycleState(tips)
			// First tip/dest pairs for dispense
			val tws0 = twss.head
			
			// Start with assumption that tips are clean
			val tipStates = new HashMap[Tip, TipState] ++ tips.toSeq.map(tip => tip -> TipState(tip))
			// Associate source liquid with tip
			tws0.foreach(tws => {
				val item = mapDestToItem(tws.well)
				val wellState = state.getWellState(item.srcs.head)
				tipStates(tws.tip) = tipStates(tws.tip).aspirate(wellState.liquid, 0)
			})
				
			// Src/tip/dest/volume combinations
			val stdvs = tws0.map(tw => {
				val item = mapDestToItem(tw.well)
				new SrcTipDestVolume(src, tw.tip, item.dest, item.nVolume)
			})
			
			// If we can't accommodate the required volume, abort:
			if (!checkVols(cycle, state, stdvs))
				return false

			// Tuples of tip to clean degree required by source liquid
			val tcs = stdvs.map(stdv => {
				val tipState = state.getTipState(stdv.tip)
				val wellState = state.getWellState(stdv.src)
				val liquid = wellState.liquid
				val cleanDegree = helper.getCleanDegreeAspirate(tipState, liquid)
				(stdv.tip, cleanDegree)
			})

			clean(cycle, state, tcs)
			aspirate(cycle, state, stdvs)
			dispense(cycle, state, stdvs)
			
			cycles += cycle
			
			createCycles(twss.tail)
		}

		val bOk = createCycles(twss0.toList)
		if (bOk) {
			// TODO: optimize aspiration
			cycles
		}
		else
			Nil
	}

	// Check for appropriate volumes
	private def checkVols(cycle: CycleState, state: RobotStateBuilder, stdvs: Seq[SrcTipDestVolume]): Boolean = {
		assert(!stdvs.isEmpty)
		
		def isVolOk(stdv: SrcTipDestVolume): Boolean = {
			val tip = stdv.tip
			val liquid = state.getWellState(stdv.src).liquid
			val nMin = robot.getTipAspirateVolumeMin(tip, liquid)
			val nMax = robot.getTipHoldVolumeMax(tip, liquid)
			val nTipVolume = 0 // Volume already in the tip
			val bOk = (
				stdv.nVolume >= nMin &&
				stdv.nVolume + nTipVolume <= nMax
			)
			bOk
		}

		stdvs.forall(isVolOk)
	}
	
	private def clean(cycle: CycleState, state: RobotStateBuilder, tcs: Seq[Tuple2[Tip, CleanDegree.Value]]) {
		// Add tokens
		cycle.cleans ++= robot.batchesForClean(tcs)
		// Update state
		for (c <- cycle.cleans) {
			for (tip <- c.tips) {
				val tipState = state.getTipState(tip)
				state.cleanTip(tip, c.degree)
			}
		}
	}

	private def aspirate(cycle: CycleState, state: RobotStateBuilder, stdvs0: Seq[SrcTipDestVolume]) {
		// Cases:
		// Case 1: tips size == srcs size:
		// Case 2: tips size < srcs size:
		// Case 3: tips size > srcs size:
		// -----
		// The sources should be chosen according to this algorithm:
		// - sort the sources by volume descending (secondary sort key is index order)
		// - keep the top tips.size() entries
		// Repeat the sorting each time all sources have been used (e.g. when there are more tips than sources)

		val stdvs = stdvs0.sortBy(_.tip)
		val twvps = stdvs.map(stdv => new TipWellVolumePolicy(stdv.tip, stdv.src, stdv.nVolume, PipettePolicy(PipettePosition.WetContact)))
		val twvpss = robot.batchesForAspirate(state, twvps)
		cycle.aspirates ++= twvpss.map(twvs => new T1_Aspirate(twvps))
		twvps.foreach(state.aspirate)
	}

	private def dispense(cycle: CycleState, state: RobotStateBuilder, stdvs0: Seq[SrcTipDestVolume]) {
		val stdvs = stdvs0.sortBy(_.tip)
		// Map tip/dest pairs to TipWellVolumePolicy objects
		val twvds = stdvs.map(stdv => {
			val wellStateSrc = state.getWellState(stdv.src)
			val wellStateDest = state.getWellState(stdv.dest)
			val policy = robot.getPipettePolicy(stdv.tip, wellStateSrc.liquid, stdv.nVolume, wellStateDest)
			new TipWellVolumePolicy(stdv.tip, stdv.dest, stdv.nVolume, policy)
		})
		val twvdss = robot.batchesForDispense(state, twvds)
		// Create dispense tokens
		cycle.dispenses ++= twvdss.map(twvds => new T1_Dispense(twvds))
		// Add volume to required aspirate volume for this cycle
		for (twvd <- twvds) {
			//cycle.mapTipToVolume(twvd.tip) += twvd.nVolume
			state.dispense(twvd)
		}
	}
	
	private def combine(cycle1: CycleState, cycle2: CycleState): Option[CycleState] = {
		if (cycle1.aspirates.size < cycle2.aspirates.size)
			return None
		
		val aspirates = new ArrayBuffer[T1_Aspirate]
		
		def getLiquid(well: Well): Liquid = state0.getWellState(well).liquid 
		val twvs1 = cycle1.aspirates.flatMap(_.twvs)
		val twvs2 = cycle2.aspirates.flatMap(_.twvs)
		if (twvs1.size < twvs2.size)
			return None
		
		val twvsPairs = twvs1 zip twvs2
		
		// List of liquids aspirated in cycle1 and cycle2
		val liqs = twvsPairs.map(pair => (getLiquid(pair._1.well), getLiquid(pair._2.well)))
		// Are the liquids the same?
		if (liqs.forall(pair => pair._1 eq pair._2)) {
			val vols = twvsPairs.map(pair => pair._1.nVolume + pair._2.nVolume)
			aspirates ++= twvs1
			None
		}
		else {
			None
		}
	}

	/** Would a cleaning be required before a subsequent dispense from the same tip? */
	private def checkNoCleanRequired(cycle: CycleState, state: RobotStateBuilder, tws: Seq[TipWell]): Boolean = {
		def step(tipWell: TipWell): Boolean = {
			val tipState = state.getTipState(tipWell.tip)
			helper.getCleanDegreeDispense(tipState) == CleanDegree.None
		}
		tws.forall(step)
	}
}
