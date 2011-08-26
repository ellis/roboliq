package roboliq.devices.pipette

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._


trait PipetteDevice {
	val config: PipetteDeviceConfig
	
	def addKnowledge(kb: KnowledgeBase)
	/** Minimum volume which can be aspirated */
	def getTipAspirateVolumeMin(tip: TipStateL2, liquid: Liquid): Double
	/** Maximum volume of the given liquid which this tip can hold */
	def getTipHoldVolumeMax(tip: TipStateL2, liquid: Liquid): Double
	/** Choose aspirate method */
	def getAspiratePolicy(tipState: TipStateL2, wellState: WellStateL2): Option[PipettePolicy]
	/** Choose dispense method */
	def getDispensePolicy(tipState: TipStateL2, wellState: WellStateL2, nVolume: Double): Option[PipettePolicy]
	def chooseTipWellPairs(tips: SortedSet[Tip], wells: SortedSet[Well], wellPrev_? : Option[Well]): Seq[Tuple2[Tip, Well]]
	def batchesForAspirate(twvps: Seq[L2A_SpirateItem]): Seq[Seq[L2A_SpirateItem]]
	def batchesForDispense(twvps: Seq[L2A_SpirateItem]): Seq[Seq[L2A_SpirateItem]]
	def batchesForClean(tcs: Seq[Tuple2[TipConfigL2, CleanDegree.Value]]): Seq[Seq[Tuple2[TipConfigL2, CleanDegree.Value]]]
	def batchesForMix(twvpcs: Seq[L2A_MixItem]): Seq[Seq[L2A_MixItem]]
}

class PipetteDeviceGeneric extends PipetteDevice {
	val config = new PipetteDeviceConfig(
		tips = SortedSet((0 to 1).map(i => new Tip(i)) : _*),
		tipGroups = Array(Array(0,1))
	)
	def addKnowledge(kb: KnowledgeBase) = {
		config.tips.foreach(kb.addObject)
	}
	def getTipAspirateVolumeMin(tip: TipStateL2, liquid: Liquid): Double = 0
	def getTipHoldVolumeMax(tip: TipStateL2, liquid: Liquid): Double = tip.sType_? match { case None => 0; case Some(s) => s.toDouble }
	def getAspiratePolicy(tipState: TipStateL2, wellState: WellStateL2): Option[PipettePolicy] = {
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
	
	def getDispensePolicy(tipState: TipStateL2, wellState: WellStateL2, nVolume: Double): Option[PipettePolicy] = {
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
	
	def batchesForAspirate(twvps: Seq[L2A_SpirateItem]): Seq[Seq[L2A_SpirateItem]] = Seq(twvps)
	def batchesForDispense(twvps: Seq[L2A_SpirateItem]): Seq[Seq[L2A_SpirateItem]] = Seq(twvps)
	def batchesForClean(tcs: Seq[Tuple2[TipConfigL2, CleanDegree.Value]]): Seq[Seq[Tuple2[TipConfigL2, CleanDegree.Value]]] = Seq(tcs)
	def batchesForMix(twvpcs: Seq[L2A_MixItem]): Seq[Seq[L2A_MixItem]] = Seq(twvpcs)
}
