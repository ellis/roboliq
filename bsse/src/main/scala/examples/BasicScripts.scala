package examples

//import roboliq.common._
//import roboliq.commands.pipette._
//import roboliq.compiler._
//import roboliq.devices.pipette._
import bsse.Protocol

class Example01 extends Protocol with ExampleTable {
	val ddw = new Liquid(LiquidFamily.Water)
	val plate1 = new Plate(PlateFamily.Standard)
	
	pipette(ddw, plate1, 300 ul)
	mix(plate1, 30 ul, 5)

	customize {
		//ddw set (table.BUF12(A1+8))
		plate1.set(PlateModel.Standard96, Location.P4)
	}
}
