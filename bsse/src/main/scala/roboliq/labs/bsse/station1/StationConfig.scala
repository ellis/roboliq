package roboliq.labs.bsse.station1

import roboliq.core
import roboliq.robots.evoware._


class StationConfig(configFile: EvowareConfigFile, sFilename: String) extends EvowareTable(configFile, sFilename) {
	object Locations {
		val List(trough1, trough2, trough3) = labelSites(List("trough1", "trough2", "trough3"), "LI - Trough 3Pos 100ml")
		val List(reagents15, reagents50) = labelSites(List("reagents15", "reagents50"), "Cooled 8Pos*15ml 8Pos*50ml")
		val ethanol = labelSite("ethanol", "Trough 1000ml", 0)
		val holder = labelSite("holder", "Downholder", 0)
		val List(uncooled2_high, uncooled_2_low, _, shaker) = labelSites(List("uncooled2_high", "uncooled2_low", "shaker_bad", "shaker"), "MP 2Pos H+P Shake")
		val eppendorfs = labelSite("reagents1.5", "Block 20Pos", 0)
		val List(cooled1, cooled2) = labelSites(List("cooled1", "cooled2"), "MP 3Pos Cooled 1 PCR")
		val List(cooled3, cooled4, cooled5) = labelSites(List("cooled3", "cooled4", "cooled5"), "MP 3Pos Cooled 2 PCR")
		//val (filter1, filter2) = createSites(Carriers.filters, "filter1", "filter2", Seq(0, 1))
		val waste = labelSite("waste", "Te-VacS", 6)
		val sealer = labelSite("sealer", "RoboSeal", 0)
		val peeler = labelSite("peeler", "RoboPeel", 0)
		val pcr1 = labelSite("pcr1", "TRobot1", 0)
		val pcr2 = labelSite("pcr2", "TRobot2", 0)
		val centrifuge = labelSite("centrifuge", "Centrifuge", 0)
		val regrip = labelSite("regrip", "ReGrip Station", 0)
		val reader = labelSite("reader", "Infinite M200", 0)
	}
	// HACK: force evaluation of Locations
	Locations.toString()
}
