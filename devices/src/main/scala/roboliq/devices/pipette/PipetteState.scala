package roboliq.devices.pipette

import scala.collection
import scala.collection.immutable
import scala.collection.mutable
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.devices._


class PipetteStateHelper(val states: RobotState) {
	val builder = new StateBuilder(states)
	
	// Aspirate liquid into the tip -- prefer aspirate()
	def addLiquid0(tip: Tip, liquid: Liquid, nVolume: Double) {
		tip.stateWriter(builder).aspirate(liquid, nVolume)
	}

	// Remove liquid from well and put it in tip
	def aspirate(twv: TipWellVolume) {
		import twv._
		val liquid = getWellState(well).liquid
		addLiquid0(tip, liquid, nVolume)
		removeLiquid0(well, nVolume)
	}

	// Remove liquid from tip -- prefer dispense()
	def removeLiquid0(tip: Tip, nVolume: Double) {
		tip.stateWriter(builder).dispenseFree(nVolume)
	}

	// Remove liquid from tip and put it in well
	def dispense(twvp: TipWellVolumePolicy) {
		import twvp._
		val tipState = tip.state(states)
		val wellState = well.obj.state(states)
		tip.stateWriter(builder).dispense(nVolume, wellState.liquid, policy.pos)
		addLiquid0(well, tipState.liquid, nVolume)
	}
	
	/*def mix(twv: TipWellVolume) {
		aspirate(twv)
		val twvd = new TipWellVolumePolicy(twv.tip, twv.well, twv.nVolume, PipettePolicy(PipettePosition.WetContact))
		dispense(twvd)
	}*/

	def addLiquid0(well: Well, liquid: Liquid, nVolume: Double) {
		wellStates(well) = getWellState(well).add(liquid, nVolume)
	}
	def removeLiquid0(well: Well, nVolume: Double) {
		wellStates(well) = getWellState(well).remove(nVolume)
	}

	def fillWells(wells: Traversable[Well], liquid: Liquid, nVolume: Double) {
		for (well <- wells)
			addLiquid0(well, liquid, nVolume)
	}
	
	def cleanTip(tip: Tip, cleanDegree: CleanDegree.Value) {
		tipStates(tip) = getTipState(tip).clean(cleanDegree)
	}

	def toImmutable = new RobotState(prev_?, mapPartToSite.toMap, tipStates.toMap, wellStates.toMap)
}
