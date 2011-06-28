package roboliq.level2.commands

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import roboliq.parts._
import roboliq.tokens._
import roboliq.robot._
import roboliq.level2.tokens._


class T2_PipetteLiquid_Compiler(token: T2_PipetteLiquid, robot: Robot) {
	class CycleState(val tips: SortedSet[Tip]) {
		// Tips available for dispense.  Once a tip has dispensed with wet contact into a contaminating liquid, it should be removed from this list.
		var tipsAvailable = tips
		// The volume which needs to be aspirated into the given tip
		val mapTipToVolume = new mutable.HashMap[Tip, Double] ++ tips.map(_ -> 0.0)

		val cleans = new ArrayBuffer[T1_Clean]
		val aspirates = new ArrayBuffer[T1_Aspirate]
		val dispenses = new ArrayBuffer[T1_Dispense]

		//def score(): Int = aspirates.size + dispenses.size
		def toTokenSeq: Seq[T1_Token] = cleans ++ aspirates ++ dispenses
	}
	
	val	srcs = token.srcs
	val liquid = robot.state.getLiquid(srcs.head)
	// Make sure that all the source wells contain the same liquid
	assert(srcs.forall(well => robot.state.getLiquid(well) == liquid))
	
	val helper = new PipetteHelper

	// Contamination scenarios:
	// dispense contaminates: wash after each dispense
	// aspirate contaminates, dispense does not: wash before each subsequent aspirate
	// aspirate does not contaminate, dispense doesn't contaminate: no washing required
	
	//private def doesWellContaminate(well: Well): Boolean = state.getLiquid(well).contaminates
	//val bDestContaminates = dests.exists(doesWellContaminate)
	
	//val bAspirateContaminates = liquid.bContaminates
	//val bDispenseContaminates = dispenseStrategy.bEnter && dests.exists(doesWellContaminate)
	
	val dests = SortedSet[Well]() ++ token.mapDestAndVolume.keys

	val mapDestToVolume = token.mapDestAndVolume
	
	val tokens: Seq[T1_Token] = {
		// Need to split into tip groups (e.g. large tips, small tips, all tips)
		// For each group, perform the pipetting and score the results
		// Pick the strategy with the best score
		var winner = Seq[T1_Token]()
		var nWinnerScore = Int.MaxValue
		for (tipGroup <- robot.config.tipGroups) {
			val tips = robot.config.tips.filter(tip => tipGroup.contains(tip.index))
	
			val cycles = pipetteLiquid(tips)
			if (!cycles.isEmpty) {
				val tokens = cycles.flatMap(_.toTokenSeq)
				val nScore = robot.score(tokens)
				if (nScore < nWinnerScore) {
					winner = tokens
					nWinnerScore = nScore
				}
			}
		}
		winner
	}
	
	// - Get all tip/dest pairs
	// - 
	private def pipetteLiquid(tips: SortedSet[Tip]): Seq[CycleState] = {
		// For each dispense, pick the top-most destination wells available in the next column
		// Break off dispense batch if any tips cannot fully dispense volume
		val cycles = new ArrayBuffer[CycleState]
		val state = new RobotStateBuilder(robot.state)
		
		// Pair up all tips and wells
		val twss0 = helper.chooseTipWellPairsAll(tips, dests)

		def createCycles(twss: List[Seq[TipWell]]): Boolean = {
			if (twss.isEmpty)
				return true
				
			val cycle = new CycleState(tips)
			val tws0 = twss.head
			
			// If we can't accommodate the required volume, abort:
			if (!checkVols(cycle, state, tws0))
				return false

			// Get the next destination for each tip
			val mapTipToDestNext: Map[Tip, Well] = twss.flatMap(tws=>tws).groupBy(_.tip).mapValues(pairs => pairs.head.well)
			// Tips actually used
			val tips0 = SortedSet[Tip]() ++ mapTipToDestNext.keys
			
			// Tuples of tip to clean degree required by source liquid
			val tcs = tips0.toSeq.map(tip => {
				val tipState = state.getTipState(tip)
				val cleanDegree = helper.getCleanDegreeAspirate(tipState, liquid)
				(tip, cleanDegree)
			})
			// Add clean tokens to cycle
			clean(cycle, state, tcs)

			// Pseduo-aspirate: Indicate that the tips contain the source liquid
			// We add 0-volume, because we don't know how much yet, but we need to put the proper liquid in already
			tips0.foreach(tip => state.addLiquid0(tip, liquid, 0))
			
			// Dispense the first batch
			dispense(cycle, state, tws0)
			
			// Add as many tip/dest groups to this cycle as possible, and return list of remaining groups
			val twssRest = twss.tail.dropWhile(tws => {
				if (checkVols(cycle, state, tws) && checkNoCleanRequired(cycle, state, tws)) {
					dispense(cycle, state, tws)
					true
				}
				else
					false
			})
			
			aspirate(cycle, state)
			cycles += cycle
			
			createCycles(twssRest)
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
			val nMin = robot.getTipAspirateVolumeMin(tip, liquid)
			val nMax = robot.getTipHoldVolumeMax(tip, liquid)
			val nTipVolume = cycle.mapTipToVolume(tip) // Volume already in the tip
			val nVolume = mapDestToVolume(well) // Volume to dispense; also volume to add to amount which the tip will need to aspirate
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

	private def dispense(cycle: CycleState, state: RobotStateBuilder, tws: Seq[TipWell]) {
		// Map tip/dest pairs to TipWellVolumeDispense objects
		val twvds = tws.map(tw => {
			val nVolume = mapDestToVolume(tw.well)
			val wellState = state.getWellState(tw.well)
			val dispenseKind = robot.getDispenseKind(tw.tip, liquid, nVolume, wellState)
			new TipWellVolumeDispense(tw.tip, tw.well, nVolume, dispenseKind)
		})
		val twvdss = robot.batchesForDispense(state, twvds)
		// Create dispense tokens
		cycle.dispenses ++= twvdss.map(twvds => new T1_Dispense(twvds))
		// Add volume to required aspirate volume for this cycle
		for (twvd <- twvds) {
			cycle.mapTipToVolume(twvd.tip) += twvd.nVolume
			state.dispense(twvd)
		}
	}

	private def aspirate(cycle: CycleState, state: RobotStateBuilder) {
		// Cases:
		// Case 1: tips size == srcs size:
		// Case 2: tips size < srcs size:
		// Case 3: tips size > srcs size:
		// -----
		// The sources should be chosen according to this algorithm:
		// - sort the sources by volume descending (secondary sort key is index order)
		// - keep the top tips.size() entries
		// Repeat the sorting each time all sources have been used (e.g. when there are more tips than sources)

		// Get list of tips which require aspiration	
		var tips = SortedSet[Tip]() ++ cycle.mapTipToVolume.filter({case (tip, n) => n > 0}).keys

		// sort the sources by volume descending (secondary sort key is index order)
		def order(well1: Well, well2: Well): Boolean = {
			val a = state.getWellState(well1)
			val b = state.getWellState(well2)
			(a.nVolume > b.nVolume) || (a.nVolume == b.nVolume && well1.index < well2.index)
		}
		// keep the top tips.size() entries ordered by index
		val srcs2 = SortedSet[Well](srcs.toSeq.sortWith(order).take(tips.size) : _*)
	
		val twss0 = helper.chooseTipSrcPairs(tips, srcs2, state)
		for (tws <- twss0) {
			val twvs = tws.map(tw => {
				val nVolume = cycle.mapTipToVolume(tw.tip)
				new TipWellVolume(tw.tip, tw.well, nVolume)
			})
			val twvss = robot.batchesForAspirate(state, twvs)
			cycle.aspirates ++= twvss.map(twvs => new T1_Aspirate(twvs))
			twvs.foreach(state.aspirate)
		}
	}
}
