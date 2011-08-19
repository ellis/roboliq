package roboliq.devices.pipette

import scala.collection.immutable.SortedSet

import roboliq.common._
//import roboliq.parts._
//import roboliq.tokens._


trait PipetteDevice {
	val config: PipetteDeviceConfig
	/** Minimum volume which can be aspirated */
	def getTipAspirateVolumeMin(tip: Tip, liquid: Liquid): Double
	/** Maximum volume of the given liquid which this tip can hold */
	def getTipHoldVolumeMax(tip: Tip, liquid: Liquid): Double
	/** Choose aspirate method */
	def getAspiratePolicy(tipState: TipStateL1, wellState: WellStateL1): Option[PipettePolicy]
	/** Choose dispense method */
	def getDispensePolicy(tipState: TipStateL1, wellState: WellStateL1, nVolume: Double): Option[PipettePolicy]
	def chooseTipWellPairs(tips: SortedSet[Tip], wells: SortedSet[Well], wellPrev_? : Option[Well]): Seq[Tuple2[Tip, Well]]
	def batchesForAspirate(twvps: Seq[TipWellVolumePolicy]): Seq[Seq[TipWellVolumePolicy]]
	def batchesForDispense(twvps: Seq[TipWellVolumePolicy]): Seq[Seq[TipWellVolumePolicy]]
	def batchesForClean(tcs: Seq[Tuple2[Tip, CleanDegree.Value]]): Seq[L1C_SetTipStateClean]
}

class PipetteDeviceGeneric extends PipetteDevice {
	val config = new PipetteDeviceConfig(
		tips = SortedSet((0 to 1).map(i => new Tip(i)) : _*),
		tipGroups = Array(Array(0,1))
	)
	def getTipAspirateVolumeMin(tip: Tip, liquid: Liquid): Double = 0
	def getTipHoldVolumeMax(tip: Tip, liquid: Liquid): Double = 1000
	def getAspiratePolicy(tipState: TipStateL1, wellState: WellStateL1): Option[PipettePolicy] = {
		val liquid = wellState.liquid
		// Can't aspirate from an empty well
		assert(liquid ne Liquid.empty)

		if (liquid.bCells)
			Some(PipettePolicy("Comp cells free dispense", PipettePosition.Free))
		else if (liquid.sName.contains("DMSO"))
			Some(PipettePolicy("DMSO free dispense", PipettePosition.Free))
		else if (liquid.sName.contains("D-BSSE Decon"))
			Some(PipettePolicy("D-BSSE Decon", PipettePosition.WetContact))
		else
			Some(PipettePolicy("Water wet contact", PipettePosition.WetContact))
	}
	
	val nFreeDispenseVolumeThreshold = 20
	
	def getDispensePolicy(tipState: TipStateL1, wellState: WellStateL1, nVolume: Double): Option[PipettePolicy] = {
		val liquid = tipState.liquid
		
		if (liquid.bCells)
			Some(PipettePolicy("Comp cells free dispense", PipettePosition.Free))
		else if (liquid.sName.contains("DMSO"))
			Some(PipettePolicy("DMSO free dispense", PipettePosition.Free))
		else if (liquid.sName.contains("D-BSSE Decon"))
			Some(PipettePolicy("D-BSSE Decon", PipettePosition.Free))
		// If our volume is high enough that we don't need to worry about accuracy
		else if (nVolume >= nFreeDispenseVolumeThreshold)
			Some(PipettePolicy("Water free dispense", PipettePosition.Free))
		else if (wellState.nVolume == 0)
			Some(PipettePolicy("Water dry contact", PipettePosition.Free))
		else
			Some(PipettePolicy("Water wet contact", PipettePosition.Free))
	}
	
	def chooseTipWellPairs(tips: SortedSet[Tip], wells: SortedSet[Well], wellPrev_? : Option[Well]): Seq[Tuple2[Tip, Well]] = {
		val tips2 = tips.toIndexedSeq
		val wells2 = wells.toSeq.zipWithIndex
		for ((well, i) <- wells2) yield {
			val i2 = i % tips2.size
			tips2(i2) -> well
		}
	}
	
	def batchesForAspirate(twvps: Seq[TipWellVolumePolicy]): Seq[Seq[TipWellVolumePolicy]] = Seq(twvps)
	def batchesForDispense(twvps: Seq[TipWellVolumePolicy]): Seq[Seq[TipWellVolumePolicy]] = Seq(twvps)
	def batchesForClean(tcs: Seq[Tuple2[Tip, CleanDegree.Value]]): Seq[L1C_SetTipStateClean] = Seq(new L1C_SetTipStateClean(tcs.map(_._1), tcs.head._2))
}
