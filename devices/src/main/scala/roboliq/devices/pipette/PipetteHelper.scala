package roboliq.devices.pipette

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._
//import roboliq.robot._


object PipetteHelper {
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
		//println(well0.index+" START")
		for (tip <- tips.tail) {
			val dRowTip = tip.index - tip0.index
			val iWell = well0.index + dRowTip
			val iColWell = iWell / holder.nRows
			if (iColWell == iColWell0) {
				wellsOnHolder.find(_.index == iWell) match {
					case None =>
					case Some(well) => pairs += new TipWell(tip, well)
				}
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

	def chooseAdjacentWellsByVolume(states: RobotState, wells: Set[WellConfigL2], nCount: Int): SortedSet[WellConfigL2] = {
		if (nCount <= 0 || wells.isEmpty)
			return SortedSet()
		
		// sort the sources by volume descending (secondary sort key is index order)
		// TRUE means that well1 should be placed before well2
		def compare(well1: WellConfigL2, well2: WellConfigL2): Boolean = {
			val a = well1.obj.state(states)
			val b = well2.obj.state(states)
			if (a.nVolume == b.nVolume)
				well1.index < well2.index
			else
				a.nVolume > b.nVolume
		}
		val order = wells.toSeq.sortWith(compare)
		
		//println("step0: "+nCount+","+wells)
		chooseAdjacentWellsByVolume_Step1(order, nCount)
	}
	
	private def chooseAdjacentWellsByVolume_Step1(order: Seq[WellConfigL2], nCount: Int): SortedSet[WellConfigL2] = {
		val wells = chooseAdjacentWellsByVolume_Step2(order, nCount)
		val wellsAll = {
			if (wells.isEmpty)
				Set()
			else {
				val order1 = order.filter(well => !wells.contains(well))
				val nCount1 = nCount - wells.size
				if (nCount1 > 0 && !order1.isEmpty) {
					wells ++ chooseAdjacentWellsByVolume_Step2(order1, nCount1)
				}
				else
					wells
			}
		}
		//println("step1: "+SortedSet(wellsAll.toSeq : _*))
		SortedSet(wellsAll.toSeq : _*)
	}

	private def chooseAdjacentWellsByVolume_Step2(order: Seq[WellConfigL2], nCount: Int): Set[WellConfigL2] = {
		if (nCount <= 0 || order.isEmpty)
			return Set()

		val well0 = order.head
		if (nCount == 1) {
			//println("step2: "+well0)
			return Set(well0)
		}
		
		val holder0 = well0.holder
		val iCol0 = well0.index / holder0.nRows
		var iRowTop = well0.index % holder0.nRows
		var iRowBot = iRowTop
		def isAboveOrBelow(well: WellConfigL2): Boolean = {
			if (well.holder ne holder0)
				return false
			val iCol = well.index / holder0.nRows
			if (iCol != iCol0)
				return false
			val iRow = well.index % holder0.nRows
			(iRow == iRowTop - 1 || iRow == iRowBot + 1)
		}
		
		val wells = new ArrayBuffer[WellConfigL2]
		wells += well0
		var wellsInCol = order.filter(well => well.ne(well0) && well.iCol == iCol0)
		def step() {
			wellsInCol.find(isAboveOrBelow) match {
				case None =>
				case Some(well) =>
					wells += well
					wellsInCol = wellsInCol.filter(_ ne well)
					iRowTop = math.min(iRowTop, well.index)
					iRowBot = math.max(iRowBot, well.index)
					if (wells.size < nCount)
						step()
			}
		}
		step()
		//println("step2: "+wells.sortBy(_.index))
		SortedSet(wells : _*)
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
	
	/*def getCleanDegreeAspirate(tipState: TipStateL2, liquid: Liquid): WashIntensity.Value = {
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

	}*/

	// REFACTOR: Remove this method -- ellis, 2011-08-30
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

	def choosePreAsperateReplacement(liquidInWell: Liquid, tipState: TipStateL2): Boolean = {
		// If there is no tip, then we'll need to get a new one
		if (tipState.sType_?.isEmpty) {
			true
		}
		else {
			val bInsideOk = tipState.liquid.eq(liquidInWell) || tipState.contamInside.isEmpty
			val bOutsideOk = tipState.destsEntered.filter(_ ne Liquid.empty).isEmpty
			if (!bInsideOk || !bOutsideOk)
				true
			else
				false
		}
	}
	
	def choosePreDispenseReplacement(tipState: TipStateL2): Boolean = {
		assert(tipState.sType_?.isDefined)
		val bOutsideOk = tipState.destsEntered.filter(_ ne Liquid.empty).isEmpty
		if (!bOutsideOk)
			true
		else
			false
	}
	
	def choosePreAsperateWashSpec(tipOverrides: TipHandlingOverrides, washIntensityDefault: WashIntensity.Value, liquidInWell: Liquid, tipState: TipStateL2): Option[WashSpec] = {
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
		
		if (washIntensity == WashIntensity.None)
			return None
			
		val contamInside = tipOverrides.contamInside_? match { case Some(v) => v; case None => tipState.contamInside }
		val contamOutside = tipOverrides.contamOutside_? match { case Some(v) => v; case None => tipState.contamOutside }
		Some(new WashSpec(washIntensity, contamInside, contamOutside))
	}
	
	def choosePreDispenseWashSpec(tipOverrides: TipHandlingOverrides, washIntensityDefault: WashIntensity.Value, liquidInWell: Liquid, tipState: TipStateL2): Option[WashSpec] = {
		val bOutsideOk = tipState.destsEntered.filter(_ ne Liquid.empty).isEmpty
		val washIntensity = tipOverrides.washIntensity_? match {
			case Some(v) => v
			case None =>
				if (bOutsideOk)
					WashIntensity.None
				else
					washIntensityDefault
		}
		
		if (washIntensity == WashIntensity.None)
			return None
			
		val contamInside = tipOverrides.contamInside_? match { case Some(v) => v; case None => tipState.contamInside }
		val contamOutside = tipOverrides.contamOutside_? match { case Some(v) => v; case None => tipState.contamOutside }
		Some(new WashSpec(washIntensity, contamInside, contamOutside))
	}
}
