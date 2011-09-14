package roboliq.labs.weizmann.station1

import roboliq.common._
import roboliq.commands.pipette._


object Config1Models {
	val tipSpec10 = new TipModel("DiTi 10ul", 10, 0, 0, 0)
	val tipSpec20 = new TipModel("DiTi 20ul", 20, 0, 0, 0)
	val tipSpec50 = new TipModel("DiTi 50ul", 50, 1, 0, 0)
	val tipSpec200 = new TipModel("DiTi 200ul", 200, 2, 0, 0)
	//private val tipSpec1000 = new TipModel("DiTi 1000ul", 1000, 3, 960)
	val tipSpec1000 = new TipModel("DiTi 1000ul", 925, 0, 0, 0)
}