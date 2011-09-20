package roboliq.labs.bsse

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._
import roboliq.robots.evoware._
import roboliq.robots.evoware.devices.EvowarePipetteDevice


class BssePipetteDevice(tipModel50: TipModel, tipModel1000: TipModel) extends EvowarePipetteDevice {
	val config = new PipetteDeviceConfig(
		tipSpecs = Seq(tipModel50, tipModel1000),
		tips = SortedSet((0 to 7).map(i => new Tip(i)) : _*),
		tipGroups = {
			val g1000 = (0 to 3).map(i => i -> tipModel1000).toSeq
			val g50 = (4 to 7).map(i => i -> tipModel50).toSeq
			Seq(g1000, g50)
		}
	)
	val mapTipModels = config.tipSpecs.map(spec => spec.id -> spec).toMap

	val plateDeconAspirate, plateDeconDispense = new Plate
	
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
		new PlateProxy(kb, plateDeconAspirate) match {
			case pp =>
				pp.label = "DA"
				pp.location = "decon2"
				pp.setDimension(8, 1)
				val reagent = new Reagent
				pp.wells.foreach(well => kb.getWellSetup(well).reagent_? = Some(reagent))
				val rs = kb.getReagentSetup(reagent)
				rs.sName_? = Some("Decon")
				rs.sFamily_? = Some("Decon")
				rs.group_? = Some(new LiquidGroup(GroupCleanPolicy.NNN))
		}
		kb.addPlate(plateDeconAspirate, true)
		new PlateProxy(kb, plateDeconDispense) match {
			case pp =>
				pp.label = "DD"
				pp.location = "decon3"
				pp.setDimension(8, 1)
		}
	}

	def areTipsDisposable: Boolean = false
	
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
	
	val nFreeDispenseVolumeThreshold = 20
	
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
}
