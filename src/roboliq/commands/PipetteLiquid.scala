package roboliq.commands

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import roboliq.parts._
import roboliq.tokens._
import roboliq.robot._


class PipetteLiquid(robot: Robot, srcs: Array[Well], dests: Array[Well], volumes: Array[Double], aspirateStrategy: AspirateStrategy, dispenseStrategy: DispenseStrategy) {
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
	
	val mapDestToVolume = Map() ++ (dests zip volumes)
	
	val tokens: Seq[Token] = {
		// Need to split into tip groups (e.g. large tips, small tips, all tips)
		// For each group, perform the pipetting and score the results
		// Pick the strategy with the best score
		var winner: Seq[Token] = null
		var nWinnerScore = Int.MaxValue
		for (tipGroup <- robot.config.tipGroups) {
			val tips = tipGroup.map(t => new TipState(robot.config.tips(t)))
	
			val cycles = pipetteLiquid(tips)
			val tokens = cycles.flatMap(_.toTokenSeq)
			val nScore = robot.score(tokens)
			if (nScore < nWinnerScore) {
				winner = tokens
				nWinnerScore = nScore
			}
		}
		winner
	}
	
	/*
	private def pipetteLiquid_DispenseContaminates(tips: Array[TipState]): Seq[CycleState] = {
		// For each destination well:
		//  - assert that there are tips which can completely hold the desired volume
		//  - try to select the first free tip which can completely hold the desired volume:
		//     - if available, indicate that the volume should be drawn into that tip from the next available source
		//  - otherwise append clean/aspirate/dispense tokens
		
		def handle() {
			cycle.aspirates ++= handleAspirates(tips, srcs, aspirateStrategy)
			handleDispense(tips)
		}
		
		val cycles = new ArrayBuffer[CycleState]
		var cycle = new CycleState(tips)
		var twvs = new ArrayBuffer[TipWellVolume]
		for (dest <- dests) {
			val nVolume = mapDestToVolume(dest)
			assert(tipsExistForVolume(nVolume))
			tips.find(tipIsFreeAndHasVolume(nVolume)) match {
				case Some(tip) =>
					twvs += new TipWellVolume(tip.tip, dest, nVolume)
					tip.nVolume += nVolume
				case None =>
					handle()
			}
		}
		
		handle()
		
		cycles
	}
	*/
	
	private def pipetteLiquid(tips: Array[TipState]): Seq[CycleState] = {
		// For each dispense, pick the top-most destination wells available in the next column
		// Break off dispense batch if any tips cannot fully dispense volume
		val cycles = new ArrayBuffer[CycleState]
		var cycle: CycleState = null
		cycle = newCycle(cycles, tips)
		
		val destsRemaining = new ArrayBuffer[Well]
		destsRemaining ++= dests
		
		var wellPrev_? : Option[Well] = None
		while (!destsRemaining.isEmpty) {
			var bNewCycle = false
			val tipsFree = tips.filter(_.bFree)
			if (tipsFree.isEmpty) {
				bNewCycle = true
			}
			else {
				val destsNext = getNextDestBatch(tipsFree, destsRemaining, wellPrev_?)
				assert(!destsNext.isEmpty)
	
				val destsNextWithIndex = destsNext.zipWithIndex
				
				// Check whether the free tips all have enough free volume for their respective destinations
				val bOk = destsNextWithIndex.forall(pair => {
					val (dest, i) = pair
					val nVolume = mapDestToVolume(dest)
					val tip = tipsFree(i)
					tipIsFreeAndHasVolume(nVolume)(tip)
				})
				
				if (bOk) {
					// Indicate that tip should dispense the given volume to dest
					val twvs = new ArrayBuffer[TipWellVolume]
					for ((dest, i) <- destsNextWithIndex) {
						val nVolume = mapDestToVolume(dest)
						val tip = tipsFree(i)
						tip.nVolume += nVolume
						// If dispense contaminates the tip, then don't let this tip be reused
						if (bDispenseContaminates)
							tip.bFree = false
						twvs += new TipWellVolume(tip.tip, dest, nVolume)
						destsRemaining -= dest
					}
					cycle.dispenses += new Dispense(twvs, dispenseStrategy)
					wellPrev_? = Some(destsNext.head)
				}
				else {
					bNewCycle = true
				}
			}
			
			if (bNewCycle) {
				aspirate(cycle, tips)
				cycle = newCycle(cycles, tips)
				assert(tips.exists(_.bFree))
			}
		}
		
		aspirate(cycle, tips)
		
		cycles
	}

	private def tipIsFreeAndHasVolume(nVolume: Double)(tip: TipState): Boolean =
		(tip.bFree && nVolume + tip.nVolume >= tip.nVolumeMin && nVolume + tip.nVolume <= tip.nVolumeMax)
	
	private def newCycle(cycles: ArrayBuffer[CycleState], tips: Array[TipState]): CycleState = {
		val cycle = new CycleState(tips)
		cycles += cycle
		for (tip <- tips) {
			tip.nVolume = 0
			tip.bFree = true
		}
		cycle
	}
	
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
			
			val twvs = new ArrayBuffer[TipWellVolume]
			var iSrc = 0
			while (iTip < tips.size && iSrc < srcs3.size) {
				val tip = tips(iTip)
				val nVolume = tip.nVolume
				if (nVolume > 0) {
					val src = srcs3(iSrc)
					val liquid = state.getLiquid(src)
					
					twvs += new TipWellVolume(tip.tip, src, nVolume)
					state.removeLiquid(src, nVolume)
					tip.bContaminated |= liquid.bContaminates
				}

				iTip += 1
				iSrc += 1
			}
			if (twvs.size > 0)
				cycle.aspirates += new Aspirate(twvs, aspirateStrategy)
		}
	}

	private def getNextDestBatch(tips: Seq[TipState], wellsAvailable: Seq[Well], wellPrev_? : Option[Well]): Seq[Well] = {
		// If the previous well is defined but is on a different holder, then ignore it
		if (wellPrev_?.isDefined && !wellsAvailable.exists(_.holder == wellPrev_?.get.holder))
			getNextDestBatch(tips, wellsAvailable, None)
		else {
			val _well = wellPrev_? match {
				case Some(well) => well
				case None => wellsAvailable(0)
			}
			val holder = _well.holder
			val iCol = wellPrev_? match {
				case Some(well) => well.index / holder.nRows
				case None => 0
			}
			val wellsOnHolder = wellsAvailable.filter(_.holder == holder)
			getNextDestBatch(tips, holder, wellsOnHolder, iCol)
		}
	}
	
	// Pick the top-most destination wells available in the given column
	// If none found, loop through columns until wells are found
	private def getNextDestBatch(tips: Seq[TipState], holder: WellHolder, wellsOnHolder: Seq[Well], iCol0: Int): Seq[Well] = {
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
