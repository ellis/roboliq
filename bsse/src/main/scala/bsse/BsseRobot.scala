package bsse

import roboliq.parts._
import roboliq.robot._
import roboliq.tokens._

import evoware._


//sealed class BsseTip(val roboTip: Tip)

class BsseRobot(evowareState: EvowareSetupState) extends EvowareRobot(evowareState) {
	val nFreeDispenseVolumeThreshold = 10
	val tipKind1000 = new EvowareTipKind("large", 2, 950, 50)
	val tipKind50 = new EvowareTipKind("small", 0.01, 45, 5)
	val tips = (0 until 8).map(new Tip(_))
	val tipTipKinds = (0 until 8).map(iTip => if (iTip < 4) tipKind1000 else tipKind50)
	val plateDecon = new Plate(3, 1)
	val config = new RobotConfig(tips.toArray, Array(Array(0,1,2,3), Array(4,5,6,7), Array(0,1,2,3,4,5,6,7)))
	
	def getTipKind(tip: Tip): EvowareTipKind = tipTipKinds(tip.index)
	
	def getTipAspirateVolumeMin(tip: Tip, liquid: Liquid): Double = {
		val tipKind = getTipKind(tip)
		tipKind.nAspirateVolumeMin
	}
	
	def getTipHoldVolumeMax(tip: Tip, liquid: Liquid): Double = {
		val tipKind = getTipKind(tip)
		val nReduce =
			if (liquid.contaminates)
				tipKind.nWashVolumeExtra
			else
				0
		tipKind.nHoldVolumeMax - nReduce
	}
	
	def getDispenseKind(tip: Tip, liquid: Liquid, nVolume: Double, wellState: WellState): DispenseKind.Value = {
		// If our volume is high enough that we don't need to worry about accuracy,
		// or if we're pipetting competent cells,
		// then perform a free dispense.
		if (nVolume >= nFreeDispenseVolumeThreshold || liquid.bCells)
			DispenseKind.Free
		else if (wellState.nVolume == 0)
			DispenseKind.DryContact
		else
			DispenseKind.WetContact
	}

	def chooseTipWellPairs(tips: Seq[Tip], wells: Seq[Well], wellPrev_? : Option[Well]): Seq[Seq[Tuple2[Tip, Well]]] = {
		if (tips.isEmpty || wells.isEmpty)
			return Nil

		val (holder, wellsOnHolder, iCol) = getHolderWellsCol(wells, wellPrev_?)
		val wellsInCol = getWellsInCol(holder, wellsOnHolder, iCol, tips.size)
		tips zip WellsInCol
	}

	private def getHolderWellsCol(wells: Seq[Well], wellPrev_? : Option[Well]): Tuple3[Holder, Seq[Well], Int] = {
		// If the previous well is defined but is on a different holder, then ignore it
		val wellPrevValidated_? = wellPrev_? match {
			case None => None
			case Some(wellPrev) =>
				if (wells.exists(_.holder == wellPrev.holder))
					wellPrev_?
				else
					None
		}

		// Choose first well
		val well0 = wellPrevValidated_? match {
			case Some(well) => well
			case None => wellsAvailable.head
		}
		val holder = well0.holder
		val iCol = wellPrevValidated_? match {
			case Some(well) => well.index / holder.nRows + 1
			case None => 0
		}
		val wellsOnHolder = wellsAvailable.filter(_.holder == holder)
		(holder, wellsOnHolder, iCol)
	}

	// Pick the top-most destination wells available in the given column
	// If none found, loop through columns until wells are found
	private def getWellsInCol(holder: WellHolder, wellsOnHolder: Seq[Well], iCol0: Int, nWellsMax: Int): Seq[Well] = {
		val nRows = holder.nRows
		val nCols = holder.nCols
		var iCol = iCol0
		var wellsInCol: Seq[Well] = null
		do {
			wellsInCol = wellsOnHolder.filter(_.index / nRows == iCol).take(nWellsMax)
			if (wellsInCol.isEmpty) {
				iCol = (iCol + 1) % nCols
				assert(iCol != iCol0)
			}
		} while (wellsInCol.isEmpty)
		wellsInCol
	}

	def getAspirateClass(tip: Tip, well: Well): Option[String] = {
		val tipKind = getTipKind(tip)
		val wellState = state.getWellState(well)
		val liquid = wellState.liquid
		// Can't aspirate from an empty well
		assert(liquid ne Liquid.empty)

		val bLarge = (tipKind.sName == "large")
		//val tipState = state.getTipState(tip)
		//val tipLiquid = tipState.liquid
		// If our volume is high enough that we don't need to worry about accuracy,
		// or if we're pipetting competent cells,
		// then perform a free dispense.
		if (liquid.bCells)
			if (bLarge) Some("Comp cells free dispense") else None
		else if (liquid.sName.contains("DMSO"))
			if (bLarge) Some("DMSO free dispense") else None
		else if (liquid.sName.contains("D-BSSE Decon"))
			Some("D-BSSE Decon")
		else
			if (bLarge) Some("Water wet contact") else Some("D-BSSE Te-PS Wet Contact")
	}
	
	def getDispenseClass(tip: Tip, well: Well, nVolume: Double): Option[String] = {
		val tipKind = getTipKind(tip)
		val wellState = state.getWellState(well)
		val liquid = wellState.liquid
		
		val bLarge = (tipKind.sName == "large")
		//val tipState = state.getTipState(tip)
		//val tipLiquid = tipState.liquid
		// If our volume is high enough that we don't need to worry about accuracy,
		// or if we're pipetting competent cells,
		// then perform a free dispense.
		if (liquid.bCells)
			if (bLarge) Some("Comp cells free dispense") else None
		else if (liquid.sName.contains("DMSO"))
			if (bLarge) Some("DMSO free dispense") else None
		else if (liquid.sName.contains("D-BSSE Decon"))
			Some("D-BSSE Decon")
		// If our volume is high enough that we don't need to worry about accuracy
		else if (nVolume >= nFreeDispenseVolumeThreshold)
			if (bLarge) Some("Water free dispense") else None
		else if (wellState.nVolume == 0)
			if (bLarge) Some("Water dry contact") else Some("D-BSSE Te-PS Dry Contact")
		else
			if (bLarge) Some("Water wet contact") else Some("D-BSSE Te-PS Wet Contact")
	}
	
	def batchesForAspirate(twvs: Seq[TipWellVolume]): Seq[Seq[TipWellVolume]] = {
		def getLiquidClass(twv: TipWellVolume) = getAspirateClass(twv.tip, twv.well)
		// Group by tip type and liquid dispense class
		def canBatch(twv0: TipWellVolume, twv1: TipWellVolume, getLiquidClass: (TipWellVolume => Option[String])): Boolean = {
			val tipKind0 = getTipKind(twv0.tip)
			val tipKind1 = getTipKind(twv1.tip)
			if (tipKind0 ne tipKind1) {
				false
			}
			else {
				val sClass0 = getLiquidClass(twv0).get
				val sClass1 = getLiquidClass(twv1).get
				sClass0 == sClass1
			}
		}

		val bAllHaveClass = twvs.forall(twv => getLiquidClass(twv).isDefined)
		if (!bAllHaveClass) {
			Nil
		}
		else {
			def matcher(twv0: TipWellVolume, twv1: TipWellVolume) = canBatch(twv0, twv1, getLiquidClass)
			partitionBy(twvs.toList, matcher)
		}
	}
	
	def batchesForDispense(twvs: Seq[TipWellVolumeDispense]): Seq[Seq[TipWellVolumeDispense]] = {
		def getLiquidClass(twv: TipWellVolume) = getDispenseClass(twv.tip, twv.well, twv.nVolume)
		// Group by tip type and liquid dispense class
		def canBatch(twv0: TipWellVolume, twv1: TipWellVolume, getLiquidClass: (TipWellVolume => Option[String])): Boolean = {
			val tipKind0 = getTipKind(twv0.tip)
			val tipKind1 = getTipKind(twv1.tip)
			if (tipKind0 ne tipKind1) {
				false
			}
			else {
				val sClass0 = getLiquidClass(twv0).get
				val sClass1 = getLiquidClass(twv1).get
				sClass0 == sClass1
			}
		}

		val bAllHaveClass = twvs.forall(twv => getLiquidClass(twv).isDefined)
		if (!bAllHaveClass) {
			Nil
		}
		else {
			def matcher(twv0: TipWellVolume, twv1: TipWellVolume) = canBatch(twv0, twv1, getLiquidClass)
			partitionBy(twvs.toList, matcher)
		}
	}

	/*private def batches(twvs: Seq[TipWellVolume], getLiquidClass: (TipWellVolume => Option[String])): Seq[Seq[TipWellVolume]] = {
		// Group by tip type and liquid dispense class
		def canBatch(twv0: TipWellVolume, twv1: TipWellVolume, getLiquidClass: (TipWellVolume => Option[String])): Boolean = {
			val tipKind0 = getTipKind(twv0.tip)
			val tipKind1 = getTipKind(twv1.tip)
			if (tipKind0 ne tipKind1) {
				false
			}
			else {
				val sClass0 = getLiquidClass(twv0).get
				val sClass1 = getLiquidClass(twv1).get
				sClass0 == sClass1
			}
		}
		
		val bAllHaveClass = twvs.forall(twv => getLiquidClass(twv).isDefined)
		if (!bAllHaveClass) {
			Nil
		}
		else {
			def matcher(twv0: TipWellVolume, twv1: TipWellVolume) = canBatch(twv0, twv1, getLiquidClass)
			partitionBy(twvs.toList, matcher)
		}
	}*/

	private def partitionBy[T](list: List[T], fn: (T, T) => Boolean): List[List[T]] = {
		list match {
			case Nil => Nil
			case a :: rest =>
				val (as, bs) = rest.partition(x => fn(a, x))
				(a :: as) :: partitionBy(bs, fn)
		}
	}

	private def partitionSeqBy[T](list: Seq[T], fn: (T, T) => Boolean): Seq[Seq[T]] = {
		list match {
			case Seq() => Nil
			case Seq(a, rest @ _*) =>
				val (as, bs) = rest.partition(x => fn(a, x))
				Seq(a) ++ as ++ partitionSeqBy(bs, fn)
		}
	}

	private def sameTipKind(a: Tip, b: Tip): Boolean = getTipKind(a) eq getTipKind(b)
}
