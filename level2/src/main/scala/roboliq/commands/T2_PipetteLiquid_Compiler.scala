package roboliq.commands

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import roboliq.parts._
import roboliq.tokens._
import roboliq.robot._
import roboliq.level2.tokens._


class T2_PipetteLiquid_Compiler(token: T2_PipetteLiquid, robot: Robot) {
	class CycleState(val tips: Seq[Tip]) {
		// Tips available for dispense.  Once a tip has dispensed with wet contact into a contaminating liquid, it should be removed from this list.
		val tipsAvailable = new ArrayBuffer[Tip]() ++ tips
		// The volume which needs to be aspirated into the given tip
		val mapTipToVolume = new mutable.HashMap[Tip, Double] ++ tips.map(_ -> 0.0)

		val aspirates = new ArrayBuffer[T1_Aspirate]
		val dispenses = new ArrayBuffer[T1_Dispense]

		
		def score(): Int = aspirates.size + dispenses.size
		def toTokenSeq: Seq[T1_Token] = aspirates ++ dispenses
	}
	
	val	srcs = token.srcs
	val liquid = robot.state.getLiquid(srcs.head)
	// Make sure that all the source wells contain the same liquid
	assert(srcs.forall(well => robot.state.getLiquid(well) == liquid))
	
	val state = new RobotStateBuilder(robot.state)
	
	// Contamination scenarios:
	// dispense contaminates: wash after each dispense
	// aspirate contaminates, dispense does not: wash before each subsequent aspirate
	// aspirate does not contaminate, dispense doesn't contaminate: no washing required
	
	private def doesWellContaminate(well: Well): Boolean = state.getLiquid(well).contaminates
	//val bDestContaminates = dests.exists(doesWellContaminate)
	
	//val bAspirateContaminates = liquid.bContaminates
	//val bDispenseContaminates = dispenseStrategy.bEnter && dests.exists(doesWellContaminate)
	
	val dests = {
		def sortDests(well1: Well, well2: Well): Boolean = {
			(well1.holder.index < well2.holder.index || well1.index < well2.index)
		}
		token.mapDestAndVolume.keys.toList.sortWith(sortDests)
	}

	val mapDestToVolume = token.mapDestAndVolume
	
	val tokens: Seq[T1_Token] = {
		// Need to split into tip groups (e.g. large tips, small tips, all tips)
		// For each group, perform the pipetting and score the results
		// Pick the strategy with the best score
		var winner: Seq[T1_Token] = null
		var nWinnerScore = Int.MaxValue
		for (tipGroup <- robot.config.tipGroups) {
			val tips = tipGroup.map(iTip => robot.config.tips(iTip))
	
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
	
	private def pipetteLiquid(tips: Seq[Tip]): Seq[CycleState] = {
		// For each dispense, pick the top-most destination wells available in the next column
		// Break off dispense batch if any tips cannot fully dispense volume
		val cycles = new ArrayBuffer[CycleState]
		
		val destsRemaining = new ArrayBuffer[Well]
		destsRemaining ++= dests

		var bOk = true
		while (!destsRemaining.isEmpty && bOk) {
			val cycle = new CycleState(tips)
			def dispense(wellPrev_? : Option[Well]) {
				val twvss = pipetteLiquid_dispenseBatchOnce(cycle, destsRemaining, wellPrev_?)
				if (!twvss.isEmpty) {
					for (twvs <- twvss) {
						assert(!twvs.isEmpty)
						cycle.dispenses += new T1_Dispense(twvs)
						for (twv <- twvs) {
							cycle.mapTipToVolume(twv.tip) += twv.nVolume
							// If dispense contaminates the tip, then don't let this tip be reused
							val wellState = state.getWellState(twv.well)
							robot.getDispenseKind(twv.tip, liquid, twv.nVolume, wellState) match {
								case DispenseKind.WetContact => cycle.tipsAvailable -= twv.tip
								case _ =>
							}
							destsRemaining -= twv.well
						}
						dispense(Some(twvs.head.well))
					}
				}
			}
			dispense(None)
			if (!destsRemaining.isEmpty) {
				bOk = false
			}
			else {
				aspirate(cycle)
				updateState(cycle)
				cycles += cycle
			}
		}
		
		cycles
	}

	private def pipetteLiquid_dispenseBatchOnce(cycle: CycleState, destsRemaining: Seq[Well], wellPrev_? : Option[Well]): Seq[Seq[TipWellVolumeDispense]] = {
		// Get a list of wells to dispense into
		val destsNext = getWellsForTips(cycle.tipsAvailable, destsRemaining, wellPrev_?)
		val tipsAndDests = cycle.tipsAvailable zip destsNext

		if (tipsAndDests.isEmpty)
			return Nil

		/*val dispenseKind0 = {
			val (tip0, well0) = tipsAndDest.head
			val nVolume0 = mapDestToVolume(well0) // Volume to dispense; also volume to add to amount which the tip will need to aspirate
			val wellState0 = state.getWellState(well0)
			robot.getDispenseKind(tip0, liquid, nVolume0, wellState0)
		}*/

		def pairIsOk(tipWell: Tuple2[Tip, Well]): Boolean = {
			val (tip, well) = tipWell
			val nMin = robot.getTipAspirateVolumeMin(tip, liquid)
			val nMax = robot.getTipHoldVolumeMax(tip, liquid)
			val nTipVolume = cycle.mapTipToVolume(tip) // Volume already in the tip
			val nVolume = mapDestToVolume(well) // Volume to dispense; also volume to add to amount which the tip will need to aspirate
			val bOk = (
				nVolume >= nMin &&
				nVolume + nTipVolume <= nMax //&&
				//dispenseKind == dispenseKind0
			)
			bOk
		}

		if (tipsAndDests.forall(pairIsOk)) {
			// Indicate that tip should dispense the given volume to dest
			val twvs = for ((tip, well) <- tipsAndDests) yield {
				val nVolume = mapDestToVolume(well)
				val wellState = state.getWellState(well)
				val dispenseKind = robot.getDispenseKind(tip, liquid, nVolume, wellState)
				new TipWellVolumeDispense(tip, well, nVolume, dispenseKind)
			}
			robot.batchesForDispense(twvs)
		}
		else {
			Nil
		}
	}

	/*private def resetTips() {
		for ((_, tipState) <- mapTipStates) {
			tipState.nVolume = 0
			tipState.bFree = true
		}
	}
	*/
	/*	
	private def newCycle(cycles: ArrayBuffer[CycleState], tips: Array[TipState]): CycleState = {
		val cycle = new CycleState(tips)
		cycles += cycle
		for (tip <- tips) {
			tip.nVolume = 0
			tip.bFree = true
		}
		cycle
	}
	*/
	private def aspirate(cycle: CycleState) {
		// Cases:
		// Case 1: tips size == srcs size:
		// Case 2: tips size < srcs size:
		// Case 3: tips size > srcs size:
		// -----
		// The sources should be chosen according to this algorithm:
		// - sort the sources by volume descending (secondary sort key is index order)
		// - keep the top tips.size() entries
		// Repeat the sorting each time all sources have been used (e.g. when there are more tips than sources)
		//val tips = cycle.tips
		val tips = cycle.tips
		var iTip = 0
		while (iTip < tips.size) {
			// sort the sources by volume descending (secondary sort key is index order)
			def order(well1: Well, well2: Well): Boolean = {
				val a = state.getWellState(well1)
				val b = state.getWellState(well2)
				(a.nVolume > b.nVolume) || (a.nVolume == b.nVolume && well1.index < well2.index) 
			}
			// keep the top tips.size() entries ordered by index
			val srcs2 = srcs.toSeq.sortWith(order).take(tips.size)
			val srcs3 = getWellsForTips(tips, srcs2, None).sortWith(_.index < _.index)
			assert(!srcs3.isEmpty)
			
			val twvsAll = new ArrayBuffer[TipWellVolume]
			var iSrc = 0
			while (iTip < tips.size && iSrc < srcs3.size) {
				val tip = tips(iTip)
				val nVolume = cycle.mapTipToVolume(tip)
				if (nVolume > 0) {
					val src = srcs3(iSrc)
					twvsAll += new TipWellVolume(tip, src, nVolume)
				}

				iTip += 1
				iSrc += 1
			}
			if (twvsAll.size > 0) {
				val twvss = robot.batchesForAspirate(twvsAll)
				cycle.aspirates ++= twvss.map(twvs => new T1_Aspirate(twvs))
			}
		}
	}

	private def updateState(cycle: CycleState) {
		for (tok <- cycle.aspirates) {
			for (twv <- tok.twvs)
				state.aspirate(twv)
		}
		for (tok <- cycle.dispenses) {
			for (twvd <- tok.twvs)
				state.dispense(twvd)
		}
	}

	private def getWellsForTips(tips: Seq[Tip], wellsAvailable: Seq[Well], wellPrev_? : Option[Well]): Seq[Well] = {
		if (tips.isEmpty || wellsAvailable.isEmpty)
			Nil
		// If the previous well is defined but is on a different holder, then ignore it
		else if (wellPrev_?.isDefined && !wellsAvailable.exists(_.holder == wellPrev_?.get.holder))
			getWellsForTips(tips, wellsAvailable, None)
		else {
			val _well = wellPrev_? match {
				case Some(well) => well
				case None => wellsAvailable.head
			}
			val holder = _well.holder
			val iCol = wellPrev_? match {
				case Some(well) => well.index / holder.nRows + 1
				case None => 0
			}
			val wellsOnHolder = wellsAvailable.filter(_.holder == holder)
			getWellsAlignedWithTips(tips, holder, wellsOnHolder, iCol)
		}
	}
	
	// Pick the top-most destination wells available in the given column
	// If none found, loop through columns until wells are found
	private def getWellsAlignedWithTips(tips: Seq[Tip], holder: WellHolder, wellsOnHolder: Seq[Well], iCol0: Int): Seq[Well] = {
		val nRows = holder.nRows
		val nCols = holder.nCols
		var iCol = iCol0
		var wellsInCol: Seq[Well] = null
		do {
			wellsInCol = wellsOnHolder.filter(_.index / nRows == iCol).take(tips.size)
			if (wellsInCol.isEmpty) {
				iCol = (iCol + 1) % nCols
				assert(iCol != iCol0)
			}
		} while (wellsInCol.isEmpty)
		wellsInCol
	}
}
