package roboliq.labs.bsse

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._

import _root_.evoware._


class BssePipetteDevice extends PipetteDevice {
	private val tipSpec50 = new TipModel("DiTi 50ul", 50, 0.01, 5, 10)
	//private val tipSpec1000 = new TipModel("DiTi 1000ul", 1000, 2, 950)
	private val tipSpec1000 = new TipModel("DiTi 1000ul", 1000, 3, 50, 100)
	private val tipSpecs = Seq(tipSpec50, tipSpec1000)
	val config = new PipetteDeviceConfig(
		tipSpecs = tipSpecs,
		tips = SortedSet((0 to 7).map(i => new Tip(i)) : _*),
		tipGroups = {
			val g1000 = (0 to 3).map(i => i -> tipSpec1000).toSeq
			val g50 = (4 to 7).map(i => i -> tipSpec50).toSeq
			Seq(g1000, g50)
		}
	)
	val mapTipModels = config.tipSpecs.map(spec => spec.id -> spec).toMap

	val plateDeconAspirate, plateDeconDispense = new Plate
			
	def addKnowledge(kb: KnowledgeBase) = {
		config.tips.foreach(tip => {
			kb.addObject(tip)
			
			val tipSpec = if (tip.index < 4) tipSpec1000 else tipSpec50
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
	
	def getTipAspirateVolumeMin(tip: TipStateL2, liquid: Liquid): Double = {
		tip.model_?.map(_.nVolumeAspirateMin).getOrElse(0.0)
	}
	
	def getTipHoldVolumeMax(tip: TipStateL2, liquid: Liquid): Double = {
		tip.model_?.map(tipModel => {
			val nExtra = WashIntensity.max(tip.cleanDegreePending, liquid.group.cleanPolicy.exit) match {
				case WashIntensity.Decontaminate => tipModel.nVolumeDeconExtra
				case _ => tipModel.nVolumeWashExtra
			}
			tipModel.nVolume - nExtra
		}).getOrElse(0.0)
	}
	
	//def getPipetteSpec(sLiquidClass: String): Option[PipetteSpec] = mapPipetteSpecs.get(sLiquidClass)
	
	def getAspiratePolicy(tipState: TipStateL2, wellState: WellStateL2): Option[PipettePolicy] = {
		val liquid = wellState.liquid
		// Can't aspirate from an empty well
		//assert(liquid ne Liquid.empty)
		if (liquid eq Liquid.empty)
			return None

		val bLarge = (tipState.conf.obj.index < 4)

		/*val res: Option[String] = {
			import PipettePosition._
			if (liquid.contaminants.contains(Contaminant.Cell))
				if (bLarge) Some("Cells") else None
			else if (liquid.contaminants.contains(Contaminant.DSMO))
				if (bLarge) Some("DMSO") else None
			else if (liquid.contaminants.contains(Contaminant.Decon))
				Some("Decon")
			else
				Some("Water")
		}
		res match {
			case None => None
			case Some(sClass) =>*/
		val sClass = liquid.sFamily
				val sPos = "Top"
				val sTip = if (bLarge) "1000" else "0050"
				val sName = "Roboliq_"+sClass+"_"+sPos+"_"+sTip
				Some(PipettePolicy(sName, PipettePosition.WetContact))
		//}
	}
	
	val nFreeDispenseVolumeThreshold = 20
	
	def getDispensePolicy(liquid: Liquid, tip: TipConfigL2, nVolume: Double, nVolumeDest: Double): Option[PipettePolicy] = {
		val sClass = liquid.sFamily
		val bLarge = (tip.obj.index < 4)
		
		val res: Option[PipettePosition.Value] = {
			import PipettePosition._
			sClass match {
				case "Cells" => Some(Free)
				case "DMSO" => Some(Free)
				case "Decon" => Some(Free)
				case _ =>
					// If our volume is high enough that we don't need to worry about accuracy
					if (bLarge && nVolume >= nFreeDispenseVolumeThreshold)
						Some(Free)
					else if (nVolumeDest == 0)
						Some(DryContact)
					else
						Some(WetContact)
			}
		}
		res match {
			case None => None
			case Some(pos) =>
				val sPos = pos match {
					case PipettePosition.Free => "Air"
					case PipettePosition.DryContact => "Dry"
					case PipettePosition.WetContact => "Top"
				}
				val sTip = if (bLarge) "1000" else "0050"
				val sName = "Roboliq_"+sClass+"_"+sPos+"_"+sTip
				Some(PipettePolicy(sName, pos))
		}
	}
	
	def chooseTipWellPairs(tips: SortedSet[Tip], wells: SortedSet[Well], wellPrev_? : Option[Well]): Seq[Tuple2[Tip, Well]] = {
		val tips2 = tips.toIndexedSeq
		val wells2 = wells.toSeq.zipWithIndex
		for ((well, i) <- wells2) yield {
			val i2 = i % tips2.size
			tips2(i2) -> well
		}
	}
	
	private case class KeySpirate(plate: PlateConfigL2, iTipType: Int) {
		def this(item: L2A_SpirateItem) = this(item.well.holder, item.tip.index / 4)
	}
	def batchesForAspirate(items: Seq[L2A_SpirateItem]): Seq[Seq[L2A_SpirateItem]] = {
		items.groupBy(item => new KeySpirate(item)).values.toSeq
	}
	def batchesForDispense(items: Seq[L2A_SpirateItem]): Seq[Seq[L2A_SpirateItem]] = {
		items.groupBy(item => new KeySpirate(item)).values.toSeq
	}
	def batchesForClean(tcs: Seq[Tuple2[TipConfigL2, WashIntensity.Value]]): Seq[Seq[Tuple2[TipConfigL2, WashIntensity.Value]]] = Seq(tcs)
	def batchesForMix(items: Seq[L2A_MixItem]): Seq[Seq[L2A_MixItem]] = {
		items.groupBy(item => KeySpirate(item.well.holder, item.tip.index / 4)).values.toSeq
	}
}
