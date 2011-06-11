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
	
	def getTipKind(tip: Tip): EvowareTipKind = tipTipKinds(tip.index)
	
	def getTipAspirateVolumeMin(tip: Tip, liquid: Liquid): Double = {
		val tipKind = getTipKind(tip)
		tipKind.nAspirateVolumeMin
	}
	
	def getTipHoldVolumeMax(tip: Tip, liquid: Liquid): Double = {
		val tipKind = getTipKind(tip)
		val nReduce =
			if (liquid.contaminates)
				tipKind.nDecontaminateVolumeExtra
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
	
	def getAspirateClass(tip: Tip, well: Well): Option[String] = {
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
			if (bLarge) Some("Comp cells free dispense") else None
		else if (wellState.nVolume == 0)
			if (bLarge) Some("Water dry contact") else Some("D-BSSE Te-PS Dry Contact")
		else
			if (bLarge) Some("Water wet contact") else Some("D-BSSE Te-PS Wet Contact")
	}
	
	def batchesForAsperate(twvs: Seq[TipWellVolume]): Seq[Seq[TipWellVolume]] = {
		def getLiquidClass(twv: TipWellVolume) = getAspirateClass(twv.tip, twv.well)
		batches(twvs, getLiquidClass)
	}
	
	def batchesForDispense(twvs: Seq[TipWellVolume]): Seq[Seq[TipWellVolume]] = {
		def getLiquidClass(twv: TipWellVolume) = getDispenseClass(twv.tip, twv.well, twv.nVolume)
		batches(twvs, getLiquidClass)
	}

	private def batches(twvs: Seq[TipWellVolume], getLiquidClass: (TipWellVolume => Option[String])): Seq[Seq[TipWellVolume]] = {
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

	private def partitionBy[T](list: List[T], fn: (T, T) => Boolean): List[List[T]] = {
		list match {
			case Nil => Nil
			case a :: rest =>
				val (as, bs) = rest.partition(x => fn(a, x))
				(a :: as) :: partitionBy(bs, fn)
		}
	}
	
	private def sameTipKind(a: Tip, b: Tip): Boolean = getTipKind(a) eq getTipKind(b)
	
	// REFACTOR: This really doesn't belong here: put it in BsseTranslator -- ellis, 2011-06-11
	def clean(degreeMin: CleanDegree.Value): List[T0_Token] = {
		// Partition tips into groups according to tip kind
		val tipss = partitionBy(tips, sameTipKind)
		// determine cleaning degree for each group based upon type of contamination
		// create appropriate wash/spirate/dispose instructions for the given cleaning degree
		//  determine volumes and wells
		def sameTipKind(a: TipCleanSpec, b: TipCleanInfo): Boolean = {
			val ka = robot.getTipKind(a.tip)
			val kb = robot.getTipKind(b.tip)
			ka == kb
		}
		val specss = partitionBy(specs0.toList, sameTipKind)
		for (specs <- specss) yield {
			// Indexes of tips we want to use
			val aiTips = specs.map(_.tip.index)
			// Create mask for all tips being used
			val mTips = aiTips.foldLeft(0) { (sum, i) => sum + (1 << i) }
			
			Array(
				mTips,
				iWasteGrid, iWasteSite,
				iCleanerGrid, iCleanerSite,
				'"'+sWasteVolume+'"',
				nWasteDelay,
				'"'+sCleanerVolume+'"',
				nCleanerDelay,
				nAirgapVolume,
				nAirgapSpeed,
				nRetractSpeed,
				(if (bFastWash) 1 else 0),
				0,1000,0).mkString("Wash(", ",", ")")
		}.toList
	}
	
	private def cleanGroup(tips: Seq[Tip], degreeMin: CleanDegree.Value): List[T0_Token] = {
		val tipStates = tips.map(tip => state.getTipState(tip))
		// Calculate an overall tip state for maximum contamination
		val tipStateAcc = tipState.foldRight(TipState(tips.head)) { (tipState, acc) =>
				acc.aspirate(tipState.liquid, tipState.nContamInsideVolume)
				   .dispense(tipState.nContamInsideVolume)
		}
		val bRequireDecontam = tipStateAcc.contamInside.contaminated || tipStateAcc.contamOutside.contaminated
		val degree = {
			if (bRequireDecontam) CleanDegree.Decontaminate
			else degreeMin
		}
		
		degreeMin match {
			case None => Nil
			case Light => cleanGroupLight(tips)
			case Thorough => cleanGroupLight(tips)
			case Decontaminate => cleanGroupLight(tips)
		}
	}
	
	private def cleanGroupLight(tips: Seq[Tip], nContamInsideVolume: Double): List[T0_Token] = {
		val tipKind = getTipKind(tips.head)
		val mTips = EvowareTranslator.encodeTips(tips)
		// TODO: Set these values depending on the tip kind
		val nWasteDelay = 10
		val nCleanerVolume = 10.0
		val nCleanerDelay = 10
		val nAirgapVolume = 10
		val nAirgapSpeed = 10
		val nRetractSpeed = 10
		List(
			T0_Wash(
				mTips,
				/*iWasteGrid*/ 2, /*iWasteSite*/ 1,
				/*iCleanerGrid*/ 2, /*iCleanerSite*/ 2,
				nContamInsideVolume + tipKind.nDecontaminateVolumeExtra,
				nWasteDelay,
				nCleanerVolume,
				nCleanerDelay,
				nAirgapVolume,
				nAirgapSpeed,
				nRetractSpeed,
				/*bFastWash*/ true
			),
			T0_Wash(
				mTips,
				/*iWasteGrid*/ 1, /*iWasteSite*/ 1,
				/*iCleanerGrid*/ 1, /*iCleanerSite*/ 2,
				nContamInsideVolume + tipKind.nDecontaminateVolumeExtra, // ???
				nWasteDelay,
				nCleanerVolume,
				nCleanerDelay,
				nAirgapVolume,
				nAirgapSpeed,
				nRetractSpeed,
				/*bFastWash*/ true
			)
		)
	}
}
