package roboliq.labs.bsse.devices

import scala.collection.immutable.SortedSet
import scala.collection.mutable.HashSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._
import roboliq.robots.evoware._
import roboliq.robots.evoware.devices.EvowarePipetteDevice


class BssePipetteDevice(tipModel50: TipModel, tipModel1000: TipModel) extends EvowarePipetteDevice {
	val tips1000 = SortedSet((0 to 3).map(i => new Tip(i, Some(tipModel1000))) : _*)
	val tips50 = SortedSet((4 to 7).map(i => new Tip(i, Some(tipModel50))) : _*)
	val config = new PipetteDeviceConfig(
		lTipModel = Seq(tipModel1000, tipModel50),
		tips = tips1000 ++ tips50,
		tipGroups = {
			val g1000 = (0 to 3).map(i => i -> tipModel1000).toSeq
			val g50 = (4 to 7).map(i => i -> tipModel50).toSeq
			Seq(g1000, g50)
		}
	)
	val mapIndexToTip = config.tips.toSeq.map(tip => tip.index -> tip).toMap
	val mapTipModels = config.lTipModel.map(spec => spec.id -> spec).toMap

	val tipBlock1000 = new TipBlock(tips1000.map(_.conf0), Seq(tipModel1000))
	val tipBlock50 = new TipBlock(tips50.map(_.conf0), Seq(tipModel50))
	val tTipAll: SortedSet[TipConfigL2] = tipBlock1000.tTip ++ tipBlock50.tTip
	val lTipAll: Seq[TipConfigL2] = tTipAll.toSeq
	val mTipToBlock: Map[TipConfigL2, TipBlock] = (tipBlock1000.lTip.map(_ -> tipBlock1000) ++ tipBlock50.lTip.map(_ -> tipBlock50)).toMap
	val mTipToModel: Map[TipConfigL2, TipModel] = mTipToBlock.mapValues(_.lTipModels.head)
	val mModelToTips = Map(tipModel1000 -> tipBlock1000.tTip, tipModel50 -> tipBlock50.tTip)

	val plateDecon = new PlateObj
	
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
	
	override def addKnowledge(kb: KnowledgeBase) = {
		super.addKnowledge(kb)
		
		/*config.tips.foreach(tip => {
			val tipSpec = if (tip.index < 4) tipModel1000 else tipModel50
			val tipSetup = kb.getObjSetup[TipSetup](tip)
			tipSetup.modelPermanent_? = Some(tipSpec)
		})*/
		new PlateProxy(kb, plateDecon) match {
			case pp =>
				pp.label = "Decon"
				pp.location = "decon3"
				pp.setDimension(8, 1)
				val reagent = new Reagent
				pp.wells.foreach(well => well.reagent_? = Some(reagent))
				reagent.sName_? = Some("Decon")
				reagent.sFamily_? = Some("Decon")
				reagent.group_? = Some(new LiquidGroup(GroupCleanPolicy.NNN))
		}
		kb.addPlate(plateDecon, true)
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
	def assignTips(tipsFree: SortedSet[TipConfigL2], tipModelCounts: Seq[Tuple2[TipModel, Int]]): Result[Seq[SortedSet[TipConfigL2]]] = {
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

	def assignTips(lTipAvailable: SortedSet[TipConfigL2], tipModel: TipModel, nTips: Int): Result[SortedSet[TipConfigL2]] = {
		val lTipAppropriate = mModelToTips(tipModel).intersect(lTipAvailable)
		if (nTips > lTipAppropriate.size)
			return Error("INTERNAL ERROR: assignTips: not enough tips"+(lTipAvailable, tipModel, nTips))
		val lTip = lTipAppropriate.take(nTips)
		Success(lTip)
	}

	def areTipsDisposable: Boolean = false
	
	def getDispenseAllowableTipModels(liquid: Liquid, nVolume: Double, nVolumeDest: Double): Seq[TipModel] = {
		val b1000 =
			(nVolume >= tipModel1000.nVolumeAspirateMin)
		val b50 =
			(nVolume >= tipModel50.nVolumeAspirateMin && nVolume <= tipModel50.nVolume && !liquid.contaminants.contains(Contaminant.Cell))
		
		(if (b1000) Seq(tipModel1000) else Seq()) ++ (if (b50) Seq(tipModel50) else Seq())
	}
	
	def getAspiratePolicy(tipState: TipStateL2, nVolume: Double, wellState: WellStateL2): Option[PipettePolicy] = {
		import PipettePosition._

		val liquid = wellState.liquid
		// Can't aspirate from an empty well
		//assert(liquid ne Liquid.empty)
		if (liquid eq Liquid.empty)
			return None

		val nVolumeSrc = wellState.nVolume
		val bLarge = (tipState.conf.obj.index < 4)
		val bForceDry = (wellState.bCheckVolume && nVolumeSrc < 20)
		
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
	
	val nFreeDispense1VolumeThreshold = 20
	val nFreeDispense2VolumeThreshold = 5
	val nFreeDispense2DestVolumeThreshold = 20
	
	def getDispensePolicy(liquid: Liquid, tip: TipConfigL2, nVolume: Double, wellState: WellStateL2): Option[PipettePolicy] = {
		import PipettePosition._

		val sFamily = liquid.sFamily
		val bLarge = (tip.obj.index < 4)
		val tipModel = if (tip.obj.index < 4) tipModel1000 else tipModel50
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
	
	def getMixSpec(tipState: TipStateL2, wellState: WellStateL2, mixSpec_? : Option[MixSpec]): Result[MixSpecL2] = {
		// FIXME: Passing nVolume=0 is kinda dangerous -- ellis
		val policyDefault_? = getAspiratePolicy(tipState, 0, wellState)
		val mixSpecDefault = MixSpec(Some(wellState.nVolume * 0.7), Some(4), policyDefault_?)
		val mixSpec = mixSpec_? match {
			case None => mixSpecDefault
			case Some(a) => a + mixSpecDefault
		}
		mixSpec.toL2
	}
	
	def getOtherTipsWhichCanBeCleanedSimultaneously(lTipAll: SortedSet[TipConfigL2], lTipCleaning: SortedSet[TipConfigL2]): SortedSet[TipConfigL2] = {
		val lModel = lTipCleaning.toSeq.map(tip => mTipToModel(tip))
		val lTip = lTipAll.filter(tip => lModel.contains(mTipToModel(tip)))
		lTip -- lTipCleaning
	}

	/*def getTipsToCleanSimultaneously(lTipAll: SortedSet[TipConfigL2], lTipCleaning: SortedSet[TipConfigL2]): SortedSet[TipConfigL2] = {
		val lModel = lTipCleaning.toSeq.map(tip => mTipToModel(tip))
		val lTip = lTipAll.filter(tip => lModel.contains(mTipToModel(tip)))
		lTip
	}*/
	
	//def batchCleanSpecs(lTipAll: SortedSet[TipConfigL2], mTipToCleanSpec: Map[TipConfigL2, WashSpec]): Map[TipConfigL2, WashSpec]
	def batchCleanSpecs(lTipAll: SortedSet[TipConfigL2], mTipToCleanSpec: Map[TipConfigL2, WashSpec]): Seq[Tuple2[WashSpec, SortedSet[TipConfigL2]]] = {
		val lBlock = mTipToCleanSpec.keys.map(mTipToBlock)
		val lBlockToCleanSpec = lBlock.map(block => {
			val lCleanSpec = mTipToCleanSpec.toSeq.collect({ case (tip, cleanSpec) if block.tTip.contains(tip) => cleanSpec })
			val cleanSpec = lCleanSpec.reduce(_ + _)
			block -> cleanSpec
		})
		lBlockToCleanSpec.map(pair => pair._2 -> pair._1.tTip).toSeq
		//val lTip = lTipAll.filter(tip => lModel.contains(mapTipToModels(tip.obj))).map()
	}
	
	def batchCleanTips(lTipAll: SortedSet[TipConfigL2]): Seq[SortedSet[TipConfigL2]] = {
		lTipAll.toSeq.groupBy(mTipToBlock).values.toSeq.map(l => SortedSet(l : _*))
	}
}
