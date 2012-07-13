package roboliq.devices.pipette

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import roboliq.core._
import roboliq.commands.pipette._
//import roboliq.robot._


object PipetteHelper {
	def chooseTipWellPairsAll(states: StateMap, tips: SortedSet[Tip], dests: SortedSet[Well2]): Result[Seq[Seq[TipWell]]] = {
		//println("chooseTipWellPairsAll()")
		//println("tips: "+tips)
		//println("dests: "+dests)
		val twss = new ArrayBuffer[Seq[TipWell]]
		var destsRemaining = dests
		var twsPrev = Nil
		while (!destsRemaining.isEmpty) {
			//print("B")
			chooseTipWellPairsNext(states, tips, destsRemaining, twsPrev) match {
				case Error(ls) => return Error(ls)
				case Success(Nil) => return Success(twss)
				case Success(tws) =>
					//println("chooseTipWellPairsAll: tws: "+tws+", "+destsRemaining)
					twss += tws
					//println("destsRemaining A:"+destsRemaining)
					destsRemaining --= tws.map(_.well)
					//println("destsRemaining B:"+destsRemaining)
			}
		}
		Success(twss)
	}
	
	//private def getWellPosList(states: StateMap, wells: Iterable[Well2]): Result[List[Tuple2[Well2, Well2]]] = {
		//states.getWellPosList(wells)
	//}

	private def chooseTipWellPairsNext(states: StateMap, tips: SortedSet[Tip], wells: SortedSet[Well2], twsPrev: Seq[TipWell]): Result[Seq[TipWell]] = {
		//print("A")
		//println("chooseTipWellPairsNext()")
		//println("tips: "+tips)
		//println("wells: "+wells)
		if (tips.isEmpty || wells.isEmpty)
			return Success(Nil)

		for {
			temp1 <- getHolderWellsCol(states, wells, twsPrev)
			(idPlate, lWellPosOnHolder, iCol) = temp1
			//lWellPos <- getWellPosList(states, wellsOnHolder)
			well0 <- getFirstWell(states, idPlate, lWellPosOnHolder, iCol)
			plate <- states.findPlate(idPlate)
			pos0 <- states.findWellPosition(well0.id)
		} yield {
			val tip0 = tips.head
			val iRowTip0 = tip0.index
			val iColTip0 = 0
			
			val pairs = new ArrayBuffer[TipWell]
			pairs += new TipWell(tip0, well0)
			//println(pos0.index+" START")
			for (tip <- tips.tail) {
				val dRowTip = tip.index - tip0.index
				val iWell = pos0.index + dRowTip
				val iColWell = iWell / plate.model.nRows
				if (iColWell == pos0.iCol) {
					lWellPosOnHolder.find(_.index == iWell) match {
						case None =>
						case Some(well) => pairs += new TipWell(tip, well)
					}
				}
			}
			//println("pair: "+pairs)
			pairs.toSeq
		}
	}

	private def getHolderWellsCol(
		states: StateMap, wells: SortedSet[Well2], twsPrev: Seq[TipWell]
	): Result[
		Tuple3[
			String, 
			List[Well2], 
			Int
		]
	] = {
		for {
			lWellPos <- Success(wells.toList)
		} yield {
			// Pick a "reference" well if twsPrev isn't empty
			val wellRef_? = {
				if (twsPrev.isEmpty)
					None
				else {
					val wellRef = twsPrev.last.well
					states.findWellPosition(wellRef.id) match {
						case Error(ls) => return Error(ls)
						case Success(posRef) =>
							if (lWellPos.exists(_.idPlate == posRef.idPlate))
								Some(wellRef)
							else
								None
					}
				}
			}
			//println("wellRef_?: "+wellRef_?)
			val posRef_? = wellRef_? match {
				case None => None
				case Some(wellRef) => states.findWellPosition(wellRef.id).toOption
			}
	
			// Get the holder of interest
			val idPlate: String = posRef_? match {
				case None => lWellPos.head.idPlate
				case Some(posRef) => posRef.idPlate
			}
			//println("holder: "+holder)
	
			// Either choose the first column or the column after the reference well
			val iCol = posRef_? match {
				case None => 0
				case Some(posRef) => posRef.iCol + 1
			}
	
			val lWellPosOnHolder = lWellPos.filter(_.idPlate == idPlate)
			(idPlate, lWellPosOnHolder, iCol)
		}
	}

	// Get the upper-most well in iCol.
	// If none found, loop through columns until wells are found
	private def getFirstWell(states: StateMap, idPlate: String, wellsOnHolder: List[Well2], iCol0: Int): Result[Well2] = {
		//println("getFirstWell()")
		//println("wells: "+wellsOnHolder)
		assert(!wellsOnHolder.isEmpty)

		states.findPlate(idPlate) match {
			case Error(ls) => Error(ls)
			case Success(plate) =>
				val nCols = plate.model.nCols
					
				def checkCol(iCol: Int): Well2 = {
					wellsOnHolder.find(_.iCol == iCol) match {
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
	def chooseAdjacentWellsByVolume(states: StateMap, wells: Set[Well2], nCount: Int): Result[SortedSet[Well2]] = {
		if (nCount <= 0 || wells.isEmpty)
			return Success(SortedSet())
		
		// sort the sources by volume descending (secondary sort key is index order)
		// TRUE means that well1 should be placed before well2
		def compare(well1: Well2, well2: Well2): Boolean = {
			(for {
				st1 <- states.findWellState(well1.id)
				st2 <- states.findWellState(well2.id)
			} yield {
				if (st1.nVolume == st2.nVolume)
					well1.compare(well2) <= 0
				else
					st1.nVolume > st2.nVolume
			}) getOrElse (well1.compare(well2) <= 0)
		}
		val order = wells.toSeq.sortWith(compare)
		
		//println("step0: "+nCount+","+wells)
		chooseAdjacentWellsByVolume_Step1(states, order, nCount)
	}
	
	private def chooseAdjacentWellsByVolume_Step1(states: StateMap, order: Seq[Well2], nCount: Int): Result[SortedSet[Well2]] = {
		def makeWellsAll(wells: Set[Well2]): Result[Set[Well2]] = {
			if (wells.isEmpty)
				Success(Set())
			else {
				val order1 = order.filter(well => !wells.contains(well))
				val nCount1 = nCount - wells.size
				if (nCount1 > 0 && !order1.isEmpty) {
					chooseAdjacentWellsByVolume_Step2(states, order1, nCount1).map(wells ++ _)
				}
				else
					Success(wells)
			}
		}
		
		for {
			wells <- chooseAdjacentWellsByVolume_Step2(states, order, nCount)
			wellsAll <- makeWellsAll(wells)
		} yield {
			//println("step1: "+SortedSet(wellsAll.toSeq : _*))
			SortedSet(wellsAll.toSeq : _*)
		}
	}

	private def chooseAdjacentWellsByVolume_Step2(states: StateMap, order: Seq[Well2], nCount: Int): Result[Set[Well2]] = {
		if (nCount <= 0 || order.isEmpty)
			return Success(Set())

		for {
			lOrderPos <- Success(order.toList)
		} yield {
			val well0 = lOrderPos.head
			val pos0 = well0
			
			if (nCount == 1) {
				//println("step2: "+well0)
				return Success(Set(well0))
			}
			
			var iRowTop = pos0.iRow
			var iRowBot = iRowTop
			def isAboveOrBelow(pos: Well2): Boolean = {
				if (pos.idPlate != pos0.idPlate)
					false
				else if (pos.iCol != pos0.iCol)
					false
				else
					(pos.iRow == iRowTop - 1 || pos.iRow == iRowBot + 1)
			}
			
			val wells = new ArrayBuffer[Well2]
			wells += well0
			var wellsInCol = lOrderPos.filter(well => {
				well.ne(well0) && well.iCol == pos0.iCol
			})
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
	}
	
	def chooseTipSrcPairs(states: StateMap, tips: SortedSet[Tip], srcs: SortedSet[Well2]): Result[Seq[Seq[TipWell]]] = {
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
	
	def splitTipWellPairs(states: StateMap, tws: Seq[TipWell]): Seq[Seq[TipWell]] = {
		val map = tws.map(tw => tw.well -> tw).toMap
		val gws: Iterable[WellGroup] = WellGroup(states, tws.map(_.well)).splitByAdjacent()
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
