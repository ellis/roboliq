package roboliq.labs.weizmann.station1

import roboliq.common
import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.move._
import roboliq.devices.pcr._
import roboliq.devices.pipette._
import roboliq.handlers.system._
import roboliq.robots.evoware._
import roboliq.robots.evoware.devices._
import roboliq.robots.evoware.devices.centrifuge._
import roboliq.robots.evoware.devices.roboseal._
import roboliq.robots.evoware.devices.robopeel._
import roboliq.robots.evoware.devices.trobot._
import roboliq.labs.weizmann._
import roboliq.labs.weizmann.devices._
import roboliq.labs.weizmann.handlers._


class StationConfig extends EvowareTable {
	object TipModels {
		val tipModel10 = new TipModel("DiTi 10ul", 10, 0, 0, 0)
		val tipModel20 = new TipModel("DiTi 20ul", 20, 0, 0, 0)
		val tipModel50 = new TipModel("DiTi 50ul", 50, 1, 0, 0)
		val tipModel200 = new TipModel("DiTi 200ul", 200, 2, 0, 0)
		val tipModel1000 = new TipModel("DiTi 1000ul", 925, 3, 0, 0)
	}
	val tipModels = {
		import TipModels._
		Seq(tipModel200, tipModel1000, tipModel50, tipModel20, tipModel10)
	}

	object CarrierModels {
		// BSSE:
		val wash = new CarrierModel("Wash Station Clean", 3, false)
		val decon = new CarrierModel("decon", 3, false)
		val reagents = new CarrierModel("reagent columns", 2, true)
		val ethanol = new CarrierModel("ethanol", 1, true)
		val holder = new CarrierModel("Downholder", 2, true)
		val shaker = new CarrierModel("MP 2Pos H+P Shake", 1, false)
		val eppendorfs = new CarrierModel("Block 20Pos", 1, true)
		val cooled2 = new CarrierModel("MP 3Pos Cooled 1 PCR", 2, true)
		val cooled3 = new CarrierModel("MP 3Pos Cooled 2 PCR", 3, false)
		val filters = new CarrierModel("Te-VaCS", 2, false)
		val pcr = new CarrierModel("TRobot1", 1, false)
		val sealer = new CarrierModel("RoboSeal", 1, false)
		val peeler = new CarrierModel("RoboPeel", 1, false)
		val centrifuge = new CarrierModel("Centrifuge", 1, false)
		// Weizmann:
		val waste = new CarrierModel("Waste", 7, false)
		
		val mp3pos = new CarrierModel("MP 3Pos Fixed", 3, false)
		val mp3posPcr = new CarrierModel("MP 3Pos Fixed PCR", 3, false)
		val mp3pos2clips = new CarrierModel("MP 3Pos Fixed 2+clips", 3, false)
		
		val hotel5a = new CarrierModel("HOTEL5A", 5, false)
		val hotel5b = new CarrierModel("HOTEL5B", 5, false)
		val roche = new CarrierModel("ROCHE", 1, false)
		val plateReader = new CarrierModel("PLATE_READER", 1, false)
		/*
		DefineRack("CSL",2,0,1,8, 5000000,"Carousel MTP"),
		DefineRack("LNK",65,0,12,8, 200,"Te-Link"),
		DefineRack("S1",53,0,12,8, 200,"Te-Shake 2Pos"),
		DefineRack("S2",53,1,12,8, 1200,"Te-Shake 2Pos"),
		DefineRack("MS1",53,0,24,16, 200,"Te-Shake 2Pos"),
		DefineRack("MS2",53,1,24,16, 1200,"Te-Shake 2Pos"),
		DefineRack("TP1",59,0,12,8, 200,"Torrey pines"),
		DefineRack("TP2",59,1,12,8, 1200,"Torrey pines"),
		*/

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
		val cooled = new CarrierObj("MP 3Pos Cooled 1 PCR", CarrierModels.cooled2, 17)
		val uncooled = new CarrierObj("MP 3Pos Cooled 2 PCR", CarrierModels.cooled3, 24)
		val filters = new CarrierObj("Te-VaCS", CarrierModels.filters, 33)
		val sealer = new CarrierObj("RoboSeal", CarrierModels.sealer, 35)
		val pcr1 = new CarrierObj("TRobot1", CarrierModels.pcr, 40)
		val pcr2 = new CarrierObj("TRobot2", CarrierModels.pcr, 47)
		val centrifuge = new CarrierObj("Centrifuge", CarrierModels.centrifuge, 54)
		// Weizmann:
		val g1 = new CarrierObj("g1", CarrierModels.waste, 1)
		val g23 = new CarrierObj("g23", CarrierModels.mp3pos, 23)
		val g29 = new CarrierObj("g29", CarrierModels.mp3pos, 29)
		val g35 = new CarrierObj("g35", CarrierModels.mp3posPcr, 35)
		val g41a = new CarrierObj("g41a", CarrierModels.mp3pos2clips, 41)
		val g41b = new CarrierObj("g41b", CarrierModels.mp3pos, 41)
		val g47 = new CarrierObj("g47", CarrierModels.mp3pos, 47)
		//val g53
		//val g59
		//val g65
	}
	object Sites {
		def make4(nIndex: Int, carrier: CarrierObj, iSite: Int, liRoma: Seq[Int]): List[SiteObj] = {
			List(
				createSite(carrier, iSite, "P"+nIndex, liRoma),
				createSite(carrier, iSite, "T"+nIndex, liRoma),
				createSite(carrier, iSite, "BUF"+nIndex, liRoma),
				createSite(carrier, iSite, "M"+nIndex, liRoma)
			)
		}
		def make4b(nIndex: Int, carrierA: CarrierObj, carrierB: CarrierObj, iSite: Int, liRoma: Seq[Int]): List[SiteObj] = {
			List(
				createSite(carrierA, iSite, "P"+nIndex, liRoma),
				createSite(carrierA, iSite, "T"+nIndex, liRoma),
				createSite(carrierA, iSite, "BUF"+nIndex, liRoma),
				createSite(carrierB, iSite, "M"+nIndex, liRoma)
			)
		}
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
		val pcr1 = createSite(Carriers.pcr1, 0, "pcr1", Seq(0, 1))
		val pcr2 = createSite(Carriers.pcr2, 0, "pcr2", Seq(0, 1))
		val sealer = createSites(Carriers.sealer, "sealer", Seq(1))
		val peeler = createSites(Carriers.peeler, "peeler", Seq(1))
		val centrifuge = createSites(Carriers.centrifuge, "centrifuge", Seq(0))
		// Weizmann
		createSite(Carriers.g1, 6, "WASTE", Seq())
		make4(2, Carriers.g23, 1, Seq(0, 1))
		make4(3, Carriers.g23, 2, Seq(0, 1))
		make4(4, Carriers.g29, 0, Seq(0, 1))
		make4(5, Carriers.g29, 1, Seq(0, 1))
		make4(6, Carriers.g29, 2, Seq(0, 1))
		createSite(Carriers.g35, 0, "P7", Seq(0, 1))
		createSite(Carriers.g35, 1, "P8", Seq(0, 1))
		createSite(Carriers.g35, 2, "P9", Seq(0, 1))
		make4b(10, Carriers.g41a, Carriers.g41b, 0, Seq(0, 1))
		make4b(11, Carriers.g41a, Carriers.g41b, 1, Seq(0, 1))
		make4b(12, Carriers.g41a, Carriers.g41b, 2, Seq(0, 1))
		make4(13, Carriers.g47, 0, Seq(0, 1))
		make4(14, Carriers.g47, 1, Seq(0, 1))
		make4(15, Carriers.g47, 2, Seq(0, 1))
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
		val test4x3 = new PlateModel("test 4x3 Plate", 4, 3, 500) // FIXME: nVolume?
		// Weizmann:
		val plateDeepWell = new PlateModel("96 Well DeepWell square", 8, 12, 1000) // FIXME: nVolume? 
		
		//"MTP Waste"
	}
	object Labwares {
		val wash1a = new TroughObj("wash1a", LabwareModels.washA, Sites.wash1a)
		val wash1b = new TroughObj("wash1b", LabwareModels.washB, Sites.wash1b)
		val wash1c = new TroughObj("wash1c", LabwareModels.washC, Sites.wash1c)
		val wash2a = new TroughObj("wash2a", LabwareModels.washA, Sites.wash2a)
		val wash2b = new TroughObj("wash2b", LabwareModels.washB, Sites.wash2b)
		val wash2c = new TroughObj("wash2c", LabwareModels.washC, Sites.wash2c)
		val decon1 = new TroughObj("decon1", LabwareModels.decon, Sites.decon1)
		val decon2 = new TroughObj("decon2", LabwareModels.decon, Sites.decon2)
		val decon3 = new TroughObj("decon3", LabwareModels.decon, Sites.decon3)
		val reagents15 = new PlateObj("reagents15", LabwareModels.reagents15, Sites.reagents15)
		val reagents50 = new PlateObj("reagents50", LabwareModels.reagents50, Sites.reagents50)
		val ethanol = new TroughObj("ethanol", LabwareModels.ethanol, Sites.ethanol)
		val eppendorfs = new PlateObj("eppendorfs", LabwareModels.eppendorfs, Sites.eppendorfs)
	}
	
	
	Sites
	LabwareModels
	
	
	val mover = new EvowareMoveDevice
	val pipetter = new WeizmannPipetteDevice(tipModels)
	val sealer = new RoboSealDevice("RoboSeal", """C:\Programme\HJBioanalytikGmbH\RoboSeal3\RoboSeal_PlateParameters\4titude_PCR_blau.bcf""", "sealer")
	val peeler = new RoboPeelDevice("RoboPeel", """C:\Programme\HJBioanalytikGmbH\RoboPeel3\RoboPeel_PlateParameters\4titude_PCR_blau.bcf""", "peeler")
	val trobot1 = new TRobotDevice("TRobot1", "pcr1")
	val centrifuge = new CentrifugeDevice("Centrifuge", "centrifuge", 4)
	
	val devices = Seq(
		mover,
		pipetter,
		sealer,
		peeler,
		trobot1,
		centrifuge
	)
	
	val processors = Seq(
		new L3P_CleanPending(pipetter),
		new L3P_Comment,
		new L3P_Execute,
		new L3P_Mix(pipetter),
		new L3P_MovePlate(mover),
		new L3P_PcrMix,
		new L3P_Pipette(pipetter),
		new L3P_Prompt,
		
		new L3P_TipsDrop("WASTE"),
		new L3P_TipsReplace,
		new L3P_TipsWash_Weizmann,
		
		new L3P_Shake_HPShaker("shaker"),
		new L3P_Seal_RoboSeal(sealer),
		new L3P_Peel_RoboPeel(peeler),
		new L3P_Thermocycle_TRobot(trobot1),
		new L3P_PcrClose(trobot1),
		new L3P_PcrOpen(trobot1),
		new L3P_PcrRun(trobot1),
		new L3P_Centrifuge(centrifuge),
		new L3P_CentrifugeClose(centrifuge),
		new L3P_CentrifugeMoveTo(centrifuge),
		new L3P_CentrifugeOpen(centrifuge),
		new L3P_CentrifugeRun(centrifuge)
	)
	//def addKnowledge

	val mapWashProgramArgs = Map[Int, WashProgramArgs](
		0 -> new WashProgramArgs(
			iWasteGrid = 1, iWasteSite = 1,
			iCleanerGrid = 1, iCleanerSite = 0,
			nWasteVolume_? = Some(2),
			nWasteDelay = 500,
			nCleanerVolume = 1,
			nCleanerDelay = 500,
			nAirgapVolume = 20,
			nAirgapSpeed = 70,
			nRetractSpeed = 30,
			bFastWash = true,
			bUNKNOWN1 = true)
	)

	val sHeader =
""""3A748C70
20100328_144238 apiuser         
                                                                                                                                
                                                                                                                                
--{ RES }--
V;200
--{ CFG }--
999;218;32;
14;-1;104;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;34;-1;-1;-1;-1;-1;34;-1;-1;-1;-1;-1;34;-1;-1;-1;-1;-1;221;-1;-1;-1;-1;-1;222;-1;-1;-1;-1;-1;34;-1;-1;-1;-1;-1;44;-1;-1;-1;-1;-1;216;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;
998;0;
998;8;Washstation 2Grid Cleaner short;Washstation 2Grid Waste;;;;;Washstation 2Grid DiTi Waste;;
998;;;;;;;;;
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
998;3;DiTi 1000ul;DiTi 200ul;DiTi 50ul;
998;1000;200;50;
998;0;
998;0;
998;0;
998;0;
998;0;
998;3;DiTi 20ul;;;
998;20;;;
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
998;3;;;;
998;;;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;3;Block Eppendorf 24 Pos;;6 pos DeepWell trough;
998;T10;;BUF12;
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
998;2;;;
998;;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;2;;;
998;;;
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
998;0;
998;0;
998;4;
998;93;4;
998;93;50;
998;85;66;
998;225;48;
998;11;
998;4;0;System;
998;0;7;Te-Shake 2Pos;
998;0;3;Carousel MTP;
998;0;4;Carousel DiTi 200;
998;0;5;Carousel DiTi 1000;
998;0;8;Carousel DiTi 10;
998;0;6;Te-Link;
998;0;0;Hotel 5Pos SPE;
998;0;1;Hotel 5Pos SPE;
998;0;2;Hotel 5Pos DeepWell;
998;4;1;Reader;
998;1;
998;215;96_Well_Microplate;
998;1;
998;53;
998;2;
998;4;
998;5;
998;3;
998;65;
998;4;
998;50;
998;66;
998;48;
996;0;0;
--{ RPG }--
"""
}