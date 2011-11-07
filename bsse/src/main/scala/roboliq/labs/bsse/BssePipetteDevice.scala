package roboliq.labs.bsse

import scala.collection.immutable.SortedSet
import scala.collection.mutable.HashSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._
import roboliq.robots.evoware._
import roboliq.robots.evoware.devices.EvowarePipetteDevice


class BssePipetteDevice(tipModel50: TipModel, tipModel1000: TipModel) extends EvowarePipetteDevice {
	val tips1000 = SortedSet((0 to 3).map(i => new Tip(i)) : _*)
	val tips50 = SortedSet((4 to 7).map(i => new Tip(i)) : _*)
	val config = new PipetteDeviceConfig(
		tipSpecs = Seq(tipModel50, tipModel1000),
		tips = tips1000 ++ tips50,
		tipGroups = {
			val g1000 = (0 to 3).map(i => i -> tipModel1000).toSeq
			val g50 = (4 to 7).map(i => i -> tipModel50).toSeq
			Seq(g1000, g50)
		}
	)
	val mapIndexToTip = config.tips.toSeq.map(tip => tip.index -> tip).toMap
	val mapModelToTips = Map(tipModel1000 -> tips1000, tipModel50 -> tips50)
	val mapTipToModels = mapModelToTips.flatMap(pair => (pair._2.toSeq).map(_ -> pair._1))
	val mapTipModels = config.tipSpecs.map(spec => spec.id -> spec).toMap

	val plateDecon = new Plate
	
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
					WetContact -> "Roboliq_Glycerol_Wet_1000"
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
					WetContact -> "Roboliq_Glycerol_Wet_0050"
				),
				"Decon" -> Map(
					WetContact -> "Roboliq_Decon_Wet"
				)
			)
		)
	}
	
	override def addKnowledge(kb: KnowledgeBase) = {
		super.addKnowledge(kb)
		
		config.tips.foreach(tip => {
			val tipSpec = if (tip.index < 4) tipModel1000 else tipModel50
			val tipSetup = kb.getObjSetup[TipSetup](tip)
			tipSetup.modelPermanent_? = Some(tipSpec)
		})
		new PlateProxy(kb, plateDecon) match {
			case pp =>
				pp.label = "Decon"
				pp.location = "decon3"
				pp.setDimension(8, 1)
				val reagent = new Reagent
				pp.wells.foreach(well => kb.getWellSetup(well).reagent_? = Some(reagent))
				val rs = kb.getReagentSetup(reagent)
				rs.sName_? = Some("Decon")
				rs.sFamily_? = Some("Decon")
				rs.group_? = Some(new LiquidGroup(GroupCleanPolicy.NNN))
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
		val lTipAppropriate = lTipAvailable.filter(tip => mapModelToTips(tipModel).contains(tip.obj))
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
	
	def getAspiratePolicy(tipState: TipStateL2, wellState: WellStateL2): Option[PipettePolicy] = {
		import PipettePosition._

		val liquid = wellState.liquid
		// Can't aspirate from an empty well
		//assert(liquid ne Liquid.empty)
		if (liquid eq Liquid.empty)
			return None

		val bLarge = (tipState.conf.obj.index < 4)		
		
		val sFamily = liquid.sFamily
		val tipModel = tipState.model_?.get
		val posDefault = WetContact
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
		
		val sPos = "Wet"
		val sTip = if (bLarge) "1000" else "0050"
		val sName = "Roboliq_"+sFamily+"_"+sPos+"_"+sTip
		Some(PipettePolicy(sName, posDefault))
		
	}
	
	val nFreeDispenseVolumeThreshold = 5
	
	def getDispensePolicy(liquid: Liquid, tip: TipConfigL2, nVolume: Double, nVolumeDest: Double): Option[PipettePolicy] = {
		import PipettePosition._

		val sFamily = liquid.sFamily
		val bLarge = (tip.obj.index < 4)
		val tipModel = if (tip.obj.index < 4) tipModel1000 else tipModel50

		val posDefault = {
			// If our volume is high enough that we don't need to worry about accuracy
			if (bLarge && nVolume >= nFreeDispenseVolumeThreshold)
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
	
	def getOtherTipsWhichCanBeCleanedSimultaneously(lTipAll: SortedSet[TipConfigL2], lTipCleaning: SortedSet[TipConfigL2]): SortedSet[TipConfigL2] = {
		val lModel = lTipCleaning.toSeq.map(tip => mapTipToModels(tip.obj))
		val lTip = lTipAll.filter(tip => lModel.contains(mapTipToModels(tip.obj)))
		lTip -- lTipCleaning
	}

}
