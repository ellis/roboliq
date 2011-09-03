package examples

import roboliq.common
import roboliq.common._


trait ExampleTable {
	//val table = new {
		//object BUF12 extends roboliq.protocol.PlateFixedPlate(8, 1, "BUF12")
	//}
	object Location {
		val P4 = "P4"
		val P5 = "P5"
		val P6 = "P6"
	}
	object PlateModel {
		val Standard96 = new common.PlateModel("D-BSSE 96 Deep Well Plate", 8, 12, 20000)
	}
}
