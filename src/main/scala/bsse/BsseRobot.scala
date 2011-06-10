package bsse

import roboliq.parts._
import roboliq.robot._
import roboliq.tokens._

import evoware._


//sealed class BsseTip(val roboTip: Tip)

class BsseRobot extends EvowareRobot {
	val nFreeDispenseVolumeThreshold = 10
	val tipKind1000 = new EvowareTipKind("large", 2, 950, 50)
	val tipKind50 = new EvowareTipKind("small", 0.01, 45, 5)
	val tips = (0 until 8).map(new Tip(_))
	val tipTipKinds = (0 until 8).map(iTip => if (iTip < 4) tipKind1000 else tipKind50)
	
	def getTipKind(tip: Tip): EvowareTipKind = tipTipKinds(tip.index)
	
	def getTipAspirateVolumeMin(tip: Tip, liquid: Liquid): Double = {
		val tipKind = getTipKind(tip)
		tipKind.nAspirateVolumeMin
	}
	
	def getTipHoldVolumeMax(tip: Tip, liquid: Liquid): Double = {
		val tipKind = getTipKind(tip)
		val nReduce =
			if (liquid.contaminates)
				tipKind.nDecontaminateVolumeExtra
			else
				0
		tipKind.nHoldVolumeMax - nReduce
	}
	
	def getDispenseKind(tip: Tip, liquid: Liquid, nVolume: Double, wellState: WellState): DispenseKind.Value = {
		// If our volume is high enough that we don't need to worry about accuracy,
		// or if we're pipetting competent cells,
		// then perform a free dispense.
		if (nVolume >= nFreeDispenseVolumeThreshold || liquid.bCells)
			DispenseKind.Free
		else if (wellState.nVolume == 0)
			DispenseKind.DryContact
		else
			DispenseKind.WetContact
	}
	
	def getAspirateClass(tip: Tip, well: Well): Option[String] = {
		val tipKind = getTipKind(tip)
		val wellState = state.getWellState(well)
		val liquid = wellState.liquid
		
		val bLarge = (tipKind.sName == "large")
		//val tipState = state.getTipState(tip)
		//val tipLiquid = tipState.liquid
		// If our volume is high enough that we don't need to worry about accuracy,
		// or if we're pipetting competent cells,
		// then perform a free dispense.
		if (liquid.bCells)
			if (bLarge) Some("Comp cells free dispense") else None
		else if (liquid.sName.contains("DMSO"))
			if (bLarge) Some("DMSO free dispense") else None
		else if (liquid.sName.contains("D-BSSE Decon"))
			Some("D-BSSE Decon")
		else
			if (bLarge) Some("Water wet contact") else Some("D-BSSE Te-PS Wet Contact")
	}
	
	def getDispenseClass(tip: Tip, well: Well, nVolume: Double): Option[String] = {
		val tipKind = getTipKind(tip)
		val wellState = state.getWellState(well)
		val liquid = wellState.liquid
		
		val bLarge = (tipKind.sName == "large")
		//val tipState = state.getTipState(tip)
		//val tipLiquid = tipState.liquid
		// If our volume is high enough that we don't need to worry about accuracy,
		// or if we're pipetting competent cells,
		// then perform a free dispense.
		if (liquid.bCells)
			if (bLarge) Some("Comp cells free dispense") else None
		else if (liquid.sName.contains("DMSO"))
			if (bLarge) Some("DMSO free dispense") else None
		else if (liquid.sName.contains("D-BSSE Decon"))
			Some("D-BSSE Decon")
		// If our volume is high enough that we don't need to worry about accuracy
		else if (nVolume >= nFreeDispenseVolumeThreshold)
			if (bLarge) Some("Comp cells free dispense") else None
		else if (wellState.nVolume == 0)
			if (bLarge) Some("Water dry contact") else Some("D-BSSE Te-PS Dry Contact")
		else
			if (bLarge) Some("Water wet contact") else Some("D-BSSE Te-PS Wet Contact")
	}
}
