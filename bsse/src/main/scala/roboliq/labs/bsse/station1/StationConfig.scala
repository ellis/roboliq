package roboliq.labs.bsse.station1

import roboliq.common
import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.move._
import roboliq.devices.pcr._
import roboliq.devices.pipette._
import roboliq.robots.evoware._
import roboliq.robots.evoware.devices._
import roboliq.robots.evoware.devices.centrifuge._
import roboliq.robots.evoware.devices.roboseal._
import roboliq.robots.evoware.devices.robopeel._
import roboliq.robots.evoware.devices.trobot._
import roboliq.labs.bsse._
import roboliq.labs.bsse.devices._
import roboliq.labs.bsse.handlers._


class StationConfig(configFile: EvowareConfigFile, sFilename: String) extends EvowareTable(configFile, sFilename) {
	object TipModels {
		val tipModel50 = new TipModel("DiTi 50ul", 45, 0.01, 0, 0)
		val tipModel1000 = new TipModel("DiTi 1000ul", 950, 3, 0, 0)
	}
	val tipModels = {
		import TipModels._
		Seq(tipModel50, tipModel1000)
	}

	/*
	object CarrierModels {
		val wash = new CarrierModel("Wash Station Clean", 3, false)
		val decon = new CarrierModel("decon", 3, false)
		val reagents = new CarrierModel("reagent columns", 2, true)
		val ethanol = new CarrierModel("ethanol", 1, true)
		val holder = new CarrierModel("Downholder", 2, true)
		val shaker = new CarrierModel("MP 2Pos H+P Shake", 1, false)
		val eppendorfs = new CarrierModel("Block 20Pos", 1, true)
		val cooled = new CarrierModel("MP 3Pos Cooled 1 PCR", 2, true)
		val uncooled = new CarrierModel("MP 3Pos Cooled 2 PCR", 3, false)
		val filters = new CarrierModel("Te-VaCS", 2, false)
		val pcr = new CarrierModel("TRobot", 1, false)
		val sealer = new CarrierModel("RoboSeal", 1, false)
		val peeler = new CarrierModel("RoboPeel", 1, false)
		val centrifuge = new CarrierModel("Centrifuge", 1, false)
	}
	object Carriers {
		val wash1 = new CarrierObj("Wash Station Clean", CarrierModels.wash, 1)
		val wash2 = new CarrierObj("Wash Station Dirty", CarrierModels.wash, 2)
		val decon = new CarrierObj("LI - Trough 3Pos 100ml", CarrierModels.decon, 3)
		val reagents = new CarrierObj("Cooled 8Pos*15ml 8Pos*50ml", CarrierModels.reagents, 4)
		val ethanol = new CarrierObj("Trough 1000ml", CarrierModels.ethanol, 7)
		val holder = new CarrierObj("Downholder", CarrierModels.holder, 9)
		val shaker = new CarrierObj("MP 2Pos H+P Shake", CarrierModels.shaker, 10)
		val peeler = new CarrierObj("RoboPeel", CarrierModels.peeler, 12)
		val eppendorfs = new CarrierObj("Block 20Pos", CarrierModels.eppendorfs, 16)
		val cooled = new CarrierObj("MP 3Pos Cooled 1 PCR", CarrierModels.cooled, 17)
		val uncooled = new CarrierObj("MP 3Pos Cooled 2 PCR", CarrierModels.uncooled, 24)
		val filters = new CarrierObj("Te-VaCS", CarrierModels.filters, 33)
		val sealer = new CarrierObj("RoboSeal", CarrierModels.sealer, 35)
		//val pcr1 = new CarrierObj("TRobot1", CarrierModels.pcr, 40)
		val pcr2 = new CarrierObj("TRobot2", CarrierModels.pcr, 47)
		val centrifuge = new CarrierObj("Centrifuge", CarrierModels.centrifuge, 54)
	}
	object Sites {
		val (wash1a, wash1b, wash1c) = createSites(Carriers.wash1, "wash1a", "wash1b", "wash1c", Seq())
		val (wash2a, wash2b, wash2c) = createSites(Carriers.wash2, "wash2a", "wash2b", "wash2c", Seq())
		val (decon1, decon2, decon3) = createSites(Carriers.decon, "decon1", "decon2", "decon3", Seq())
		val (reagents15, reagents50) = createSites(Carriers.reagents, "reagents15", "reagents50", Seq())
		val ethanol = createSites(Carriers.ethanol, "ethanol", Seq())
		val holder = createSites(Carriers.holder, "holder", Seq(1))
		val (cover, shaker) = createSites(Carriers.shaker, "cover", "shaker", Seq(0, 1))
		val eppendorfs = createSites(Carriers.eppendorfs, "eppendorfs", Seq())
		val (cooled1, cooled2) = createSites(Carriers.cooled, "cooled1", "cooled2", Seq(0, 1))
		val (cooled3, cooled4, cooled5) = createSites(Carriers.uncooled, "uncooled1", "uncooled2", "uncooled3", Seq(0, 1))
		val (filter1, filter2) = createSites(Carriers.filters, "filter1", "filter2", Seq(0, 1))
		//val pcr1 = createSite(Carriers.pcr1, 0, "pcr1", Seq(0, 1))
		val pcr2 = createSite(Carriers.pcr2, 0, "pcr2", Seq(0, 1))
		val sealer = createSites(Carriers.sealer, "sealer", Seq(1))
		val peeler = createSites(Carriers.peeler, "peeler", Seq(1))
		val centrifuge = createSites(Carriers.centrifuge, "centrifuge", Seq(0))
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
		val List(reagents15, reagents50) = labelLabwares(List("reagents15", "reagents50"), "Cooled 8Pos*15ml 8Pos*50ml")
		val ethanol = labelLabware("ethanol", "Trough 1000ml", 0)
		val holder = labelSite("holder", "Downholder", 0)
		val List(cover, shaker) = labelSites(List("cover", "shaker"), "MP 2Pos H+P Shake")
		val eppendorfs = labelLabware("eppendorfs", "Block 20Pos", 0)
		val List(cooled1, cooled2) = labelSites(List("cooled1", "cooled2"), "MP 3Pos Cooled 1 PCR")
		val List(cooled3, cooled4, cooled5) = labelSites(List("cooled3", "cooled4", "cooled5"), "MP 3Pos Cooled 2 PCR")
		//val (filter1, filter2) = createSites(Carriers.filters, "filter1", "filter2", Seq(0, 1))
		val waste = labelLabware("waste", "Te-VacS", 6)
		val sealer = labelSite("sealer", "RoboSeal", 0)
		val peeler = labelSite("peeler", "RoboPeel", 0)
		val pcr1 = labelSite("pcr1", "TRobot1", 0)
		val pcr2 = labelSite("pcr2", "TRobot2", 0)
		val centrifuge = labelSite("centrifuge", "Centrifuge", 0)
		val regrip = labelSite("regrip", "ReGrip Station", 0)
	}
	
	
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
	//def addKnowledge

	val mapWashProgramArgs: Map[Int, WashProgramArgs] = {
		// TODO: Set these values depending on the tip kind
		val nWasteDelay = 500
		val nCleanerVolume = 10.0
		val nCleanerDelay = 500
		val nAirgapVolume = 10
		val nAirgapSpeed = 70
		val nRetractSpeed = 30
		Map(
			// Light wash, part 1
			1 -> new WashProgramArgs(
				iWasteGrid = 2, iWasteSite = 1,
				iCleanerGrid = 2, iCleanerSite = 2,
				nWasteVolume_? = Some(25),
				nWasteDelay = nWasteDelay,
				nCleanerVolume = 4, // FIXME: how should this be calculated? -- ellis, 2011-06-16
				nCleanerDelay = nCleanerDelay,
				nAirgapVolume = nAirgapVolume,
				nAirgapSpeed = nAirgapSpeed,
				nRetractSpeed = nRetractSpeed,
				bFastWash = true),
			// Light wash, part 2
			2 -> new WashProgramArgs(
				iWasteGrid = 1, iWasteSite = 1,
				iCleanerGrid = 1, iCleanerSite = 2,
				nWasteVolume_? = Some(0.5),
				nWasteDelay = nWasteDelay,
				nCleanerVolume = 1, // FIXME: how should this be calculated? -- ellis, 2011-06-16
				nCleanerDelay = nCleanerDelay,
				nAirgapVolume = nAirgapVolume,
				nAirgapSpeed = nAirgapSpeed,
				nRetractSpeed = nRetractSpeed,
				bFastWash = false),
			// Decontamination, part 1
			5 -> new WashProgramArgs(
				iWasteGrid = 2, iWasteSite = 1,
				iCleanerGrid = 2, iCleanerSite = 2,
				nWasteVolume_? = Some(25),
				nWasteDelay = nWasteDelay,
				nCleanerVolume = 3, // FIXME: how should this be calculated? -- ellis, 2011-06-16
				nCleanerDelay = nCleanerDelay,
				nAirgapVolume = nAirgapVolume,
				nAirgapSpeed = nAirgapSpeed,
				nRetractSpeed = nRetractSpeed,
				bFastWash = true),
			6 -> new WashProgramArgs(
				iWasteGrid = 2, iWasteSite = 1,
				iCleanerGrid = 2, iCleanerSite = 2,
				nWasteVolume_? = Some(25),
				nWasteDelay = nWasteDelay,
				nCleanerVolume = 5, // FIXME: how should this be calculated? -- ellis, 2011-06-16
				nCleanerDelay = nCleanerDelay,
				nAirgapVolume = nAirgapVolume,
				nAirgapSpeed = nAirgapSpeed,
				nRetractSpeed = nRetractSpeed,
				bFastWash = true),
			7 -> new WashProgramArgs(
				iWasteGrid = 1, iWasteSite = 1,
				iCleanerGrid = 1, iCleanerSite = 2,
				nWasteVolume_? = Some(25),
				nWasteDelay = nWasteDelay,
				nCleanerVolume = 5, // FIXME: how should this be calculated? -- ellis, 2011-06-16
				nCleanerDelay = nCleanerDelay,
				nAirgapVolume = nAirgapVolume,
				nAirgapSpeed = nAirgapSpeed,
				nRetractSpeed = nRetractSpeed,
				bFastWash = true)
		)
	}
/*
	val sHeader =
"""00000000
20110101_000000 No log in       
                                                                                                                                
No user logged in                                                                                                               
--{ RES }--
V;200
--{ CFG }--
999;219;32;
14;-1;239;240;130;241;-1;-1;52;-1;242;249;-1;-1;-1;-1;-1;250;243;-1;-1;-1;-1;-1;-1;244;-1;-1;-1;-1;-1;-1;-1;-1;35;-1;-1;-1;-1;-1;-1;234;-1;-1;-1;-1;-1;-1;235;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;246;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;
998;0;
998;3;Wash Station Cleaner shallow;Wash Station Waste;Wash Station Cleaner deep;
998;;;;
998;3;Wash Station Cleaner shallow;Wash Station Waste;Wash Station Cleaner deep;
998;;;;
998;3;;;Trough 25ml;
998;;;Decon;
998;2;Reagent Cooled 8*15ml;Reagent Cooled 8*50ml;
998;Labware5;Labware6;
998;0;
998;0;
998;1;Trough 1000ml;
998;Labware10;
998;0;
998;1;;
998;;
998;2;;;
998;;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;1;;
998;;
998;2;D-BSSE 96 Well PCR Plate;D-BSSE 96 Well PCR Plate;
998;Labware14;Labware15;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;3;;;;
998;;;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;9;;;;;;;MTP Waste;;;
998;;;;;;;Waste;;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;1;D-BSSE 96 Well PCR Plate;
998;Labware12;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;1;D-BSSE 96 Well PCR Plate;
998;Labware13;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;1;;
998;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;5;
998;245;11;
998;93;55;
998;252;54;
998;251;59;
998;253;64;
998;6;
998;4;0;System;
998;0;0;Shelf 32Pos Microplate;
998;0;4;Hotel 5Pos SPE;
998;0;1;Hotel 3Pos DWP;
998;0;2;Hotel 4Pos DWP 1;
998;0;3;Hotel 4Pos DWP 2;
998;0;
998;1;
998;11;
998;55;
998;54;
998;59;
998;64;
996;0;0;
--{ RPG }--"""
*/
}