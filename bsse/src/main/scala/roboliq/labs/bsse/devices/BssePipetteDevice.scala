package roboliq.labs.bsse.devices

import scala.collection.immutable.SortedSet
import scala.collection.mutable.HashSet

import roboliq.core._
import roboliq.commands.pipette._
import roboliq.devices.pipette._
import roboliq.robots.evoware._
import roboliq.robots.evoware.devices.EvowarePipetteDevice


class BssePipetteDevice(tipModel50: TipModel, tipModel1000: TipModel) extends EvowarePipetteDevice {
	var tipModels: List[TipModel] = Nil
	var tips = SortedSet[Tip]()
	
	def setObjBase(ob: ObjBase): Result[Unit] = {
		for {
			tipModels <- ob.findAllTipModels()
			tips <- ob.findAllTips()
		} yield {
			this.tipModels = tipModels
			this.tips = SortedSet(tips : _*)
		}
	}
	
	def getTipModels = tipModels
	def getTips = tips
	
	lazy val tips1000 = SortedSet((0 to 3).map(i => new Tip(i, Some(tipModel1000))) : _*)
	lazy val tips50 = SortedSet((4 to 7).map(i => new Tip(i, Some(tipModel50))) : _*)
	lazy val config = new PipetteDeviceConfig(
		lTipModel = Seq(tipModel1000, tipModel50),
		tips = tips1000 ++ tips50,
		tipGroups = {
			lazy val g1000 = (0 to 3).map(i => i -> tipModel1000).toSeq
			lazy val g50 = (4 to 7).map(i => i -> tipModel50).toSeq
			Seq(g1000, g50)
		}
	)
	lazy val mapIndexToTip = config.tips.toSeq.map(tip => tip.index -> tip).toMap
	lazy val mapTipModels = config.lTipModel.map(spec => spec.id -> spec).toMap

	lazy val tipBlock1000 = new TipBlock(tips1000, Seq(tipModel1000))
	lazy val tipBlock50 = new TipBlock(tips50, Seq(tipModel50))
	lazy val tTipAll: SortedSet[Tip] = tipBlock1000.tTip ++ tipBlock50.tTip
	lazy val lTipAll: Seq[Tip] = tTipAll.toSeq
	lazy val mTipToBlock: Map[Tip, TipBlock] = (tipBlock1000.lTip.map(_ -> tipBlock1000) ++ tipBlock50.lTip.map(_ -> tipBlock50)).toMap
	lazy val mTipToModel: Map[Tip, TipModel] = mTipToBlock.mapValues(_.lTipModels.head)
	lazy val mModelToTips = Map(tipModel1000 -> tipBlock1000.tTip, tipModel50 -> tipBlock50.tTip)

	private val mapLcInfo = {
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
	
	def supportTipModelCounts(tipModelCounts: Map[TipModel, Int]): Result[Boolean] = {
		val b = tipModelCounts.forall(pair => {
			val (tipModel, nTips) = pair
			if (tipModel != tipModel50 && tipModel != tipModel1000)
				return Error("Unknown tip model: "+tipModel)
			(nTips <= 4)
		})
		Success(b)
	}
	
	/*
	def assignTips(tipsFree: SortedSet[Tip], tipModelCounts: Seq[Tuple2[TipModel, Int]]): Result[Seq[SortedSet[Tip]]] = {
		var tips = tipsFree
		val llTip = tipModelCounts.map(pair => {
			val (tipModel, nTips) = pair
			val tipsForModel = tips.filter(tip => mapModelToTips(tipModel).contains(tip.obj))
			if (nTips > tipsForModel.size)
				return Error("INTERNAL ERROR: assignTips: not enough tips"+(tipsFree, tipModelCounts))
			val tips1 = tipsForModel.toSeq.take(nTips)
			tips = tips -- tips1
			SortedSet(tips1 : _*)
		})
		Success(llTip)
	}
	*/

	def assignTips(lTipAvailable: SortedSet[Tip], tipModel: TipModel, nTips: Int): Result[SortedSet[Tip]] = {
		val lTipAppropriate = mModelToTips(tipModel).intersect(lTipAvailable)
		if (nTips > lTipAppropriate.size)
			return Error("INTERNAL ERROR: assignTips: not enough tips"+(lTipAvailable, tipModel, nTips))
		val lTip = lTipAppropriate.take(nTips)
		Success(lTip)
	}

	def areTipsDisposable: Boolean = false
	
	def getDispenseAllowableTipModels(liquid: Liquid, nVolume: LiquidVolume, nVolumeDest: LiquidVolume): Seq[TipModel] = {
		val b1000 =
			(nVolume >= tipModel1000.nVolumeAspirateMin)
		val b50 =
			(nVolume >= tipModel50.nVolumeAspirateMin && nVolume <= tipModel50.nVolume && !liquid.contaminants.contains(Contaminant.Cell))
		
		(if (b1000) Seq(tipModel1000) else Seq()) ++ (if (b50) Seq(tipModel50) else Seq())
	}
	
	def getAspiratePolicy(tipState: TipState, nVolume: LiquidVolume, wellState: WellState): Option[PipettePolicy] = {
		import PipettePosition._

		val liquid = wellState.liquid
		// Can't aspirate from an empty well
		//assert(liquid ne Liquid.empty)
		if (liquid eq Liquid.empty)
			return None

		val nVolumeSrc = wellState.nVolume
		val bLarge = (tipState.conf.index < 4)
		val bForceDry = (wellState.bCheckVolume && nVolumeSrc < LiquidVolume.ul(20))
		
		val sFamily = liquid.sFamily
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
	
	def getDispensePolicy(liquid: Liquid, tip: Tip, nVolume: LiquidVolume, wellState: WellState): Option[PipettePolicy] = {
		import PipettePosition._

		val sFamily = liquid.sFamily
		val bLarge = (tip.index < 4)
		val tipModel = if (tip.index < 4) tipModel1000 else tipModel50
		val nVolumeDest = wellState.nVolume
		
		val posDefault = {
			if (bLarge) {
				if (nVolume >= nFreeDispense1VolumeThreshold)
					Free
				else if (nVolumeDest > nFreeDispense2DestVolumeThreshold && nVolume >= nFreeDispense2VolumeThreshold)
					Free
				else if (nVolumeDest == 0)
					DryContact
				else
					WetContact
			}
			else {
				if (nVolumeDest == 0)
					DryContact
				else
					WetContact
			}
			// If our volume is high enough that we don't need to worry about accuracy
			if (bLarge && (nVolume >= nFreeDispense1VolumeThreshold || (nVolumeDest > nFreeDispense2DestVolumeThreshold && nVolume >= nFreeDispense2VolumeThreshold)))
				Free
			else if (nVolumeDest == 0)
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
	
	def getMixSpec(tipState: TipState, wellState: WellState, mixSpec_? : Option[MixSpec]): Result[MixSpec] = {
		// FIXME: Passing nVolume=0 is kinda unpredictable -- ellis
		val policyDefault_? = getAspiratePolicy(tipState, LiquidVolume.empty, wellState)
		val mixSpecDefault = MixSpec(Some(wellState.nVolume * 0.7), Some(4), policyDefault_?)
		val mixSpec = mixSpec_? match {
			case None => mixSpecDefault
			case Some(a) => a + mixSpecDefault
		}
		Success(mixSpec)
	}
	
	def getOtherTipsWhichCanBeCleanedSimultaneously(lTipAll: SortedSet[Tip], lTipCleaning: SortedSet[Tip]): SortedSet[Tip] = {
		val lModel = lTipCleaning.toSeq.map(tip => mTipToModel(tip))
		val lTip = lTipAll.filter(tip => lModel.contains(mTipToModel(tip)))
		lTip -- lTipCleaning
	}

	/*def getTipsToCleanSimultaneously(lTipAll: SortedSet[Tip], lTipCleaning: SortedSet[Tip]): SortedSet[Tip] = {
		val lModel = lTipCleaning.toSeq.map(tip => mTipToModel(tip))
		val lTip = lTipAll.filter(tip => lModel.contains(mTipToModel(tip)))
		lTip
	}*/
	
	//def batchCleanSpecs(lTipAll: SortedSet[Tip], mTipToCleanSpec: Map[Tip, WashSpec]): Map[Tip, WashSpec]
	def batchCleanSpecs(lTipAll: SortedSet[Tip], mTipToCleanSpec: Map[Tip, WashSpec]): Seq[Tuple2[WashSpec, SortedSet[Tip]]] = {
		val lBlock = mTipToCleanSpec.keys.map(mTipToBlock)
		val lBlockToCleanSpec = lBlock.map(block => {
			val lCleanSpec = mTipToCleanSpec.toSeq.collect({ case (tip, cleanSpec) if block.tTip.contains(tip) => cleanSpec })
			val cleanSpec = lCleanSpec.reduce(_ + _)
			block -> cleanSpec
		})
		lBlockToCleanSpec.map(pair => pair._2 -> pair._1.tTip).toSeq
		//val lTip = lTipAll.filter(tip => lModel.contains(mapTipToModels(tip.obj))).map()
	}
	
	def batchCleanTips(lTipAll: SortedSet[Tip]): Seq[SortedSet[Tip]] = {
		lTipAll.toSeq.groupBy(mTipToBlock).values.toSeq.map(l => SortedSet(l : _*))
	}
}
