/*package roboliq.devices.pipette

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import roboliq.parts._
import roboliq.tokens._
import roboliq.robot._
import roboliq.level2.tokens._
import roboliq.devices.PipetteDeviceUtil


class T2_PipetteMix_Compiler(robot: Robot, state0: RobotState, token: T2_PipetteMix) {
	class CycleState(val tips: SortedSet[Tip]) {
		// Tips available for dispense.  Once a tip has dispensed with wet contact into a contaminating liquid, it should be removed from this list.
		var tipsAvailable = tips
		// The volume which needs to be aspirated into the given tip
		val mapTipToVolume = new mutable.HashMap[Tip, Double] ++ tips.map(_ -> 0.0)

		val cleans = new ArrayBuffer[T1_Clean]
		val mixes = new ArrayBuffer[T1_Mix]

		//def score(): Int = aspirates.size + dispenses.size
		def toTokenSeq: Seq[T1_Token] = cleans ++ mixes
	}
	
	//val srcs = token.wells
	//val liquid = state0.getLiquid(srcs.head)
	// Make sure that all the source wells contain the same liquid
	//assert(srcs.forall(well => state0.getLiquid(well) == liquid))
	
	val helper = new PipetteHelper

	// Contamination scenarios:
	// dispense contaminates: wash after each dispense
	// aspirate contaminates, dispense does not: wash before each subsequent aspirate
	// aspirate does not contaminate, dispense doesn't contaminate: no washing required
	
	//private def doesWellContaminate(well: Well): Boolean = state.getLiquid(well).contaminates
	//val bDestContaminates = dests.exists(doesWellContaminate)
	
	//val bAspirateContaminates = liquid.bContaminates
	//val bDispenseContaminates = dispenseStrategy.bEnter && dests.exists(doesWellContaminate)
	
	val dests = SortedSet[Well]() ++ token.wells

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
			
			// Src/tip/dest/volume combinations
			val twvps = tws0.map(tw => new TipWellVolumePolicy(tw.tip, tw.well, token.nVolume, PipettePolicy(PipettePosition.WetContact)))
			
			// If we can't accommodate the required volume, abort:
			if (!checkVols(cycle, state, twvps))
				return false

			// Tuples of tip to clean degree required by source liquid
			val tcs = twvps.map(twv => {
				val tipState = state.getTipState(twv.tip)
				val wellState = state.getWellState(twv.well)
				val liquid = wellState.liquid
				val cleanDegree = helper.getCleanDegreeAspirate(tipState, liquid)
				(twv.tip, cleanDegree)
			})

			clean(cycle, state, tcs)
			mix(cycle, state, twvps)
			
			cycles += cycle
			
			createCycles(twss.tail)
		}

		val bOk = createCycles(twss0.toList)
		if (bOk)
			cycles
		else
			Nil
	}

	// Check for appropriate volumes
	private def checkVols(cycle: CycleState, state: RobotStateBuilder, tws: Seq[TipWell]): Boolean = {
		assert(!tws.isEmpty)
		
		def isVolOk(tipWell: TipWell): Boolean = {
			val (tip, well) = (tipWell.tip, tipWell.well)
			val liquid = state.getWellState(well).liquid
			val nMin = robot.getTipAspirateVolumeMin(tip, liquid)
			val nMax = robot.getTipHoldVolumeMax(tip, liquid)
			val nTipVolume = cycle.mapTipToVolume(tip) // Volume already in the tip
			val nVolume = token.nVolume // Volume to mix
			val bOk = (
				nVolume >= nMin &&
				nVolume + nTipVolume <= nMax
			)
			bOk
		}

		tws.forall(isVolOk)
	}
	
	private def checkNoCleanRequired(cycle: CycleState, state: RobotStateBuilder, tws: Seq[TipWell]): Boolean = {
		def step(tipWell: TipWell): Boolean = {
			val tipState = state.getTipState(tipWell.tip)
			helper.getCleanDegreeDispense(tipState) == CleanDegree.None
		}
		tws.forall(step)
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

	private def mix(cycle: CycleState, state: RobotStateBuilder, twvps: Seq[TipWellVolumePolicy]) {
		val twvpss = robot.batchesForAspirate(state, twvps)
		// Create dispense tokens
		cycle.mixes ++= twvpss.map(twvps => new T1_Mix(twvps, PipettePolicy(PipettePosition.WetContact), token.nVolume, token.nCount))
		// Update tip state
		twvps.foreach(state.mix)
	}
}
*/