package roboliq.commands.pipette

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.pipette._


object PipetteHelperL4 {
	def getWells1(states: RobotState, wpl: WellOrPlateOrLiquid): SortedSet[WellConfigL2] = wpl match {
		case WPL_Well(o) => getWells1(states, o)
		case WPL_Plate(o) => getWells1(states, o)
		case WPL_Liquid(o) => getWells1(states, o)
	}			

	def getWells1(states: RobotState, wpl: WellOrPlate): SortedSet[WellConfigL2] = wpl match {
		case WP_Well(o) => getWells1(states, o)
		case WP_Plate(o) => getWells1(states, o)
	}			

	def getWells1(states: RobotState, well: Well): SortedSet[WellConfigL2] = SortedSet(well.state(states).conf)

	def getWells1(states: RobotState, plate: Plate): SortedSet[WellConfigL2] = {
		SortedSet(plate.state(states).conf.wells.map(well => well.state(states).conf) : _*)
	}
	
	def getWells1(states: RobotState, reagent: Reagent): SortedSet[WellConfigL2] = {
		val liquid: Liquid = reagent.state(states).conf.liquid
		getWells1(states, liquid)
	}
	
	def getWells1(states: RobotState, liquid: Liquid): SortedSet[WellConfigL2] = {
		// Only keep wells with the given initial liquid
		SortedSet(states.filterByValueType[WellStateL2].values.filter(_.liquid eq liquid).map(_.conf).toSeq : _*)
	}
}
