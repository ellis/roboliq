package roboliq.devices.pipette

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import roboliq.core._
import roboliq.commands.pipette._
//import roboliq.robot._


object PipetteHelper {
	def chooseTipWellPairsAll(states: StateMap, tips: SortedSet[Tip], dests: SortedSet[Well]): Result[Seq[Seq[TipWell]]] = {
		//println("chooseTipWellPairsAll()")
		//println("tips: "+tips)
		//println("dests: "+dests)
		val twss = new ArrayBuffer[Seq[TipWell]]
		var destsRemaining = dests
		var twsPrev = Nil
		while (!destsRemaining.isEmpty) {
			chooseTipWellPairsNext(states, tips, destsRemaining, twsPrev) match {
				case Error(ls) => return Error(ls)
				case Success(tws) =>
					twss += tws
					destsRemaining --= tws.map(_.well)
			}
		}
		Success(twss)
	}

	def chooseTipWellPairsNext(states: StateMap, tips: SortedSet[Tip], wells: SortedSet[Well], twsPrev: Seq[TipWell]): Result[Seq[TipWell]] = {
		//println("chooseTipWellPairsNext()")
		//println("tips: "+tips)
		//println("wells: "+wells)
		if (tips.isEmpty || wells.isEmpty)
			return Success(Nil)

		val (idPlate, wellsOnHolder, iCol) = getHolderWellsCol(states, wells, twsPrev)
		val well0 = getFirstWell(states, idPlate, wellsOnHolder, iCol) match {
			case Error(ls) => return Error(ls)
			case Success(well0) => well0
		}
		val plate = states.ob.findPlate(idPlate) match {
			case Error(ls) => return Error(ls)
			case Success(plate) => plate
		}

		val tip0 = tips.head
		val iRowTip0 = tip0.index
		val iColTip0 = 0
		
		val pairs = new ArrayBuffer[TipWell]
		pairs += new TipWell(tip0, well0)
		//println(well0.index+" START")
		for (tip <- tips.tail) {
			val dRowTip = tip.index - tip0.index
			val iWell = well0.index + dRowTip
			val iColWell = iWell / plate.model.nRows
			if (iColWell == well0.iCol) {
				wellsOnHolder.find(_.index == iWell) match {
					case None =>
					case Some(well) => pairs += new TipWell(tip, well)
				}
			}
		}
		Success(pairs.toSeq)
	}

	private def getHolderWellsCol(states: StateMap, wells: SortedSet[Well], twsPrev: Seq[TipWell]): Tuple3[String, SortedSet[Well], Int] = {
		// Pick a "reference" well if twsPrev isn't empty
		val wellRef_? = {
			if (twsPrev.isEmpty)
				None
			else {
				val wellRef = twsPrev.last.well
				val idPlate = wellRef.idPlate
				if (wells.exists(_.idPlate == idPlate))
					Some(wellRef)
				else
					None
			}
		}
		//println("wellRef_?: "+wellRef_?)

		// Get the holder of interest
		val idPlate: String = wellRef_? match {
			case None => wells.head.idPlate
			case Some(wellRef) => wellRef.idPlate
		}
		//println("holder: "+holder)

		// Either choose the first column or the column after the reference well
		val iCol = wellRef_? match {
			case None => 0
			case Some(wellRef) => wellRef.iCol + 1
		}

		val wellsOnHolder = wells.filter(_.idPlate == idPlate)
		(idPlate, wellsOnHolder, iCol)
	}

	// Get the upper-most well in iCol.
	// If none found, loop through columns until wells are found
	private def getFirstWell(states: StateMap, idPlate: String, wellsOnHolder: SortedSet[Well], iCol0: Int): Result[Well] = {
		//println("getFirstWell()")
		//println("wells: "+wellsOnHolder)
		assert(!wellsOnHolder.isEmpty)

		states.ob.findPlate(idPlate) match {
			case Error(ls) =>
				states.ob.findTube(idPlate) match {
					case Error(_) => Error(ls)
					case Success(tube) => Success(tube)
				}
			case Success(plate) =>
				val nCols = plate.model.nCols
					
				def checkCol(iCol: Int): Well = {
					val well_? = wellsOnHolder.find(_.iCol == iCol)
					well_? match {
						case Some(well) => well
						case None => checkCol((iCol + 1) % nCols)
					}
				}
		
				Success(checkCol(iCol0))
		}
	}

	def process[T, T2](items: Iterable[T], acc: Seq[Seq[T2]], fn: Iterable[T] => Result[Tuple2[Iterable[T], Seq[T2]]]): Result[Seq[Seq[T2]]] = {
		if (items.isEmpty)
			Success(acc)
		else {
			fn(items) match {
				case Error(ls) => Error(ls)
				case Success((itemsRemaining, accNew)) => 
					if (accNew.isEmpty)
						Success(Nil)
					else
						process(itemsRemaining, acc ++ Seq(accNew), fn)
			}
		}
	}

	/**
	 * Choose sources (all containing same liquid) according to the following algorithm:
	 * - sort wells by volume (greatest first) and plate/index
	 * - pick the well with the highest volume and put it in our set
	 * - while size of well set < nCount:
	 *   - look at the wells adjacent (and in the same column) to the wells in the well set
	 *   - if there are 0 wells, stop
	 *   - if there is 1 well, add it
	 *   - if there are 2 wells, add the one which occurs first in the sorted order
	 */
	def chooseAdjacentWellsByVolume(states: StateMap, wells: Set[Well], nCount: Int): SortedSet[Well] = {
		if (nCount <= 0 || wells.isEmpty)
			return SortedSet()
		
		// sort the sources by volume descending (secondary sort key is index order)
		// TRUE means that well1 should be placed before well2
		def compare(well1: Well, well2: Well): Boolean = {
			val a = well1.state(states)
			val b = well2.state(states)
			if (a.nVolume == b.nVolume)
				well1.compare(well2) <= 0
			else
				a.nVolume > b.nVolume
		}
		val order = wells.toSeq.sortWith(compare)
		
		//println("step0: "+nCount+","+wells)
		chooseAdjacentWellsByVolume_Step1(order, nCount)
	}
	
	private def chooseAdjacentWellsByVolume_Step1(order: Seq[Well], nCount: Int): SortedSet[Well] = {
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

	private def chooseAdjacentWellsByVolume_Step2(order: Seq[Well], nCount: Int): Set[Well] = {
		if (nCount <= 0 || order.isEmpty)
			return Set()

		val well0 = order.head
		if (nCount == 1) {
			//println("step2: "+well0)
			return Set(well0)
		}
		
		var iRowTop = well0.iRow
		var iRowBot = iRowTop
		def isAboveOrBelow(well: Well): Boolean = {
			if (well.idPlate != well0.idPlate)
				return false
			if (well.iCol != well0.iCol)
				return false
			(well.iRow == iRowTop - 1 || well.iRow == iRowBot + 1)
		}
		
		val wells = new ArrayBuffer[Well]
		wells += well0
		var wellsInCol = order.filter(well => well.ne(well0) && well.iCol == well0.iCol)
		def step() {
			wellsInCol.find(isAboveOrBelow) match {
				case None =>
				case Some(well) =>
					wells += well
					wellsInCol = wellsInCol.filter(_ ne well)
					iRowTop = math.min(iRowTop, well.iRow)
					iRowBot = math.max(iRowBot, well.iRow)
					if (wells.size < nCount)
						step()
			}
		}
		step()
		//println("step2: "+wells.sortBy(_.index))
		SortedSet(wells : _*)
	}
	
	def chooseTipSrcPairs(states: StateMap, tips: SortedSet[Tip], srcs: SortedSet[Well]): Result[Seq[Seq[TipWell]]] = {
		def processStep(tips0: Iterable[Tip]): Result[Tuple2[Iterable[Tip], Seq[TipWell]]] = {
			val tips = SortedSet[Tip](tips0.toSeq : _*)
			for { tws <- chooseTipWellPairsNext(states, tips, srcs, Nil) }
			yield {
				val tipsRemaining = tips -- tws.map(_.tip)
				(tipsRemaining.toSeq, tws)
			}
		}
		process(tips, Nil, processStep)
	}
	
	def splitTipWellPairs(tws: Seq[TipWell]): Seq[Seq[TipWell]] = {
		val map = tws.map(tw => tw.well -> tw).toMap
		val gws: Iterable[WellGroup] = WellGroup(tws.map(_.well)).splitByAdjacent()
		val twss1: Iterable[Seq[TipWell]] = gws.map(_.set.toSeq.map(well => map(well)))
		//val twss2: Iterable[Seq[TipWell]] = twss1.flatMap(splitTipWellPairs2)
		/*println("map: "+map)
		println("gsw:")
		gws.foreach(println)
		println("twss1: "+twss1)
		println("twss2: "+twss2)*/
		twss1.toSeq
	}
	
	/*
	private def splitTipWellPairs2(tws: Seq[TipWell]): Seq[Seq[TipWell]] = {
		if (tws.isEmpty)
			return Seq()

		val twss = new ArrayBuffer[Seq[TipWell]]
		var rest = tws
		while (!rest.isEmpty) {
			val tw0 = tws.head
			var iTip = tw0.tip.index
			var iWell = tw0.well.index
			val keep = tws.takeWhile(tw => {
				val b = (tw.tip.index == iTip && tw.well.index == iWell)
				iTip += 1
				iWell += 1
				b
			})
			twss += keep
			rest = rest.drop(keep.size)
		}
		twss.toSeq
	}*/
	
	/*
	def choosePreAspirateReplacement(liquidInWell: Liquid, tipState: TipState): Boolean = {
		// If there is no tip, then we'll need to get a new one
		if (tipState.sType_?.isEmpty) {
			true
		}
		else {
			val bInsideOk = tipState.liquid.eq(liquidInWell) || tipState.contamInside.isEmpty
			val bOutsideOk1 = tipState.destsEntered.forall(liquid => liquid.eq(Liquid.empty) || liquid.eq(liquidInWell))
			val bOutsideOk2 = tipState.srcsEntered.forall(liquid => liquid.eq(Liquid.empty) || liquid.eq(liquidInWell))
			val bOutsideOk = bOutsideOk1 && bOutsideOk2
			if (!bInsideOk || !bOutsideOk)
				true
			else
				false
		}
	}
	
	def choosePreDispenseReplacement(tipState: TipState, liquidInWell: Liquid): Boolean = {
		assert(tipState.sType_?.isDefined)
		val bOutsideOk = tipState.destsEntered.forall(liq => liq.eq(Liquid.empty) || liq.eq(liquidInWell)) && tipState.srcsEntered.isEmpty
		if (!bOutsideOk)
			true
		else
			false
	}
	
	def choosePreAspirateWashSpec(tipOverrides: TipHandlingOverrides, washIntensityDefault: WashIntensity.Value, liquidInWell: Liquid, tipState: TipState): Option[WashSpec] = {
		val washIntensity = tipOverrides.washIntensity_? match {
			case Some(v) => v
			case None =>
				val bInsideOk = tipState.liquid.eq(liquidInWell) || tipState.contamInside.isEmpty
				val bOutsideOk1 = tipState.destsEntered.forall(liquid => liquid.eq(Liquid.empty) || liquid.eq(liquidInWell))
				val bOutsideOk2 = tipState.srcsEntered.forall(liquid => liquid.eq(Liquid.empty) || liquid.eq(liquidInWell))
				val bOutsideOk = bOutsideOk1 && bOutsideOk2
				if (tipState.cleanDegreePrev < washIntensityDefault)
					washIntensityDefault
				else if (bInsideOk && bOutsideOk)
					WashIntensity.None
				else
					washIntensityDefault
		}
		//println("chose:", bInsideOk, bOutsideOk1, bOutsideOk2, washIntensity)
		if (washIntensity == WashIntensity.None)
			return None
			
		val contamInside = tipOverrides.contamInside_? match { case Some(v) => v; case None => tipState.contamInside }
		val contamOutside = tipOverrides.contamOutside_? match { case Some(v) => v; case None => tipState.contamOutside }
		Some(new WashSpec(washIntensity, contamInside, contamOutside))
	}
	*/
	
	def choosePreAspirateWashSpec(tipOverrides: TipHandlingOverrides, liquidSrc: Liquid, tipState: TipState): WashSpec = {
		chooseWashSpec(
			tipOverrides,
			liquidSrc,
			tipState.destsEntered ++ tipState.srcsEntered,
			tipState
		)
	}
	
	def choosePreDispenseWashSpec(tipOverrides: TipHandlingOverrides, liquidSrc: Liquid, liquidDest: Liquid, tipState: TipState): WashSpec = {
		chooseWashSpec(
			tipOverrides,
			liquidDest,
			tipState.destsEntered ++ tipState.srcsEntered.filter(_.group ne liquidSrc.group),
			tipState
		)
	}
	
	private def chooseWashSpec(tipOverrides: TipHandlingOverrides, liquid0: Liquid, liquids: Iterable[Liquid], tipState: TipState): WashSpec = {
		val group = liquid0.group
		
		val intensity = {
			var bDifferentLiquid = false
			var bDifferentGroup = false
			// Check previously entered liquids
			for (liquid <- liquids) {
				bDifferentGroup |= (liquid.group ne group)
				bDifferentLiquid |= (liquid ne liquid0)
			}
			
			val policy = group.cleanPolicy
			if (tipState.cleanDegreePrev == WashIntensity.None) tipOverrides.washIntensity_?.getOrElse(policy.enter)
			else if (tipOverrides.washIntensity_?.isDefined) tipOverrides.washIntensity_?.get
			else if (bDifferentGroup) WashIntensity.max(policy.enter, tipState.cleanDegreePending)
			else if (bDifferentLiquid) policy.within
			else WashIntensity.None
		}
		val contamInside = tipOverrides.contamInside_? match { case Some(v) => v; case None => tipState.contamInside }
		val contamOutside = tipOverrides.contamOutside_? match { case Some(v) => v; case None => tipState.contamOutside }
		new WashSpec(intensity, contamInside, contamOutside)
	}
	
	/*
	def choosePreDispenseWashSpec(tipOverrides: TipHandlingOverrides, washIntensityDefault: WashIntensity.Value, liquidInWell: Liquid, tipState: TipState, pos: PipettePosition.Value): Option[WashSpec] = {
		val bOutsideOk = tipState.destsEntered.forall(liq => liq.eq(Liquid.empty) || liq.eq(liquidInWell)) && tipState.srcsEntered.isEmpty
		val washIntensity = tipOverrides.washIntensity_? match {
			case Some(v) => v
			case None =>
				if (pos == PipettePosition.Free || pos == PipettePosition.DryContact)
					WashIntensity.None
				else if (tipState.cleanDegreePrev < washIntensityDefault)
					washIntensityDefault
				else if (bOutsideOk)
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
	*/
}
