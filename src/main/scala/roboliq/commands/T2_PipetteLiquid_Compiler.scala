package roboliq.commands

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import roboliq.parts._
import roboliq.tokens._
import roboliq.robot._


class T2_PipetteLiquid_Compiler(token: T2_PipetteLiquid, robot: Robot) {
	class TipState(val tip: Tip) {
		var bFree = true
		var bContaminated = true
		var nVolume = 0.0
		
		//def index = tip.index
		def nVolumeMin = tip.nVolumeMin
		def nVolumeMax = tip.nVolumeMax
	}
	
	class CycleState(val tips: Array[TipState]) {
		val aspirates = new ArrayBuffer[Aspirate]
		val dispenses = new ArrayBuffer[Dispense]
		
		def score(): Int = aspirates.size + dispenses.size
		def toTokenSeq: Seq[Token] = aspirates ++ dispenses
	}
	

	assert(dests.size == volumes.size)
	
	val liquid = robot.state.getLiquid(srcs(0))
	// Make sure that all the source wells contain the same liquid
	assert(srcs.forall(well => robot.state.getLiquid(well) == liquid))
	
	val state = new RobotStateBuilder(robot.state)
	
	// Contamination scenarios:
	// dispense contaminates: wash after each dispense
	// aspirate contaminates, dispense does not: wash before each subsequent aspirate
	// aspirate does not contaminate, dispense doesn't contaminate: no washing required
	
	private def doesWellContaminate(well: Well): Boolean = state.getLiquid(well).bContaminates
	//val bDestContaminates = dests.exists(doesWellContaminate)
	
	val bAspirateContaminates = liquid.bContaminates
	val bDispenseContaminates = dispenseStrategy.bEnter && dests.exists(doesWellContaminate)
	
	val destsSorted = {
		def sortDests(well1: Well, well2: Well): Boolean = {
			(well1.holder.index < well2.holder.index || well1.index < well2.index)
		}
		dests.sortWith(sortDests)
	}
	
	val mapDestToVolume = (dests zip volumes).toMap
	val mapTipStates = robot.config.tips.map(tip => tip -> new TipState(tip)).toMap
	
	val tokens: Seq[T1_Token] = {
		// Need to split into tip groups (e.g. large tips, small tips, all tips)
		// For each group, perform the pipetting and score the results
		// Pick the strategy with the best score
		var winner: Seq[Token] = null
		var nWinnerScore = Int.MaxValue
		for (tipGroup <- robot.config.tipGroups) {
			val tips = tipGroup.map(iTip => mapTipStates(robot.config.tips(iTip)))
	
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
	
	private def pipetteLiquid(tips: Array[TipState]): Seq[CycleState] = {
		// For each dispense, pick the top-most destination wells available in the next column
		// Break off dispense batch if any tips cannot fully dispense volume
		val cycles = new ArrayBuffer[CycleState]
		
		val destsRemaining = new ArrayBuffer[Well]
		destsRemaining ++= destsSorted

		var bOk = true
		while (!destsRemaining.isEmpty && bOk) {
			val dispenses = new ArrayBuffer[Dispense]
			def dispense(wellPrev_? : Option[Well]) {
				val twvss = pipetteLiquid_dispenseBatchOnce(tips, destsRemaining, wellPrev_?)
				if (!twvss.isEmpty) {
					for (twvs <- twvss) {
						assert(!twvs.isEmpty)
						dispenses += new T1_Dispense(twvs)
						for (twv <- twvs) {
							val tipState = mapTipStates(twv.tip)
							tipState.nVolume += twv.nVolume
							// If dispense contaminates the tip, then don't let this tip be reused
							if (bDispenseContaminates)
								tipState.bFree = false
							destsRemaining -= twv.well
						}
						dispense(Some(twvs.head.head.well))
					}
				}
			}
			dispense(None)
			if (!destsRemaining.isEmpty) {
				bOk = false
			}
			else {
				val cycle = new CycleState(tips)
				cycle.dispenses ++= dispenses
				aspirate(cycle, tips)
				cycles += cycle
				resetTips()
			}
		}
		
		cycles
	}
	
	private def pipetteLiquid_dispenseBatchOnce(tips: Seq[TipState], destsRemaining: Seq[Well], wellPrev_? : Option[Well]): Seq[Seq[TipWellVolume]] = {
		// Get a list of wells to dispense into
		val destsNext = getNextDestBatch(tips, destsRemaining, wellPrev_?)
		//assert(!destsNext.isEmpty)
		// Check whether the tips all have enough free volume for their respective destinations
		val tipsAndDests = tips zip destsNext
		val bTipsHaveSpace = tipsAndDests.forall(pair => {
			val nVolume = mapDestToVolume(pair._2)
			tipIsFreeAndHasVolume(nVolume)(pair._1)
		})
		// If they do:
		if (bTipsHaveSpace) {
			// Indicate that tip should dispense the given volume to dest
			val twvs = for ((tip, dest) <- tipsAndDests) yield {
				val nVolume = mapDestToVolume(dest)
				new TipWellVolume(tip.tip, dest, nVolume)
			}
			robot.batchesForDispense(twvs)
		}
		else {
			Nil
		}
	}

	private def tipIsFreeAndHasVolume(nVolume: Double)(tip: TipState): Boolean =
		(tip.bFree && nVolume >= tip.nVolumeMin && nVolume + tip.nVolume <= tip.nVolumeMax)
	
	private def resetTips() {
		for ((_, tipState) <- mapTipStates) {
			tipState.nVolume = 0
			tipState.bFree = true
		}
	}
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
	private def aspirate(cycle: CycleState, tips: Array[TipState]) {
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
		val tips0 = tips.map(_.tip)
		var iTip = 0
		while (iTip < tips.size) {
			// sort the sources by volume descending (secondary sort key is index order)
			def order(well1: Well, well2: Well): Boolean = {
				val a = state.getWellState(well1)
				val b = state.getWellState(well2)
				(a.nVolume > b.nVolume) || (a.nVolume == b.nVolume && well1.index < well2.index) 
			}
			// keep the top tips.size() entries ordered by index
			val srcs2 = srcs.sortWith(order).take(tips.size)
			val srcs3 = robot.chooseWellsForTips(tips0, srcs2).sortWith(_.index < _.index)
			assert(!srcs3.isEmpty)
			
			val twvsAll = new ArrayBuffer[TipWellVolume]
			var iSrc = 0
			while (iTip < tips.size && iSrc < srcs3.size) {
				val tip = tips(iTip)
				val nVolume = tip.nVolume
				if (nVolume > 0) {
					val src = srcs3(iSrc)
					val liquid = state.getLiquid(src)
					
					twvsAll += new TipWellVolume(tip.tip, src, nVolume)
					state.removeLiquid(src, nVolume)
					tip.bContaminated |= liquid.bContaminates
				}

				iTip += 1
				iSrc += 1
			}
			if (twvsAll.size > 0) {
				val twvss = robot.batchesForAspirate(twvs)
				cycle.aspirates ++= twvss.map(twvs => new Aspirate(twvs))
			}
		}
	}

	private def getNextDestBatch(tips: Traversable[TipState], wellsAvailable: Seq[Well], wellPrev_? : Option[Well]): Seq[Well] = {
		if (tips.isEmpty || wellsAvailable.isEmpty)
			Nil
		// If the previous well is defined but is on a different holder, then ignore it
		else if (wellPrev_?.isDefined && !wellsAvailable.exists(_.holder == wellPrev_?.get.holder))
			getNextDestBatch(tips, wellsAvailable, None)
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
			getNextDestBatch2(tips, holder, wellsOnHolder, iCol)
		}
	}
	
	// Pick the top-most destination wells available in the given column
	// If none found, loop through columns until wells are found
	private def getNextDestBatch2(tips: Traversable[TipState], holder: WellHolder, wellsOnHolder: Seq[Well], iCol0: Int): Seq[Well] = {
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
