package roboliq.devices.pipette

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._
//import roboliq.robot._


object PipetteHelper {
	def chooseTipWellPairsNext(states: RobotState, tips: SortedSet[TipConfigL2], wells: SortedSet[WellConfigL2], twsPrev: Seq[TipWell]): Seq[TipWell] = {
		//println("chooseTipWellPairsNext()")
		//println("tips: "+tips)
		//println("wells: "+wells)
		if (tips.isEmpty || wells.isEmpty)
			return Nil

		val (holder, wellsOnHolder, iCol) = getHolderWellsCol(states, wells, twsPrev)
		val well0 = getFirstWell(states, holder, wellsOnHolder, iCol)

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

	private def getHolderWellsCol(states: RobotState, wells: SortedSet[WellConfigL2], twsPrev: Seq[TipWell]): Tuple3[PlateConfigL2, SortedSet[WellConfigL2], Int] = {
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
		//println("wellRef_?: "+wellRef_?)

		// Get the holder of interest
		val holder: PlateConfigL2 = wellRef_? match {
			case None => wells.head.holder
			case Some(wellRef) => wellRef.holder
		}
		//println("holder: "+holder)

		// Either choose the first column or the column after the reference well
		val iCol = wellRef_? match {
			case None => 0
			case Some(wellRef) => (wellRef.index / holder.nRows + 1) % holder.nCols
		}

		val wellsOnHolder = wells.filter(_.holder eq holder)
		(holder, wellsOnHolder, iCol)
	}

	// Get the upper-most well in iCol.
	// If none found, loop through columns until wells are found
	private def getFirstWell(states: RobotState, holder: PlateConfigL2, wellsOnHolder: SortedSet[WellConfigL2], iCol0: Int): WellConfigL2 = {
		//println("getFirstWell()")
		//println("wells: "+wellsOnHolder)
		assert(!wellsOnHolder.isEmpty)
		assert(iCol0 >= 0 && iCol0 < holder.nCols)

		val nRows = holder.nRows
		val nCols = holder.nCols

		def checkCol(iCol: Int): WellConfigL2 = {
			val well_? = wellsOnHolder.find(_.index / nRows == iCol)
			well_? match {
				case Some(well) => well
				case None => checkCol((iCol + 1) % nCols)
			}
		}

		checkCol(iCol0)
	}

	def chooseTipWellPairsAll(states: RobotState, tips: SortedSet[TipConfigL2], dests: SortedSet[WellConfigL2]): Seq[Seq[TipWell]] = {
		//println("chooseTipWellPairsAll()")
		//println("tips: "+tips)
		//println("dests: "+dests)
		val twss = new ArrayBuffer[Seq[TipWell]]
		var destsRemaining = dests
		var twsPrev = Nil
		while (!destsRemaining.isEmpty) {
			val tws = chooseTipWellPairsNext(states, tips, destsRemaining, twsPrev)
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

	def chooseTipSrcPairs(states: RobotState, tips: SortedSet[TipConfigL2], srcs: SortedSet[WellConfigL2]): Seq[Seq[TipWell]] = {
		// Cases:
		// Case 1: tips size == srcs size:
		// Case 2: tips size < srcs size:
		// Case 3: tips size > srcs size:
		// -----
		// The sources should be chosen according to this algorithm:
		// - sort the sources by volume descending (secondary sort key is index order)
		// - keep the top tips.size() entries
		// Repeat the sorting each time all sources have been used (e.g. when there are more tips than sources)

		def processStep(tips0: Iterable[TipConfigL2]): Tuple2[Iterable[TipConfigL2], Seq[TipWell]] = {
			val tips = SortedSet[TipConfigL2](tips0.toSeq : _*)
			val tws = chooseTipWellPairsNext(states, tips, srcs, Nil)
			val tipsRemaining = tips -- tws.map(_.tip)
			(tipsRemaining.toSeq, tws)
		}
		process(tips, Nil, processStep)
	}
	
	def getCleanDegreeAspirate(tipState: TipStateL2, liquid: Liquid): WashIntensity.Value = {
		var bRinse = false
		var bThorough = false
		var bDecontam = false
		
		// Was a destination previously entered?
		val bDestEnteredPrev = !tipState.destsEntered.isEmpty
		bRinse |= bDestEnteredPrev

		// If the tip was previously empty (haven't aspirated previously):
		if (tipState.liquid eq Liquid.empty) {
			bThorough = true
			bDecontam = liquid.bRequireDecontamBeforeAspirate && tipState.cleanDegree < WashIntensity.Decontaminate
		}
		// Else if we're aspirating the same liquid as before:
		else if (tipState.liquid eq liquid) {
			bDecontam = liquid.bRequireDecontamBeforeAspirate && bDestEnteredPrev
		}
		// Else if we need to aspirate a new liquid:
		else {
			bDecontam = true
		}
		
		if (bDecontam) WashIntensity.Decontaminate
		else if (bThorough) WashIntensity.Thorough
		else if (bRinse) WashIntensity.Light
		else WashIntensity.None

	}

	def getCleanDegreeDispense(tipState: TipStateL2): WashIntensity.Value = {
		var bRinse = false
		var bThorough = false
		var bDecontam = false
		
		// Was a destination previously entered?
		val bDestEnteredPrev = !tipState.destsEntered.isEmpty
		bRinse |= bDestEnteredPrev
		bDecontam |= tipState.destsEntered.exists(!_.contaminants.isEmpty)

		if (bDecontam) WashIntensity.Decontaminate
		else if (bThorough) WashIntensity.Thorough
		else if (bRinse) WashIntensity.Light
		else WashIntensity.None
	}

	def choosePreAsperateReplacement(replacement_? : Option[TipReplacementAction.Value], liquidInWell: Liquid, tipState: TipStateL2): TipReplacementAction.Value = {
		// If there is no tip, then we'll need to get a new one
		if (tipState.sType_?.isEmpty) {
			TipReplacementAction.Replace
		}
		else {
			replacement_? match {
				// If the user has provided an override value, use it:
				case Some(action) =>
					action
				case None =>
					val bInsideOk = tipState.liquid.eq(liquidInWell) || tipState.contamInside.isEmpty
					val bOutsideOk = tipState.destsEntered.filter(_ ne Liquid.empty).isEmpty
					if (!bInsideOk || !bOutsideOk)
						TipReplacementAction.Replace
					else
						TipReplacementAction.None
			}
		}
	}
	
	def choosePreDispenseReplacement(replacement_? : Option[TipReplacementAction.Value], liquidInWell: Liquid, tipState: TipStateL2): TipReplacementAction.Value = {
		replacement_? match {
			case Some(action) =>
				action
			case None =>
				val bOutsideOk = tipState.destsEntered.filter(_ ne Liquid.empty).isEmpty
				if (!bOutsideOk)
					TipReplacementAction.Replace
				else
					TipReplacementAction.None
		}
	}
	
	def choosePreAsperateWashSpec(tipOverrides: TipHandlingOverrides, washIntensityDefault: WashIntensity.Value, liquidInWell: Liquid, tipState: TipStateL2): WashSpec = {
		val bInsideOk = tipState.liquid.eq(liquidInWell) || tipState.contamInside.isEmpty
		val bOutsideOk = tipState.destsEntered.filter(_ ne Liquid.empty).isEmpty
		val washIntensity = tipOverrides.washIntensity_? match {
			case Some(v) => v
			case None =>
				if (bInsideOk && bOutsideOk)
					WashIntensity.None
				else
					washIntensityDefault
		}
		
		val contamInside = tipOverrides.contamInside_? match { case Some(v) => v; case None => tipState.contamInside }
		val contamOutside = tipOverrides.contamOutside_? match { case Some(v) => v; case None => tipState.contamOutside }
		new WashSpec(washIntensity, contamInside, contamOutside)
	}
	
}
