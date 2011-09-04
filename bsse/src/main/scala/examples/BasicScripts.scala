package examples

//import roboliq.common._
//import roboliq.commands.pipette._
//import roboliq.compiler._
//import roboliq.devices.pipette._
import bsse.Protocol

class Example01 extends Protocol with ExampleTable {
	val ddw = new Liquid(LiquidFamily.Water)
	val plate1 = new Plate(PlateFamily.Standard)
	
	pipette(ddw, plate1, 30 ul)
	mix(plate1, 30 ul, 5)

	val table = new {
		val BUF12 = new Plate(PlateFamily.Standard)
		BUF12.label = "BUF12"
		BUF12.location = "BUF12"
		BUF12.setDimension(8, 1)
	}
	
	customize {
		//reagent(ddw, "BUF12", 1, 8)
		//labware(plate1, Location.P4, )
		ddw.fill(table.BUF12)
		plate1.set(PlateModel.Standard96, Location.P4)
	}
}
