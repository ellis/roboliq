package roboliq.labs.bsse.station1

import roboliq.core
import roboliq.core._
import roboliq.commands.pipette._
import roboliq.robots.evoware._
import roboliq.labs.bsse._
import roboliq.labs.bsse.devices._
import roboliq.labs.bsse.handlers._


class StationConfig(configFile: EvowareConfigFile, sFilename: String) extends EvowareTable(configFile, sFilename) {

	/*
	object CarrierModels {
		val wash = new CarrierModel("Wash Station Clean", 3, false)
		val decon = new CarrierModel("decon", 3, false)
	}
	object Carriers {
		val wash1 = new CarrierObj("Wash Station Clean", CarrierModels.wash, 1)
		val wash2 = new CarrierObj("Wash Station Dirty", CarrierModels.wash, 2)
		val decon = new CarrierObj("LI - Trough 3Pos 100ml", CarrierModels.decon, 3)
	}
	object Sites {
		val (wash1a, wash1b, wash1c) = createSites(Carriers.wash1, "wash1a", "wash1b", "wash1c", Seq())
		val (wash2a, wash2b, wash2c) = createSites(Carriers.wash2, "wash2a", "wash2b", "wash2c", Seq())
		val (decon1, decon2, decon3) = createSites(Carriers.decon, "decon1", "decon2", "decon3", Seq())
	}
	object LabwareModels {
		val washA = new TroughModel("Wash Station Cleaner shallow", 8, 1)
		val washB = new TroughModel("Wash Station Waste", 8, 1)
		val washC = new TroughModel("Wash Station Cleaner deep", 8, 1)
		val decon = new TroughModel("Trough 100ml", 8, 1)
		val reagents15 = new PlateModel("Reagent Cooled 8*15ml", 8, 1, 15000)
		val reagents50 = new PlateModel("Reagent Cooled 8*50ml", 8, 1, 50000)
		val ethanol = new TroughModel("Trough 1000ml", 8, 1)
		val platePcr = new PlateModel("D-BSSE 96 Well PCR Plate", 8, 12, 500) // FIXME: nVolume? 
		val plateCostar = new PlateModel("D-BSSE 96 Well Costar Plate", 8, 12, 500) // FIXME: nVolume? 
		val eppendorfs = new PlateModel("Block 20Pos 1.5 ml Eppendorf", 4, 5, 1500) // FIXME: nVolume?
		val test4x3 = new PlateModel("test 4x3 Plate", 4, 3, 500)
		val test8x2 = new PlateModel("test 8x2 Plate", 8, 2, 500)
		//"MTP Waste"
	}
	*/
	object Locations {
		val List(trough1, trough2, trough3) = labelSites(List("trough1", "trough2", "trough3"), "LI - Trough 3Pos 100ml")
		val List(reagents15, reagents50) = labelSites(List("reagents15", "reagents50"), "Cooled 8Pos*15ml 8Pos*50ml")
		val ethanol = labelSite("ethanol", "Trough 1000ml", 0)
		val holder = labelSite("holder", "Downholder", 0)
		val List(cover, shaker) = labelSites(List("cover", "shaker"), "MP 2Pos H+P Shake")
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
	
	/*
	val mover = new EvowareMoveDevice
	val pipetter = new BssePipetteDevice(TipModels.tipModel50, TipModels.tipModel1000)
	val sealer = new RoboSealDevice("RoboSeal", """C:\Programme\HJBioanalytikGmbH\RoboSeal3\RoboSeal_PlateParameters\4titude_PCR_blau.bcf""", "sealer")
	val peeler = new RoboPeelDevice("RoboPeel", """C:\Programme\HJBioanalytikGmbH\RoboPeel3\RoboPeel_PlateParameters\4titude_PCR_blau.bcf""", "peeler")
	//val trobot1 = new TRobotDevice("TRobot1", "pcr1")
	val trobot2 = new TRobotDevice("TRobot2", "pcr2")
	val centrifuge = new CentrifugeDevice("Centrifuge", "centrifuge", 4)
	
	val devices = Seq(
		mover,
		pipetter,
		sealer,
		peeler,
		trobot2,
		centrifuge
	)
	
	val processors = Seq(
		new L3P_CleanPending(pipetter),
		new L3P_Mix(pipetter),
		new L3P_MovePlate(mover),
		new L3P_PcrMix,
		new L3P_Pipette(pipetter),
		new L3P_TipsDrop("WASTE"),
		new L3P_TipsReplace,
		new L3P_TipsWash_BSSE(pipetter, pipetter.plateDecon),
		
		new L3P_Shake_HPShaker("shaker"),
		new L3P_Seal_RoboSeal(sealer),
		new L3P_Peel_RoboPeel(peeler),
		new L3P_Thermocycle_TRobot(trobot2),
		new L3P_PcrClose(trobot2),
		new L3P_PcrOpen(trobot2),
		new L3P_PcrRun(trobot2),
		new L3P_Centrifuge(centrifuge),
		new L3P_CentrifugeClose(centrifuge),
		new L3P_CentrifugeMoveTo(centrifuge),
		new L3P_CentrifugeOpen(centrifuge),
		new L3P_CentrifugeRun(centrifuge)
	)
	*/
}
