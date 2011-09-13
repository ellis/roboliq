package roboliq.labs.weizmann.devices

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._

import _root_.evoware._


class WeizmannPipetteDevice extends PipetteDevice {
	val tipSpec10 = new TipModel("DiTi 10ul", 10, 0, 0, 0)
	val tipSpec20 = new TipModel("DiTi 20ul", 20, 0, 0, 0)
	val tipSpec50 = new TipModel("DiTi 50ul", 50, 1, 0, 0)
	val tipSpec200 = new TipModel("DiTi 200ul", 200, 2, 0, 0)
	//private val tipSpec1000 = new TipModel("DiTi 1000ul", 1000, 3, 960)
	val tipSpec1000 = new TipModel("DiTi 1000ul", 925, 0, 0, 0)
	private val tipSpecs = Seq(tipSpec200, tipSpec10, tipSpec20, tipSpec50, tipSpec1000)
	//private val tipSpecs = Seq(tipSpec1000)
	val config = new PipetteDeviceConfig(
		tipSpecs = tipSpecs,
		tips = SortedSet((0 to 7).map(i => new Tip(i)) : _*),
		tipGroups = tipSpecs.map(spec => (0 to 7).map(i => i -> spec))
	)
	//private val mapTipSpecs = config.tipSpecs.map(spec => spec.id -> spec).toMap
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
