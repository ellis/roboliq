package roboliq.labs.weizmann.devices

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._
import roboliq.robots.evoware._
import roboliq.robots.evoware.devices.EvowarePipetteDevice


class WeizmannPipetteDevice(tipModels: Seq[TipModel]) extends EvowarePipetteDevice {
	val config = new PipetteDeviceConfig(
		tipModels,
		tips = SortedSet((0 to 7).map(i => new Tip(i, None)) : _*),
		tipGroups = tipModels.map(spec => (0 to 7).map(i => i -> spec))
	)
	
	def getDispenseAllowableTipModels(liquid: Liquid, nVolume: Double, nVolumeDest: Double): Seq[TipModel] = {
		val l1 = tipModels.filter(tipModel => nVolume >= tipModel.nVolumeAspirateMin && nVolume <= tipModel.nVolume)
		val l2 = {
			if (l1.isEmpty) {
				val tipModelLargest = tipModels.reduce((a, b) => if (a.nVolume > b.nVolume) a else b)
				Seq(tipModelLargest)
			}
			else
				l1
		}
		l2
	}
	
	def supportTipModelCounts(tipModelCounts: Map[TipModel,Int]): Result[Boolean] = {
		Success(
			if (tipModelCounts.isEmpty) true
			else (tipModelCounts.size == 1 && tipModelCounts.head._2 <= 8)
		)
	}

	def assignTips(lTipAvailable: SortedSet[TipConfigL2], tipModel: TipModel, nTips: Int): Result[SortedSet[TipConfigL2]] = {
		val lTipAppropriate = lTipAvailable
		if (nTips > lTipAppropriate.size)
			return Error("INTERNAL ERROR: assignTips: not enough tips"+(lTipAvailable, tipModel, nTips))
		val lTip = lTipAppropriate.take(nTips)
		Success(lTip)
	}
	
	//private val mapTipSpecs = config.tipModels.map(spec => spec.id -> spec).toMap
	def areTipsDisposable: Boolean = true

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
	
	def getMixSpec(tipState: TipStateL2, wellState: WellStateL2, mixSpec_? : Option[MixSpec]): Result[MixSpecL2] = {
		val mixSpecDefault = MixSpec(Some(wellState.nVolume * 0.7), Some(4), Some(PipettePolicy("LCWMX", PipettePosition.WetContact)))
		val mixSpec = mixSpec_? match {
			case None => mixSpecDefault
			case Some(a) => a + mixSpecDefault
		}
		mixSpec.toL2
	}

	def getOtherTipsWhichCanBeCleanedSimultaneously(lTipAll: SortedSet[TipConfigL2], lTipCleaning: SortedSet[TipConfigL2]): SortedSet[TipConfigL2] = {
		lTipAll -- lTipCleaning
	}

	def batchCleanSpecs(lTipAll: SortedSet[TipConfigL2], mTipToCleanSpec: Map[TipConfigL2, WashSpec]): Seq[Tuple2[WashSpec, SortedSet[TipConfigL2]]] = {
		val lCleanSpec = mTipToCleanSpec.toSeq.map(_._2)
		val cleanSpec = lCleanSpec.reduce(_ + _)
		Seq(cleanSpec -> lTipAll)
	}
}
