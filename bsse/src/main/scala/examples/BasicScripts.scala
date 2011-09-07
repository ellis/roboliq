package examples

//import roboliq.common._
//import roboliq.commands.pipette._
//import roboliq.compiler._
//import roboliq.devices.pipette._
import bsse.Protocol

class Example01 extends Protocol {
	val ddw = new Liquid(LiquidFamily.Water)
	val plate1 = new Plate(PlateFamily.Standard)
	
	pipette(ddw, plate1, 30 ul)
	mix(plate1, 30 ul, 5)

	val lab = new EvowareLab with ExampleTable2 {
		reagent(ddw, Labwares.reagents15, 1, 8)
		labware(plate1, Sites.cooled1, LabwareModels.platePcr)
	}
}
