package bsse

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._

import _root_.evoware._


class BssePipetteDevice extends PipetteDevice {
	private val tipSpec50 = new TipSpec("DiTi 50ul", 50, 0.01, 45)
	//private val tipSpec1000 = new TipSpec("DiTi 1000ul", 1000, 2, 950)
	private val tipSpec1000 = new TipSpec("DiTi 1000ul", 1000, 0, 950)
	private val tipSpecs = Seq(tipSpec50, tipSpec1000)
	val config = new PipetteDeviceConfig(
		tipSpecs = tipSpecs,
		tips = SortedSet((0 to 7).map(i => new Tip(i)) : _*),
		tipGroups = {
			val g1000 = (0 to 3).map(i => i -> tipSpec1000).toSeq
			val g50 = (4 to 7).map(i => i -> tipSpec50).toSeq
			Seq(g1000, g50, g1000 ++ g50)
		}
	)
	private val mapTipSpecs = config.tipSpecs.map(spec => spec.sName -> spec).toMap
	private val mapPipetteSpecs = Seq(
			PipetteSpec("PIE_AUTAIR", PipettePosition.WetContact, PipettePosition.Free, PipettePosition.WetContact),
			PipetteSpec("PIE_AUTAIR_LowVol", PipettePosition.WetContact, PipettePosition.Free, PipettePosition.WetContact),
			PipetteSpec("PIE_AUTBOT", PipettePosition.WetContact, PipettePosition.WetContact, PipettePosition.WetContact),
			PipetteSpec("PIE_TROUGH_AUTAIR", PipettePosition.WetContact, PipettePosition.Free, PipettePosition.WetContact),
			PipetteSpec("PIE_MIX", PipettePosition.WetContact, PipettePosition.WetContact, PipettePosition.WetContact),
			PipetteSpec("PIE_MIX_AUT", PipettePosition.WetContact, PipettePosition.Free, PipettePosition.WetContact)
			).map(spec => spec.sName -> spec).toMap

	def areTipsDisposable: Boolean = false
	def addKnowledge(kb: KnowledgeBase) = {
		config.tips.foreach(kb.addObject)
	}
	def getTipAspirateVolumeMin(tip: TipStateL2, liquid: Liquid): Double = tip.sType_? match { case None => 0; case Some(s) => mapTipSpecs(s).nVolumeAspirateMin }
	def getTipHoldVolumeMax(tip: TipStateL2, liquid: Liquid): Double = tip.sType_? match { case None => 0; case Some(s) => mapTipSpecs(s).nVolumeHoldMax }
	def getPipetteSpec(sLiquidClass: String): Option[PipetteSpec] = mapPipetteSpecs.get(sLiquidClass)
	def getAspiratePolicy(tipState: TipStateL2, wellState: WellStateL2): Option[PipettePolicy] = {
		val liquid = wellState.liquid
		// Can't aspirate from an empty well
		assert(liquid ne Liquid.empty)

		val bLarge = (tipState.conf.obj.index < 4)

		val res: Option[String] = {
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
			case Some(sClass) =>
				val sPos = "Top"
				val sTip = if (bLarge) "1000" else "0050"
				val sName = "Roboliq_"+sClass+"_"+sPos+"_"+sTip
				Some(PipettePolicy(sName, PipettePosition.WetContact))
		}
	}
	
	val nFreeDispenseVolumeThreshold = 20
	
	def getDispensePolicy(tipState: TipStateL2, wellState: WellStateL2, nVolume: Double): Option[PipettePolicy] = {
		val liquid = tipState.liquid
		
		val bLarge = (tipState.conf.obj.index < 4)
		
		val res: Option[Tuple2[String, PipettePosition.Value]] = {
			import PipettePosition._
			if (liquid.contaminants.contains(Contaminant.Cell))
				if (bLarge) Some("Cells" -> Free) else None
			else if (liquid.contaminants.contains(Contaminant.DSMO))
				if (bLarge) Some("DMSO" -> Free) else None
			else if (liquid.contaminants.contains(Contaminant.Decon))
				Some("Decon" -> Free)
			else {
				val sClass = "Water"
				// If our volume is high enough that we don't need to worry about accuracy
				if (bLarge && nVolume >= nFreeDispenseVolumeThreshold)
					Some(sClass -> Free)
				else if (wellState.nVolume == 0)
					Some(sClass -> DryContact)
				else
					Some(sClass -> WetContact)
			}
		}
		res match {
			case None => None
			case Some((sClass, pos)) =>
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
