package roboliq.labs.weizmann.devices

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._
//import _root_.evoware._


class WeizmannPipetteDevice(tipModels: Seq[TipModel]) extends PipetteDevice {
	val config = new PipetteDeviceConfig(
		tipModels,
		tips = SortedSet((0 to 7).map(i => new Tip(i)) : _*),
		tipGroups = tipModels.map(spec => (0 to 7).map(i => i -> spec))
	)
	//private val mapTipSpecs = config.tipModels.map(spec => spec.id -> spec).toMap
	def areTipsDisposable: Boolean = true
	def addKnowledge(kb: KnowledgeBase) = {
		config.tips.foreach(kb.addObject)
	}
	def getTipAspirateVolumeMin(tip: TipStateL2, liquid: Liquid): Double = tip.model_? match { case None => 0; case Some(model) => model.nVolumeAspirateMin }
	def getTipHoldVolumeMax(tip: TipStateL2, liquid: Liquid): Double = tip.model_? match { case None => 0; case Some(model) => model.nVolume }
	//def getPipetteSpec(sLiquidClass: String): Option[PipetteSpec] = mapPipetteSpecs.get(sLiquidClass)
	def getAspiratePolicy(tipState: TipStateL2, wellState: WellStateL2): Option[PipettePolicy] = {
		val liquid = wellState.liquid
		// Can't aspirate from an empty well
		assert(liquid ne Liquid.empty)

		if (liquid.contaminants.contains(Contaminant.Cell))
			Some(PipettePolicy("PIE_AUTAIR_SLOW", PipettePosition.Free))
		else if (liquid.sName.contains("DMSO"))
			Some(PipettePolicy("PIE_AUTAIR", PipettePosition.Free))
		else if (liquid.sName.contains("D-BSSE Decon"))
			Some(PipettePolicy("PIE_DECON", PipettePosition.WetContact))
		else
			Some(PipettePolicy("PIE_AUT", PipettePosition.WetContact))
	}
	
	val nFreeDispenseVolumeThreshold = 20
	
	def getDispensePolicy(liquid: Liquid, tip: TipConfigL2, nVolume: Double, nVolumeDest: Double): Option[PipettePolicy] = {
		if (liquid.contaminants.contains(Contaminant.Cell))
			Some(PipettePolicy("PIE_", PipettePosition.Free))
		else if (liquid.sName.contains("DMSO"))
			Some(PipettePolicy("PIE_AUTAIR", PipettePosition.Free))
		else if (liquid.sName.contains("D-BSSE Decon"))
			Some(PipettePolicy("PIE_DECON", PipettePosition.Free))
		// If our volume is high enough that we don't need to worry about accuracy
		else if (nVolume >= nFreeDispenseVolumeThreshold)
			Some(PipettePolicy("PIE_AUTAIR", PipettePosition.Free))
		else if (nVolumeDest == 0)
			Some(PipettePolicy("PIE_AUTBOT", PipettePosition.Free))
		else
			Some(PipettePolicy("PIE_AUT", PipettePosition.Free))
	}
	
	def chooseTipWellPairs(tips: SortedSet[Tip], wells: SortedSet[Well], wellPrev_? : Option[Well]): Seq[Tuple2[Tip, Well]] = {
		val tips2 = tips.toIndexedSeq
		val wells2 = wells.toSeq.zipWithIndex
		for ((well, i) <- wells2) yield {
			val i2 = i % tips2.size
			tips2(i2) -> well
		}
	}
	
	def batchesForAspirate(items: Seq[L2A_SpirateItem]): Seq[Seq[L2A_SpirateItem]] = {
		items.groupBy(_.well.holder).values.toSeq
	}
	def batchesForDispense(twvps: Seq[L2A_SpirateItem]): Seq[Seq[L2A_SpirateItem]] = Seq(twvps)
	def batchesForClean(tcs: Seq[Tuple2[TipConfigL2, WashIntensity.Value]]): Seq[Seq[Tuple2[TipConfigL2, WashIntensity.Value]]] = Seq(tcs)
	def batchesForMix(twvpcs: Seq[L2A_MixItem]): Seq[Seq[L2A_MixItem]] = Seq(twvpcs)
}
