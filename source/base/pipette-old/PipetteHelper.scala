package roboliq.pipette

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import roboliq.core._


object PipetteHelper {
	def chooseTipWellPairsAll(tips: SortedSet[TipState], dests: SortedSet[Well]): RsResult[Seq[Seq[TipWell]]] = {
		//println("chooseTipWellPairsAll()")
		//println("tips: "+tips)
		//println("dests: "+dests)
		val twss = new ArrayBuffer[Seq[TipWell]]
		var destsRemaining = dests
		var twsPrev = Nil
		while (!destsRemaining.isEmpty) {
			//print("B")
			chooseTipWellPairsNext(tips, destsRemaining, twsPrev) match {
				case RsError(e, w) => return RsError(e, w)
				case RsSuccess(Nil, w) => return RsSuccess(twss, w)
				case RsSuccess(tws, _) =>
					//println("chooseTipWellPairsAll: tws: "+tws+", "+destsRemaining)
					twss += tws
					//println("destsRemaining A:"+destsRemaining)
					destsRemaining --= tws.map(_.well)
					//println("destsRemaining B:"+destsRemaining)
			}
		}
		RsSuccess(twss)
	}
	
	//private def getWellPosList(states: StateMap, wells: Iterable[Well]): RsResult[List[Tuple2[Well, Well]]] = {
		//states.getWellPosList(wells)
	//}

	private def chooseTipWellPairsNext(tips: SortedSet[TipState], wells: SortedSet[Well], twsPrev: Seq[TipWell]): RsResult[Seq[TipWell]] = {
		//print("A")
		//println("chooseTipWellPairsNext()")
		//println("tips: "+tips)
		//println("wells: "+wells)
		if (tips.isEmpty || wells.isEmpty)
			return RsSuccess(Nil)

		for {
			temp1 <- getHolderWellsCol(wells, twsPrev)
			(plate, lWellPosOnHolder, iCol) = temp1
			//lWellPos <- getWellPosList(states, wellsOnHolder)
			well0 <- getFirstWell(plate, lWellPosOnHolder, iCol)
			pos0 = well0
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
				val iColWell = iWell / plate.rows
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
		wells: SortedSet[Well], twsPrev: Seq[TipWell]
	): RsResult[(PlateState, List[Well], Int)] = {
		for {
			lWellPos <- RsSuccess(wells.toList)
		} yield {
			// Pick a "reference" well if twsPrev isn't empty
			val wellRef_? = {
				if (twsPrev.isEmpty)
					None
				else {
					val wellRef = twsPrev.last.well
					if (lWellPos.exists(_.plate == wellRef.plate))
						Some(wellRef)
					else
						None
				}
			}
	
			// Get the holder of interest
			val plate = wellRef_?.map(_.plate).getOrElse(lWellPos.head.plate)
			//println("holder: "+holder)
	
			// Either choose the first column or the column after the reference well
			val iCol = wellRef_?.map(_.iCol + 1).getOrElse(0)
	
			val lWellPosOnHolder = lWellPos.filter(_.plate == plate)
			(plate, lWellPosOnHolder, iCol)
		}
	}

	// Get the upper-most well in iCol.
	// If none found, loop through columns until wells are found
	private def getFirstWell(plateState: PlateState, wellsOnHolder: List[Well], iCol0: Int): RsResult[Well] = {
		//println("getFirstWell()")
		//println("wells: "+wellsOnHolder)
		assert(!wellsOnHolder.isEmpty)

		val nCols = plateState.cols
			
		def checkCol(iCol: Int): Well = {
			wellsOnHolder.find(_.iCol == iCol) match {
				case Some(well) => well
				case None => checkCol((iCol + 1) % nCols)
			}
		}

		RsSuccess(checkCol(iCol0))
	}

	def process[T, T2](items: Iterable[T], acc: Seq[Seq[T2]], fn: Iterable[T] => RsResult[Tuple2[Iterable[T], Seq[T2]]]): RsResult[Seq[Seq[T2]]] = {
		if (items.isEmpty)
			RsSuccess(acc)
		else {
			fn(items) match {
				case RsError(e, w) => RsError(e, w)
				case RsSuccess((itemsRemaining, accNew), w) => 
					if (accNew.isEmpty)
						RsSuccess(Nil, w)
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
	def chooseAdjacentWellsByVolume(wells: Set[Well], nCount: Int): RsResult[SortedSet[Well]] = {
		if (nCount <= 0 || wells.isEmpty)
			return RsSuccess(SortedSet())
		
		// sort the sources by volume descending (secondary sort key is index order)
		// TRUE means that well1 should be placed before well2
		def compare(well1: Well, well2: Well): Boolean = {
			if (well1.vesselState.content.volume == well2.vesselState.content.volume)
				well1.compare(well2) <= 0
			else
				well1.vesselState.content.volume > well2.vesselState.content.volume
		}
		val order = wells.toSeq.sortWith(compare)
		
		//println("step0: "+nCount+","+wells)
		chooseAdjacentWellsByVolume_Step1(order, nCount)
	}
	
	private def chooseAdjacentWellsByVolume_Step1(order: Seq[Well], nCount: Int): RsResult[SortedSet[Well]] = {
		def makeWellsAll(wells: Set[Well]): RsResult[Set[Well]] = {
			if (wells.isEmpty)
				RsSuccess(Set())
			else {
				val order1 = order.filter(well => !wells.contains(well))
				val nCount1 = nCount - wells.size
				if (nCount1 > 0 && !order1.isEmpty) {
					chooseAdjacentWellsByVolume_Step2(order1, nCount1).map(wells ++ _)
				}
				else
					RsSuccess(wells)
			}
		}
		
		for {
			wells <- chooseAdjacentWellsByVolume_Step2(order, nCount)
			wellsAll <- makeWellsAll(wells)
		} yield {
			//println("step1: "+SortedSet(wellsAll.toSeq : _*))
			SortedSet(wellsAll.toSeq : _*)
		}
	}

	private def chooseAdjacentWellsByVolume_Step2(order: Seq[Well], nCount: Int): RsResult[Set[Well]] = {
		if (nCount <= 0 || order.isEmpty)
			return RsSuccess(Set())

		for {
			lOrderPos <- RsSuccess(order.toList)
		} yield {
			val well0 = lOrderPos.head
			val pos0 = well0
			
			if (nCount == 1) {
				//println("step2: "+well0)
				return RsSuccess(Set(well0))
			}
			
			var iRowTop = pos0.iRow
			var iRowBot = iRowTop
			def isAboveOrBelow(pos: Well): Boolean = {
				if (pos.idPlate != pos0.idPlate)
					false
				else if (pos.iCol != pos0.iCol)
					false
				else
					(pos.iRow == iRowTop - 1 || pos.iRow == iRowBot + 1)
			}
			
			val wells = new ArrayBuffer[Well]
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
	
	def chooseTipSrcPairs(tips: SortedSet[TipState], srcs: SortedSet[Well]): RsResult[Seq[Seq[TipWell]]] = {
		def processStep(tips0: Iterable[TipState]): RsResult[Tuple2[Iterable[TipState], Seq[TipWell]]] = {
			val tips = SortedSet[TipState](tips0.toSeq : _*)
			for { tws <- chooseTipWellPairsNext(tips, srcs, Nil) }
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
	
	def choosePreAspirateWashSpec(tipOverrides: TipHandlingOverrides, washIntensityDefault: CleanIntensity.Value, liquidInWell: Liquid, tipState: TipState): Option[WashSpec] = {
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
					CleanIntensity.None
				else
					washIntensityDefault
		}
		//println("chose:", bInsideOk, bOutsideOk1, bOutsideOk2, washIntensity)
		if (washIntensity == CleanIntensity.None)
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
			tipState.destsEntered ++ tipState.srcsEntered,
			tipState
		)
	}
	
	private def chooseWashSpec(tipOverrides: TipHandlingOverrides, liquid0: Liquid, liquids: Iterable[Liquid], tipState: TipState): WashSpec = {
		val intensity = {
			val bDifferentLiquid = liquids.exists(_ ne liquid0)
			val policy = liquid0.tipCleanPolicy
			if (tipState.cleanDegreePrev == CleanIntensity.None) tipOverrides.washIntensity_?.getOrElse(policy.enter)
			else if (tipOverrides.washIntensity_?.isDefined) tipOverrides.washIntensity_?.get
			else if (bDifferentLiquid) CleanIntensity.max(policy.enter, tipState.cleanDegreePending)
			else CleanIntensity.None
		}
		val contamInside = tipOverrides.contamInside_? match { case Some(v) => v; case None => tipState.contamInside }
		val contamOutside = tipOverrides.contamOutside_? match { case Some(v) => v; case None => tipState.contamOutside }
		new WashSpec(intensity, contamInside, contamOutside)
	}
	
	/*
	def choosePreDispenseWashSpec(tipOverrides: TipHandlingOverrides, washIntensityDefault: CleanIntensity.Value, liquidInWell: Liquid, tipState: TipState, pos: PipettePosition.Value): Option[WashSpec] = {
		val bOutsideOk = tipState.destsEntered.forall(liq => liq.eq(Liquid.empty) || liq.eq(liquidInWell)) && tipState.srcsEntered.isEmpty
		val washIntensity = tipOverrides.washIntensity_? match {
			case Some(v) => v
			case None =>
				if (pos == PipettePosition.Free || pos == PipettePosition.DryContact)
					CleanIntensity.None
				else if (tipState.cleanDegreePrev < washIntensityDefault)
					washIntensityDefault
				else if (bOutsideOk)
					CleanIntensity.None
				else
					washIntensityDefault
		}
		
		if (washIntensity == CleanIntensity.None)
			return None
			
		val contamInside = tipOverrides.contamInside_? match { case Some(v) => v; case None => tipState.contamInside }
		val contamOutside = tipOverrides.contamOutside_? match { case Some(v) => v; case None => tipState.contamOutside }
		Some(new WashSpec(washIntensity, contamInside, contamOutside))
	}
	*/
}
