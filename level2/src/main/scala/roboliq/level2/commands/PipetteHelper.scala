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
			val tws = chooseTipWellPairsNext(tips, destsRemaining, twsPrev)
			twss += tws
			destsRemaining --= tws.map(_.well)
		}
		twss
	}

	def process[T, T2](items: Iterable[T], acc: Seq[Seq[T2]], fn: Iterable[T] => Tuple2[Iterable[T], Seq[T2]]): Seq[Seq[T2]] = {
		if (items.isEmpty)
			acc
		else {
			val (itemsRemaining, accNew) = fn(items)
			if (accNew.isEmpty)
				Nil
			else
				process(itemsRemaining, acc ++ Seq(accNew), fn)
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

		def processStep(tips0: Iterable[Tip]): Tuple2[Iterable[Tip], Seq[TipWell]] = {
			val tips = tips0.asInstanceOf[SortedSet[Tip]]
			val tws = chooseTipWellPairsNext(tips, srcs, Nil)
			val tipsRemaining = tips -- tws.map(_.tip)
			(tipsRemaining, tws)
		}
		process(tips, Nil, processStep)
	}
	
	def getCleanDegreeAspirate(tipState: TipState, liquid: Liquid): CleanDegree.Value = {
		var bRinse = false
		var bThorough = false
		var bDecontam = false
		
		// Was a destination previously entered?
		val bDestEnteredPrev = !tipState.destsEntered.isEmpty
		bRinse |= bDestEnteredPrev

		// If the tip was previously empty (haven't aspirated previously):
		if (tipState.liquid eq Liquid.empty) {
			bThorough = true
			bDecontam = liquid.bRequireDecontamBeforeAspirate && tipState.cleanDegree < CleanDegree.Decontaminate
		}
		// Else if we're aspirating the same liquid as before:
		else if (tipState.liquid eq liquid) {
			bDecontam = liquid.bRequireDecontamBeforeAspirate && bDestEnteredPrev
		}
		// Else if we need to aspirate a new liquid:
		else {
			bDecontam = true
		}
		
		if (bDecontam) CleanDegree.Decontaminate
		else if (bThorough) CleanDegree.Thorough
		else if (bRinse) CleanDegree.Light
		else CleanDegree.None

	}

	def getCleanDegreeDispense(tipState: TipState): CleanDegree.Value = {
		var bRinse = false
		var bThorough = false
		var bDecontam = false
		
		// Was a destination previously entered?
		val bDestEnteredPrev = !tipState.destsEntered.isEmpty
		bRinse |= bDestEnteredPrev
		bDecontam |= tipState.destsEntered.exists(_.contaminates)

		if (bDecontam) CleanDegree.Decontaminate
		else if (bThorough) CleanDegree.Thorough
		else if (bRinse) CleanDegree.Light
		else CleanDegree.None
	}
}
