package roboliq.level2.commands

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import roboliq.parts._
import roboliq.tokens._
import roboliq.robot._
import roboliq.level2.tokens._


class PipetteHelper {
	def chooseTipWellPairs(tips: SortedSet[Tip], wells: SortedSet[Well], twvsPrev: Seq[TipWellVolume]): Seq[Tuple2[Tip, Well]] = {
		if (tips.isEmpty || wells.isEmpty)
			return Nil

		val (holder, wellsOnHolder, iCol) = getHolderWellsCol(wells, twvsPrev)
		val well0 = getFirstWell(holder, wellsOnHolder, iCol)

		val tip0 = tips.head
		val iRowTip0 = tip0.index
		val iColTip0 = 0
		val iRowWell0 = well0.index % holder.nRows
		val iColWell0 = well0.index / holder.nRows

		val pairs = new ArrayBuffer[Tuple2[Tip, Well]]
		pairs += (tip0 -> well0)
		for (tip <- tips.tail) {
			val dRowTip = tip.index - tip0.index
			val iRowWell = iRowWell0 + dRowTip
			val iColWell = iColWell0
			val iWell = iColWell * holder.nRows + iRowWell
			wellsOnHolder.find(_.index == iWell) match {
				case None =>
				case Some(well) => pairs += (tip -> well)
			}
		}
		pairs.toSeq
	}

	private def getHolderWellsCol(wells: SortedSet[Well], twvsPrev: Seq[TipWellVolume]): Tuple3[WellHolder, SortedSet[Well], Int] = {
		// Pick a "reference" well if twvsPrev isn't empty
		val wellRef_? = {
			if (twvsPrev.isEmpty)
				None
			else {
				val wellRef = twvsPrev.last.well
				val holderRef = wellRef.holder
				if (wells.exists(_.holder == holderRef))
					Some(wellRef)
				else
					None
			}
		}

		// Get the holder of interest
		val holder = wellRef_? match {
			case None => wells.head.holder
			case Some(wellRef) => wellRef.holder
		}

		// Either choose the first column or the column after the reference well
		val iCol = wellRef_? match {
			case None => 0
			case Some(wellRef) => (wellRef.index / holder.nRows + 1) % holder.nCols
		}

		val wellsOnHolder = wells.filter(_.holder == holder)
		(holder, wellsOnHolder, iCol)
	}

	// Get the upper-most well in iCol.
	// If none found, loop through columns until wells are found
	private def getFirstWell(holder: WellHolder, wellsOnHolder: SortedSet[Well], iCol0: Int): Well = {
		assert(!wellsOnHolder.isEmpty)
		assert(iCol0 >= 0 && iCol0 < holder.nCols)

		val nRows = holder.nRows
		val nCols = holder.nCols

		def checkCol(iCol: Int): Well = {
			val well_? = wellsOnHolder.find(_.index / nRows == iCol)
			well_? match {
				case Some(well) => well
				case None => checkCol((iCol + 1) % nCols)
			}
		}

		checkCol(iCol0)
	}
}

class T2_PipetteLiquid_Compiler(token: T2_PipetteLiquid, robot: Robot) {
	class CycleState(val tips: SortedSet[Tip]) {
		// Tips available for dispense.  Once a tip has dispensed with wet contact into a contaminating liquid, it should be removed from this list.
		var tipsAvailable = tips
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
	
	private def pipetteLiquid(tips: SortedSet[Tip]): Seq[CycleState] = {
		// For each dispense, pick the top-most destination wells available in the next column
		// Break off dispense batch if any tips cannot fully dispense volume
		val cycles = new ArrayBuffer[CycleState]
		
		var destsRemaining = dests

		var bOk = true
		while (!destsRemaining.isEmpty && bOk) {
			val cycle = new CycleState(tips)
			// Dispense as many times as the tip group can accommodate
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

	private def pipetteLiquid_dispenseBatchOnce(cycle: CycleState, destsRemaining: SortedSet[Well], wellPrev_? : Option[Well]): Seq[Seq[TipWellVolumeDispense]] = {
		// Get a list of wells to dispense into
		val tipsAndDests = robot.chooseTipWellPairs(cycle.tipsAvailable, destsRemaining, wellPrev_?)
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
			val srcs2 = SortedSet[Well](srcs.toSeq.sortWith(order).take(tips.size) : _*)
			val pairs = robot.chooseTipWellPairs(tips, srcs2, None)
			assert(!pairs.isEmpty)
			
			val twvsAll = new ArrayBuffer[TipWellVolume]
			for ((tip, src) <- pairs) {
				val nVolume = cycle.mapTipToVolume(tip)
				if (nVolume > 0) {
					twvsAll += new TipWellVolume(tip, src, nVolume)
				}
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
}

