package roboliq.devices.pipette

import roboliq.common._
import roboliq.commands.pipette._


object PipetteHelperL3 {
	def getWells1(states: RobotState, wpl: WellOrPlateOrLiquid): Set[WellConfigL1] = wpl match {
		case WPL_Well(o) => getWells1(states, o)
		case WPL_Plate(o) => getWells1(states, o)
		case WPL_Liquid(o) => getWells1(states, o)
	}			

	def getWells1(states: RobotState, wpl: WellOrPlate): Set[WellConfigL1] = wpl match {
		case WP_Well(o) => getWells1(states, o)
		case WP_Plate(o) => getWells1(states, o)
	}			

	def getWells1(states: RobotState, well: Well): Set[WellConfigL1] = Set(well.state(states).conf)

	def getWells1(states: RobotState, plate: Plate): Set[WellConfigL1] = {
		plate.state(states).conf.wells.map(well => well.state(states).conf).toSet
	}
	
	def getWells1(states: RobotState, liquid: Liquid): Set[WellConfigL1] = {
		// Only keep wells with the given initial liquid
		states.filterByValueType[WellStateL1].values.filter(_.liquid eq liquid).map(_.conf).toSet
	}
}
