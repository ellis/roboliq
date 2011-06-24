package roboliq.level2.commands

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import roboliq.parts._
import roboliq.tokens._
import roboliq.robot._
import roboliq.level2.tokens._


class PipetteHelper {
	def chooseTipWellPairsNext(tips: SortedSet[Tip], wells: SortedSet[Well], twsPrev: Seq[TipWell]): Seq[TipWell] = {
		if (tips.isEmpty || wells.isEmpty)
			return Nil

		val (holder, wellsOnHolder, iCol) = getHolderWellsCol(wells, twsPrev)
		val well0 = getFirstWell(holder, wellsOnHolder, iCol)

		val tip0 = tips.head
		val iRowTip0 = tip0.index
		val iColTip0 = 0
		val iRowWell0 = well0.index % holder.nRows
		val iColWell0 = well0.index / holder.nRows

		val pairs = new ArrayBuffer[TipWell]
		pairs += new TipWell(tip0, well0)
		for (tip <- tips.tail) {
			val dRowTip = tip.index - tip0.index
			val iRowWell = iRowWell0 + dRowTip
			val iColWell = iColWell0
			val iWell = iColWell * holder.nRows + iRowWell
			wellsOnHolder.find(_.index == iWell) match {
				case None =>
				case Some(well) => pairs += new TipWell(tip, well)
			}
		}
		pairs.toSeq
	}

	private def getHolderWellsCol(wells: SortedSet[Well], twsPrev: Seq[TipWell]): Tuple3[WellHolder, SortedSet[Well], Int] = {
		// Pick a "reference" well if twsPrev isn't empty
		val wellRef_? = {
			if (twsPrev.isEmpty)
				None
			else {
				val wellRef = twsPrev.last.well
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

	def chooseTipWellPairsAll(tips: SortedSet[Tip], dests: SortedSet[Well]): Seq[Seq[TipWell]] = {
		val twss = new ArrayBuffer[Seq[TipWell]]
		var destsRemaining = dests
		var twsPrev = Nil
		while (!destsRemaining.isEmpty) {
			val tws = chooseTipWellPairs(tips, destsRemaining, twsPrev)
			twss += tws
			destsRemaining --= tws.map(_.well)
		}
		twss
	}

	def process[T, T2](items: Iterable[T], acc: Seq[Seq[T2]], fn: Iterable[T] => Tuple2[Iterable[T], Seq[T2]]: Seq[Seq[T2]] = {
		if (items.isEmpty)
			acc
		else {
			val (itemsRemaining, accNew) = fn(items)
			if (accNew.isEmpty)
				Nil
			else
				x(itemsRemaining, acc ++ accNew)
		}
	}

	def chooseTipSrcPairs(tips: SortedSet[Tip], srcs: SortedSet[Well], state: IRobotState): Seq[Seq[TipWell]] = {
		// Cases:
		// Case 1: tips size == srcs size:
		// Case 2: tips size < srcs size:
		// Case 3: tips size > srcs size:
		// -----
		// The sources should be chosen according to this algorithm:
		// - sort the sources by volume descending (secondary sort key is index order)
		// - keep the top tips.size() entries
		// Repeat the sorting each time all sources have been used (e.g. when there are more tips than sources)

		// sort the sources by volume descending (secondary sort key is index order)
		def order(well1: Well, well2: Well): Boolean = {
			val a = state.getWellState(well1)
			val b = state.getWellState(well2)
			(a.nVolume > b.nVolume) || (a.nVolume == b.nVolume && well1 < well2)
		}
		// keep the top tips.size() entries ordered by index
		var srcs2 = SortedSet[Well](srcs.toSeq.sortWith(order).take(tips.size) : _*)

		def processStep(tips: SortedSet[Tip]): Tuple2[Iterable[Tip], Seq[TipWell]] = {
			val tws = helper.chooseTipWellPairsNext(tips, srcs2, Nil)
			srcs2 --= tws.map(_.well)
			val tipsRemaining = tips -- tws.map(_.tip)
			(tipsRemaining, tws)
		}
		process(tips, Nil, processStep)
	}
	
	def getCleanDegreeAspirate(tipState: TipState, srcState: WellState): CleanDegree.Value = {
		var bRinse = false
		var bThorough = false
		var bDecontam = false
		
		// Was a destination previously entered?
		val bDestEnteredPrev = !tipState.destsEntered.isEmpty
		bRinse |= bDestEnteredPrev

		// If the tip was previously empty (haven't aspirated previously):
		if (tipState.liquid eq Liquid.empty) {
			bThorough = true
			bDecontam = srcState.liquid.bRequireDecontamBeforeAspirate && tipState.cleanDegree < CleanDegree.Decontaminate
		}
		// Else if we're aspirating the same liquid as before:
		else if (tipState.liquid eq srcState.liquid) {
			bDecontam = srcState.liquid.bRequireDecontamBeforeAspirate && bDestEnteredPrev
		}
		// Else if we need to aspirate a new liquid:
		else {
			bDecontam = true
		}
		
		if (bDecontam) CleanDegree.Decontam
		else if (bThorough) CleanDegree.Thorough
		else if (bRinse) CleanDegree.Rinse
		else CleandDegree.None

	}

	def getCleanDegreeDispense(tipState: TipState, destState: WellState): CleanDegree.Value = {
		var bRinse = false
		var bThorough = false
		var bDecontam = false
		
		// Was a destination previously entered?
		val bDestEnteredPrev = !tipState.destsEntered.isEmpty
		bRinse |= bDestEnteredPrev

		if (bDecontam) CleanDegree.Decontam
		else if (bThorough) CleanDegree.Thorough
		else if (bRinse) CleanDegree.Rinse
		else CleandDegree.None
	}
}

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
		val state = new RobotStateBuilder(robot.state)
		
		// Pair up all tips and wells
		val twss0 = helper.chooseTipWellPairsAll(tips, dests)

		def x() {
			twss match {
				case Seq(tws, rest @ _*) =>
					val cycle = new CycleState(tips)
					if (!checkVols(cycle, state, tws))
						error()
					dispense(cycle, state, tws)
					chooseSrcs(cycle, state)
					//aspirate(cycle, state)
					clean(cycle, state)
			}
		}
		var destsRemaining = dests

		var bOk = true
		while (!destsRemaining.isEmpty && bOk) {
			val cycle = new CycleState(tips)
			// Dispense as many times as the tip group can accommodate
			def dispense(twsPrev: Seq[TipWell]) {
				val tws = helper.chooseTipWellPairs(cycle.tipsAvailable, destsRemaining, twsPrev)
				// If this is not the first dispense in this cycle, check whether we need to clean the tips before dispensing
				val bCleanFirst = !cycle.dispenses.isEmpty && twvs.exists(twv => {
					val tipState = state.getTipState(twv.tip)
					val destState = state.getWellState(twv.well)
					val cleanDegree == helper.getCleanDegree(tipState, None, destState)
					cleanDegree != CleanDegree.None
				})
				val twvss = pipetteLiquid_dispenseBatchOnce(cycle, /*destsRemaining, */tws)
				if (!twvss.isEmpty) {
					for (twvs <- twvss) {
						assert(!twvs.isEmpty)
						// Only use this set if we don't need to clean first
						if (!bCleanFirst) {
							cycle.dispenses += new T1_Dispense(twvs)
							destsRemaining --= twvs.map(_.well)
							for (twv <- twvs) {
								cycle.mapTipToVolume(twv.tip) += twv.nVolume
							}
						}
					}
					dispense(tws)
				}
			}
			dispense(Nil)
			if (cycle.dispenses.isEmpty) {
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

	// Check for appropriate volumes
	private def checkVols(cycle: CycleState, state: RobotStateBuilder, tws: Seq[TipWell]): Boolean = {
		if (tws.isEmpty)
			return false
		
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

	private def dispense(cycle: CycleState, state: RobotStateBuilder, tws: Seq[TipWell]) {
		// Indicate that tip should dispense the given volume to dest
		val twvds = for (tw <- tws) yield {
			val nVolume = mapDestToVolume(tw.well)
			val wellState = state.getWellState(tw.well)
			val dispenseKind = robot.getDispenseKind(tw.tip, liquid, nVolume, wellState)
			new TipWellVolumeDispense(tw.tip, tw.well, nVolume, dispenseKind)
		}

		cycle.dispenses ++= twvss.map(twvs => new T1_Dispense(twvs))
	}

	private def dispenseBatches(cycle: CycleState, state: RobotStateBuilder, tws: Seq[TipWell]): Seq[Seq[TipWellVolumeDispense]] = {
		// Get a list of wells to dispense into
		//val tws = helper.chooseTipWellPairs(cycle.tipsAvailable, destsRemaining, wellPrev_?)
		if (tws.isEmpty)
			return Nil

		/*val dispenseKind0 = {
			val (tip0, well0) = tipsAndDest.head
			val nVolume0 = mapDestToVolume(well0) // Volume to dispense; also volume to add to amount which the tip will need to aspirate
			val wellState0 = state.getWellState(well0)
			robot.getDispenseKind(tip0, liquid, nVolume0, wellState0)
		}*/

		def pairIsOk(tipWell: TipWell): Boolean = {
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

		if (tws.forall(pairIsOk)) {
			// Indicate that tip should dispense the given volume to dest
			val twvds = for (tw <- tws) yield {
				val nVolume = mapDestToVolume(tw.well)
				val wellState = state.getWellState(tw.well)
				val dispenseKind = robot.getDispenseKind(tw.tip, liquid, nVolume, wellState)
				new TipWellVolumeDispense(tw.tip, tw.well, nVolume, dispenseKind)
			}
			robot.batchesForDispense(twvds)
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
		var tips = cycle.tips.filter(tip => cycle.mapTipToVolume(tip) > 0)
		while (!tips.isEmpty) {
			// sort the sources by volume descending (secondary sort key is index order)
			def order(well1: Well, well2: Well): Boolean = {
				val a = state.getWellState(well1)
				val b = state.getWellState(well2)
				(a.nVolume > b.nVolume) || (a.nVolume == b.nVolume && well1.index < well2.index)
			}
			// keep the top tips.size() entries ordered by index
			val srcs2 = SortedSet[Well](srcs.toSeq.sortWith(order).take(tips.size) : _*)
			val tws = helper.chooseTipWellPairs(tips, srcs2, Nil)
			assert(!tws.isEmpty)
			
			val twvsAll = tws.map(tw => {
				val nVolume = cycle.mapTipToVolume(tw.tip)
				new TipWellVolume(tw.tip, tw.well, nVolume)
			})
			if (!twvsAll.isEmpty) {
				val twvss = robot.batchesForAspirate(twvsAll)
				cycle.aspirates ++= twvss.map(twvs => new T1_Aspirate(twvs))
			}
			tips --= tws.map(_.tip)
		}
	}

	private def clean(cycle: CycleState, state: RobotStateBuilder) {
		val mapTipToSrc: Map[Tip, Well] = cycle.aspirates.flatMap(_.twvs.map(twv => twv.tip -> twv.well).toMap
		val mapTipToDest: Map[Tip, Well] = cycle.dispenses.flatMap(_.tvws.map(twvd => twvd.well).toMap
		
				val tipState = state.getTipState(twv.tip)
				val wellState = state.getWellState(twv.well)
				if (wellState.liquid.bRequireDecontamBeforeAspirate)
					clean3 += twv.tip
				else
					clean2 += twv.tip
			}
		}
	}

	private def cleanBefore(cycles: Seq[CycleState]) {
		// Go through aspirates and find a) all tips which will be used, and b) cleaningDegree required
		val clean2 = new ArrayBuffer[Tip]
		val clean3 = new ArrayBuffer[Tip]
		for (cycle <- cycles) {
			for (aspirate <- cycle.aspirates) {
				for (twv <- aspirate.twvs) {
					val wellState = robot.state.getWellState(twv.well)
					if (wellState.liquid.bRequireDecontamBeforeAspirate)
						clean3 += twv.tip
					else
						clean2 += twv.tip
				}
			}
		}

		val tcs = clean2.map(_ -> CleanDegree.Thorough) ++ 
			clean3.map(_ -> CleanDegree.Decontaminate)
		val cs = robot.batchesForClean(tcs)
		if (!cs.isEmpty)
			cycles.head.cleans ++= cs
	}

	private def cleanBetween(cycle: CycleState) {
		// The tip will need to be cleaned before aspiration if it has been contaminated by a liquid other than the liquid in the source well.
		// By default, cleanDegree will be Rinse
		// Use CleanDegree.Decontaminate if:
		//  
		val mapTipToSrc = new mutable.HashMap[Tip, Well]
		for (aspirate <- cycle.aspirates) {
			for (twv <- aspirate.twvs) {
				mapTipToSrc(twv.tip) = twv.well
			}
		}
		
		val mapTipToDests = new mutable.HashMap[Tip, List[Well]]
		for (dispense <- cycle.dispenses) {
			for (twvd <- dispense.twvs) {
				val dests = mapTipToDests.getOrElse(twvd.tip, Nil)
				mapTipToDests(twvd.tip) = twvd.well :: dests
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
