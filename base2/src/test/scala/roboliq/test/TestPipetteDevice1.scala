package roboliq.test

import scala.collection.immutable.SortedSet
import roboliq.core._
import roboliq.pipette.PipetteDevice

class TestPipetteDevice1 extends PipetteDevice {
	/*
	val tipModel1000 = TipModel(
		id = "TIP1000",
		volume = LiquidVolume.ul(1000),
		volumeMin = LiquidVolume.ul(5)
	)
	val tipModel50 = TipModel(
		id = "TIP50",
		volume = LiquidVolume.ul(50),
		volumeMin = LiquidVolume.ul(1)
	)
	val tipModels = List(tipModel1000, tipModel50)
	val tips = SortedSet[Tip](
		Tip("TIP1", "LiHa", 0, 0, 0, Some(tipModel1000)),
		Tip("TIP2", "LiHa", 1, 1, 0, Some(tipModel1000)),
		Tip("TIP3", "LiHa", 2, 2, 0, Some(tipModel1000)),
		Tip("TIP4", "LiHa", 3, 3, 0, Some(tipModel1000)),
		Tip("TIP5", "LiHa", 4, 4, 0, Some(tipModel50)),
		Tip("TIP6", "LiHa", 5, 5, 0, Some(tipModel50)),
		Tip("TIP7", "LiHa", 6, 6, 0, Some(tipModel50)),
		Tip("TIP8", "LiHa", 7, 7, 0, Some(tipModel50))
	)

	def getTipModels = tipModels
	def getTips = tips
	
	def supportTipModelCounts(tipModelCounts: Map[TipModel, Int]): Result[Boolean] = {
		for ((model, n) <- tipModelCounts) {
			if (n > 4)
				return Success(false)
		}
		Success(true)
	}
	
	def assignTips(tipsFree: SortedSet[Tip], tipModel: TipModel, nTips: Int): Result[SortedSet[Tip]] = {
		Success(tipsFree.take(nTips))
	}
	
	def areTipsDisposable: Boolean = false
	
	def getTipAspirateVolumeMin(tip: TipState, liquid: Liquid): LiquidVolume = {
		tip.model_?.map(_.volumeMin).getOrElse(LiquidVolume.empty)
	}
	
	def getTipHoldVolumeMax(tip: TipState, liquid: Liquid): LiquidVolume = {
		tip.model_?.map(_.volume).getOrElse(LiquidVolume.empty)
	}
	
	private lazy val tips1000 = getTips.filter(_.index < 4)
	private lazy val tips50 = getTips.filter(_.index >= 4)
	/*private object junk {
		lazy val tipBlock1000 = new TipBlock(tips1000, Seq(tipModel1000))
		lazy val tipBlock50 = new TipBlock(tips50, Seq(tipModel50))
		lazy val tTipAll: SortedSet[Tip] = tipBlock1000.tTip ++ tipBlock50.tTip
		lazy val lTipAll: Seq[Tip] = tTipAll.toSeq
		lazy val mTipToModel: Map[Tip, TipModel] = mTipToBlock.mapValues(_.lTipModels.head)
	}
	private def mTipToBlock: Map[Tip, TipBlock] = (junk.tipBlock1000.lTip.map(_ -> junk.tipBlock1000) ++ junk.tipBlock50.lTip.map(_ -> junk.tipBlock50)).toMap*/
	//private def mModelToTips = Map(tipModel1000 -> junk.tipBlock1000.tTip, tipModel50 -> junk.tipBlock50.tTip)

	private lazy val mapLcInfo = {
		import PipettePosition._
		Map(
			tipModel1000 -> Map(
				"Water" -> Map(
					Free -> "Roboliq_Water_Air_1000",
					WetContact -> "Roboliq_Water_Wet_1000",
					DryContact -> "Roboliq_Water_Dry_1000"),
				"Glycerol" -> Map(
					Free -> "Roboliq_Glycerol_Air",
					WetContact -> "Roboliq_Glycerol_Wet_1000",
					DryContact -> "Roboliq_Glycerol_Dry_1000"
				),
				"Decon" -> Map(
					WetContact -> "Roboliq_Decon_Wet"
				)
			),
			tipModel50 -> Map(
				"Water" -> Map(
					Free -> "Roboliq_Water_Air_0050",
					WetContact -> "Roboliq_Water_Wet_0050",
					DryContact -> "Roboliq_Water_Dry_0050"),
				"Glycerol" -> Map(
					Free -> "Roboliq_Glycerol_Air",
					WetContact -> "Roboliq_Glycerol_Wet_0050",
					DryContact -> "Roboliq_Glycerol_Dry_0050"
				),
				"Decon" -> Map(
					WetContact -> "Roboliq_Decon_Wet"
				)
			)
		)
	}
	
	def getAspiratePolicy(tipState: TipState, volume: LiquidVolume, wellState: VesselState): Option[PipettePolicy] = {
		import PipettePosition._

		val liquid = wellState.liquid
		// Can't aspirate from an empty well
		//assert(liquid ne Liquid.empty)
		if (liquid eq Liquid.Empty)
			return None

		val nVolumeSrc = wellState.volume
		val bLarge = (tipState.conf.index < 4)
		val bForceDry = (wellState.isInitialVolumeKnown && nVolumeSrc < LiquidVolume.ul(20))
		
		val sFamily = "Glycerol" //liquid.sFamily
		val tipModel = tipState.model_?.get
		val posDefault = if (bForceDry) DryContact else WetContact 
		//val posDefault = WetContact
		mapLcInfo.get(tipModel) match {
			case None =>
			case Some(mapFamilies) =>
				mapFamilies.get(sFamily) match {
					case None =>
					case Some(mapPositions) =>
						if (mapPositions.isEmpty)
							return None
						else if (mapPositions.size == 1) {
							val (pos, lc) = mapPositions.head
							return Some(PipettePolicy(lc, pos))
						}
						else {
							mapPositions.get(posDefault) match {
								case None => return None
								case Some(lc) => return Some(PipettePolicy(lc, posDefault))
							}
						}
				}
		}
		
		val sPos = if (bForceDry) "Dry" else "Wet"
		//println("nVolumeSrc: "+nVolumeSrc+", "+sPos)
		val sTip = if (bLarge) "1000" else "0050"
		val sName = "Roboliq_"+sFamily+"_"+sPos+"_"+sTip
		Some(PipettePolicy(sName, posDefault))
	}
	
	val nFreeDispense1VolumeThreshold = LiquidVolume.ul(20)
	val nFreeDispense2VolumeThreshold = LiquidVolume.ul(5)
	val nFreeDispense2DestVolumeThreshold = LiquidVolume.ul(20)
	
	def getDispensePolicy(liquid: Liquid, tipModel: TipModel, volume: LiquidVolume, wellState: VesselState): Option[PipettePolicy] = {
		import PipettePosition._

		val sFamily = "Glycerol" //liquid.sFamily
		val bLarge = (tipModel eq tipModel1000)
		val nVolumeDest = wellState.volume
		
		val posDefault = {
			if (bLarge) {
				if (volume >= nFreeDispense1VolumeThreshold)
					Free
				else if (nVolumeDest > nFreeDispense2DestVolumeThreshold && volume >= nFreeDispense2VolumeThreshold)
					Free
				else if (nVolumeDest.isEmpty)
					DryContact
				else
					WetContact
			}
			else {
				if (nVolumeDest.isEmpty)
					DryContact
				else
					WetContact
			}
			// If our volume is high enough that we don't need to worry about accuracy
			if (bLarge && (volume >= nFreeDispense1VolumeThreshold || (nVolumeDest > nFreeDispense2DestVolumeThreshold && volume >= nFreeDispense2VolumeThreshold)))
				Free
			else if (nVolumeDest.isEmpty)
				DryContact
			else
				WetContact
		}
		//val lPosAlternates = Seq(WetContact, Free)
		
		mapLcInfo.get(tipModel) match {
			case None =>
			case Some(mapFamilies) =>
				mapFamilies.get(sFamily) match {
					case None =>
					case Some(mapPositions) =>
						if (mapPositions.isEmpty)
							return None
						else if (mapPositions.size == 1) {
							val (pos, lc) = mapPositions.head
							return Some(PipettePolicy(lc, pos))
						}
						else {
							mapPositions.get(posDefault) match {
								case None => return None
								case Some(lc) => return Some(PipettePolicy(lc, posDefault))
							}
						}
				}
		}
		
		val sPos = posDefault match {
			case PipettePosition.Free => "Air"
			case PipettePosition.DryContact => "Dry"
			case PipettePosition.WetContact => "Wet"
		}
		val sTip = if (bLarge) "1000" else "0050"
		val sName = "Roboliq_"+sFamily+"_"+sPos+"_"+sTip
		Some(PipettePolicy(sName, posDefault))
	}
	
	def getMixSpec(tipState: TipState, wellState: VesselState, mixSpec_? : Option[MixSpecOpt]): Result[MixSpec] = {
		// FIXME: Passing volume=0 is kinda unpredictable -- ellis
		val policyDefault_? = getAspiratePolicy(tipState, LiquidVolume.empty, wellState)
		val mixSpecDefault = MixSpecOpt(Some(wellState.volume * 0.7), Some(4), policyDefault_?)
		(mixSpec_? match {
			case None => mixSpecDefault
			case Some(a) => a + mixSpecDefault
		}).toMixSpec match {
			case RqSuccess(x, _) => Success(x)
			case RqError(l, _) => Error(l)
		}
	}

	def canBatchSpirateItems(lTwvp: List[TipWellVolumePolicy]): Boolean = true
	def canBatchMixItems(lTwvp: List[TipWellMix]): Boolean = true
	
	def getOtherTipsWhichCanBeCleanedSimultaneously(lTipAll: SortedSet[Tip], lTipCleaning: SortedSet[Tip]): SortedSet[Tip] = {
		val lModel = lTipCleaning.toSeq.map(_.permanent_?).distinct
		val lTip = lTipAll.filter(tip => lModel.contains(tip.permanent_?))
		lTip -- lTipCleaning
	}

	/*def getTipsToCleanSimultaneously(lTipAll: SortedSet[Tip], lTipCleaning: SortedSet[Tip]): SortedSet[Tip] = {
		val lModel = lTipCleaning.toSeq.map(tip => mTipToModel(tip))
		val lTip = lTipAll.filter(tip => lModel.contains(mTipToModel(tip)))
		lTip
	}*/
	
	//def batchCleanSpecs(lTipAll: SortedSet[Tip], mTipToCleanSpec: Map[Tip, WashSpec]): Map[Tip, WashSpec]
	def batchCleanSpecs(lTipAll: SortedSet[Tip], mTipToCleanSpec: Map[Tip, WashSpec]): Seq[Tuple2[WashSpec, SortedSet[Tip]]] = {
		lTipAll.groupBy(mTipToCleanSpec).toSeq
	}
	
	def batchCleanTips(lTipAll: SortedSet[Tip]): Seq[SortedSet[Tip]] = {
		Seq(lTipAll)
	}
	*/
}
