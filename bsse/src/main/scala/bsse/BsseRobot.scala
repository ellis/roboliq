package bsse

import scala.collection.immutable.SortedSet
import scala.collection.mutable.ArrayBuffer

import roboliq.parts._
import roboliq.robot._
import roboliq.tokens._

import evoware._


//sealed class BsseTip(val roboTip: Tip)

class BsseRobot extends EvowareRobot {
	val nFreeDispenseVolumeThreshold = 10
	
	// Tips
	val tipKind1000 = new EvowareTipKind("large", 2, 950, 50)
	val tipKind50 = new EvowareTipKind("small", 0.01, 45, 5)
	val tips = SortedSet[Tip]() ++ (0 until 8).map(new Tip(_))
	val tipTipKinds = (0 until 8).map(iTip => if (iTip < 4) tipKind1000 else tipKind50)

	// Fixed plates
	val plateDecon1 = new Plate(8, 1)
	val plateDecon2 = new Plate(8, 1)
	val plateDecon3 = new Plate(8, 1)
	
	// Standard liquids
	val liquidWater = new Liquid(
		sName = "water",
		bWaterFreeDispense = true,
		bRequireDecontamBeforeAspirate = false,
		bCells = false,
		bDna = false,
		bOtherContaminant = false)
	
	/*val plateWash1a = new Plate(8, 1)
	val plateWash1b = new Plate(8, 1)
	val plateWash1c = new Plate(8, 1)
	val plateWash2a = new Plate(8, 1)
	val plateWash2b = new Plate(8, 1)
	val plateWash2c = new Plate(8, 1)*/
	val config = new RobotConfig(tips, Array(Array(0,1,2,3), Array(4,5,6,7), Array(0,1,2,3,4,5,6,7)))
	
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

	def chooseTipWellPairs(tips: SortedSet[Tip], wells: SortedSet[Well], wellPrev_? : Option[Well]): Seq[Tuple2[Tip, Well]] = {
		if (tips.isEmpty || wells.isEmpty)
			return Nil

		val (holder, wellsOnHolder, iCol) = getHolderWellsCol(wells, wellPrev_?)
		val wellsInCol = getWellsInCol(holder, wellsOnHolder, iCol, tips.size)
		tips.toSeq zip wellsInCol.toSeq
	}

	private def getHolderWellsCol(wells: SortedSet[Well], wellPrev_? : Option[Well]): Tuple3[WellHolder, SortedSet[Well], Int] = {
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
			case None => wells.head
		}
		val holder = well0.holder
		val iCol = wellPrevValidated_? match {
			case Some(well) => well.index / holder.nRows + 1
			case None => 0
		}
		val wellsOnHolder = wells.filter(_.holder == holder)
		(holder, wellsOnHolder, iCol)
	}

	// Pick the top-most destination wells available in the given column
	// If none found, loop through columns until wells are found
	private def getWellsInCol(holder: WellHolder, wellsOnHolder: SortedSet[Well], iCol0: Int, nWellsMax: Int): SortedSet[Well] = {
		val nRows = holder.nRows
		val nCols = holder.nCols
		var iCol = iCol0
		var wellsInCol: SortedSet[Well] = null
		do {
			wellsInCol = wellsOnHolder.filter(_.index / nRows == iCol).take(nWellsMax)
			if (wellsInCol.isEmpty) {
				iCol = (iCol + 1) % nCols
				assert(iCol != iCol0)
			}
		} while (wellsInCol.isEmpty)
		wellsInCol
	}

	def getAspirateClass(tipState: TipState, wellState: WellState): Option[String] = {
		val tipKind = getTipKind(tipState.tip)
		val liquid = wellState.liquid
		// Can't aspirate from an empty well
		assert(liquid ne Liquid.empty)

		val bLarge = (tipKind.sName == "large")
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
	
	def getDispenseClass(tipState: TipState, wellState: WellState, nVolume: Double): Option[String] = {
		val tipKind = getTipKind(tipState.tip)
		val liquid = tipState.liquid
		
		val bLarge = (tipKind.sName == "large")
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

	def batchesForAspirate(state: IRobotState, twvs: Seq[TipWellVolume]): Seq[Seq[TipWellVolume]] = {
		def getLiquidClass(twv: TipWellVolume) = getAspirateClass(state, twv)
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
	
	def batchesForDispense(state: IRobotState, twvs: Seq[TipWellVolumeDispense]): Seq[Seq[TipWellVolumeDispense]] = {
		def getLiquidClass(twv: TipWellVolume) = getDispenseClass(state, twv)

		val bAllHaveClass = twvs.forall(twv => getLiquidClass(twv).isDefined)
		if (!bAllHaveClass) {
			Nil
		}
		else {
			def matcher(twv0: TipWellVolume, twv1: TipWellVolume) = canBatch(twv0, twv1, getLiquidClass)
			partitionBy(twvs.toList, matcher)
		}
	}

	def batchesForClean(tcs: Seq[Tuple2[Tip, CleanDegree.Value]]): Seq[T1_Clean] = {
		val cs = new ArrayBuffer[T1_Clean]
		val tcss = tcs.groupBy(pair => getTipKind(pair._1))
		for ((_, tcs) <- tcss if !tcs.isEmpty) {
			val tips = tcs.map(_._1)
			val cleanDegree = tcs.foldLeft(CleanDegree.None)((cd, pair) => {
				if (pair._2 > cd) pair._2 else cd
			})
			cs += new T1_Clean(tips, cleanDegree)
		}
		cs.toSeq
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
				Seq(Seq(a) ++ as) ++ partitionSeqBy(bs, fn)
		}
	}

	private def sameTipKind(a: Tip, b: Tip): Boolean = getTipKind(a) eq getTipKind(b)
}

object BsseRobot {
	def createRobotMockup(): Tuple2[BsseRobot, RobotState] = {
		val carrierGrids = Array(
				// Wash 1
				1,
				// Wash 2
				2,
				// Cooled carrier
				3,
				// Trough
				4,
				// Downholder
				9,
				// cover holder and shaker
				10,
				// Reagents 4x5
				16,
				// Two 96 well plates
				17,
				// Three plates?
				24
				//...
				)
		val carrierGridPairs = carrierGrids.map(new Carrier() -> _)
		val mapGridToCarrier = carrierGridPairs.map(pair => pair._2 -> pair._1).toMap

		val robot = new BsseRobot

		val carrierWash1 = mapGridToCarrier(1)
		val carrierWash2 = mapGridToCarrier(2)
		val carrierDecon = mapGridToCarrier(3)
		
		val builder = new RobotStateBuilder(RobotState.empty)
		for ((carrier, iGrid) <- carrierGridPairs) {
			builder.movePartTo(carrier, robot.partTop, iGrid)
		}
		val list = List(
			(robot.plateDecon1, carrierDecon, 0),
			(robot.plateDecon2, carrierDecon, 1),
			(robot.plateDecon3, carrierDecon, 2)
		)
		for ((part, parent, i) <- list) {
			builder.movePartTo(part, parent, i)
		}
		
		(robot, builder.toImmutable)
	}
}
