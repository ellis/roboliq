package roboliq.devices.pipette

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._


trait PipetteDevice extends Device {
	val config: PipetteDeviceConfig
	
	def addKnowledge(kb: KnowledgeBase)
	def areTipsDisposable: Boolean
	/** Minimum volume which can be aspirated */
	def getTipAspirateVolumeMin(tip: TipStateL2, liquid: Liquid): Double
	/** Maximum volume of the given liquid which this tip can hold */
	def getTipHoldVolumeMax(tip: TipStateL2, liquid: Liquid): Double
	def getPipetteSpec(sLiquidClass: String): Option[PipetteSpec]
	/** Choose aspirate method */
	def getAspiratePolicy(tipState: TipStateL2, wellState: WellStateL2): Option[PipettePolicy]
	/** Choose dispense method */
	def getDispensePolicy(liquid: Liquid, tip: TipConfigL2, nVolume: Double, nVolumeDest: Double): Option[PipettePolicy]
	def chooseTipWellPairs(tips: SortedSet[Tip], wells: SortedSet[Well], wellPrev_? : Option[Well]): Seq[Tuple2[Tip, Well]]
	//def chooseWashPrograms(tip: TipConfigL2, intensity: WashIntensity.Value): Seq[Int]
	def batchesForAspirate(twvps: Seq[L2A_SpirateItem]): Seq[Seq[L2A_SpirateItem]]
	def batchesForDispense(twvps: Seq[L2A_SpirateItem]): Seq[Seq[L2A_SpirateItem]]
	def batchesForClean(tcs: Seq[Tuple2[TipConfigL2, WashIntensity.Value]]): Seq[Seq[Tuple2[TipConfigL2, WashIntensity.Value]]]
	def batchesForMix(twvpcs: Seq[L2A_MixItem]): Seq[Seq[L2A_MixItem]]
}

class PipetteDeviceGeneric extends PipetteDevice {
	private val tipSpec50 = new TipSpec("50", 50, 0, 45)
	private val tipSpec1000 = new TipSpec("1000", 1000, 0, 960)
	val config = new PipetteDeviceConfig(
		tipSpecs = Seq(tipSpec50, tipSpec1000),
		tips = SortedSet((0 to 1).map(i => new Tip(i)) : _*),
		tipGroups = Seq(
				Seq(0 -> tipSpec50, 1 -> tipSpec50),
				Seq(0 -> tipSpec1000, 1 -> tipSpec1000)
				)
	)
	def areTipsDisposable: Boolean = true
	def addKnowledge(kb: KnowledgeBase) = {
		config.tips.foreach(kb.addObject)
	}
	def getTipAspirateVolumeMin(tip: TipStateL2, liquid: Liquid): Double = 0
	def getTipHoldVolumeMax(tip: TipStateL2, liquid: Liquid): Double = tip.sType_? match { case None => 0; case Some(s) => s.toDouble }
	def getPipetteSpec(sLiquidClass: String): Option[PipetteSpec] = None
	def getAspiratePolicy(tipState: TipStateL2, wellState: WellStateL2): Option[PipettePolicy] = {
		val liquid = wellState.liquid
		// Can't aspirate from an empty well
		assert(liquid ne Liquid.empty)

		if (liquid.contaminants.contains(Contaminant.Cell))
			Some(PipettePolicy("Comp cells free dispense", PipettePosition.Free))
		else if (liquid.sName.contains("DMSO"))
			Some(PipettePolicy("DMSO free dispense", PipettePosition.Free))
		else if (liquid.sName.contains("D-BSSE Decon"))
			Some(PipettePolicy("D-BSSE Decon", PipettePosition.WetContact))
		else
			Some(PipettePolicy("Water wet contact", PipettePosition.WetContact))
	}
	
	val nFreeDispenseVolumeThreshold = 20
	
	def getDispensePolicy(liquid: Liquid, tip: TipConfigL2, nVolume: Double, nVolumeDest: Double): Option[PipettePolicy] = {
		if (liquid.contaminants.contains(Contaminant.Cell))
			Some(PipettePolicy("Comp cells free dispense", PipettePosition.Free))
		else if (liquid.sName.contains("DMSO"))
			Some(PipettePolicy("DMSO free dispense", PipettePosition.Free))
		else if (liquid.sName.contains("D-BSSE Decon"))
			Some(PipettePolicy("D-BSSE Decon", PipettePosition.Free))
		// If our volume is high enough that we don't need to worry about accuracy
		else if (nVolume >= nFreeDispenseVolumeThreshold)
			Some(PipettePolicy("Water free dispense", PipettePosition.Free))
		else if (nVolumeDest == 0)
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
	def batchesForClean(tcs: Seq[Tuple2[TipConfigL2, WashIntensity.Value]]): Seq[Seq[Tuple2[TipConfigL2, WashIntensity.Value]]] = Seq(tcs)
	def batchesForMix(twvpcs: Seq[L2A_MixItem]): Seq[Seq[L2A_MixItem]] = Seq(twvpcs)
}
