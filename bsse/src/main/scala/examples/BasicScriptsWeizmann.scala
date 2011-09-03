package examples
/*
import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._

class Rotem_Script01 extends bsse.Roboliq {
	val ddw = new Liquid(LC.Water)
	val p1, p2, p3 = new Plate(PC.Standard)
	
	val wells = p1(G7+9) + p2(A1+64) + p3(A1+35)
	
	pipette(ddw, wells, 300 ul)
	mix(wells, 30 ul, 5)
}

class FixedPlate(val rows: Int, val cols: Int, location: String)
class FixedCarrier(val rows: Int, val cols: Int, location: String)

trait WeizmannTable {
	val table = new {
		object BUF12 extends FixedPlate(8, 1, "BUF12")
		object P4 extends FixedCarrier(8, 12, "P4")
		object P5 extends FixedCarrier(8, 12, "P4")
		object P6 extends FixedCarrier(8, 12, "P4")
	}
}

class My_Rotem_Script01 extends Rotem_Script01 with WeizmannTable {
	customize {
		ddw set (table.BUF12(A1+8))
		p1 set (table.P4, "96 Well DeepWell square")
		p2 set (table.P5, "96 Well DeepWell square")
		p3 set (table.P6, "96 Well DeepWell square")
	}
}
*/