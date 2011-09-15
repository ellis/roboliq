package roboliq.labs.bsse.station1

import roboliq.common
import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.move._
import roboliq.devices.pipette._
import roboliq.robots.evoware._
import roboliq.robots.evoware.devices._
import roboliq.labs.bsse._


class StationConfig extends EvowareTable {
	object TipModels {
		val tipModel50 = new TipModel("DiTi 50ul", 50, 0.01, 5, 10)
		val tipModel1000 = new TipModel("DiTi 1000ul", 1000, 3, 50, 100)
	}
	val tipModels = {
		import TipModels._
		Seq(tipModel50, tipModel1000)
	}

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
	}
	object Carriers {
		val wash1 = new CarrierObj("Wash Station Clean", CarrierModels.wash, 1)
		val wash2 = new CarrierObj("Wash Station Dirty", CarrierModels.wash, 2)
		val decon = new CarrierObj("LI - Trough 3Pos 100ml", CarrierModels.decon, 3)
		val reagents = new CarrierObj("Cooled 8Pos*15ml 8Pos*50ml", CarrierModels.reagents, 4)
		val ethanol = new CarrierObj("Trough 1000ml", CarrierModels.ethanol, 7)
		val holder = new CarrierObj("Downholder", CarrierModels.holder, 9)
		val shaker = new CarrierObj("MP 2Pos H+P Shake", CarrierModels.shaker, 10)
		val eppendorfs = new CarrierObj("Block 20Pos", CarrierModels.eppendorfs, 16)
		val cooled = new CarrierObj("MP 3Pos Cooled 1 PCR", CarrierModels.cooled, 17)
		val uncooled = new CarrierObj("MP 3Pos Cooled 2 PCR", CarrierModels.uncooled, 24)
		val filters = new CarrierObj("Te-VaCS", CarrierModels.filters, 33)
		val pcr1 = new CarrierObj("TRobot1", CarrierModels.pcr, 40)
		val pcr2 = new CarrierObj("TRobot2", CarrierModels.pcr, 47)
	}
	object Sites {
		val (wash1a, wash1b, wash1c) = createSites(Carriers.wash1, "wash1a", "wash1b", "wash1c")
		val (wash2a, wash2b, wash2c) = createSites(Carriers.wash2, "wash2a", "wash2b", "wash2c")
		val (decon1, decon2, decon3) = createSites(Carriers.decon, "decon1", "decon2", "decon3")
		val (reagents15, reagents50) = createSites(Carriers.reagents, "reagents15", "reagents50")
		val ethanol = createSites(Carriers.ethanol, "ethanol")
		val holder = createSites(Carriers.holder, "holder")
		val (cover, shaker) = createSites(Carriers.shaker, "cover", "shaker")
		val eppendorfs = createSites(Carriers.eppendorfs, "eppendorfs")
		val (cooled1, cooled2) = createSites(Carriers.cooled, "cooled1", "cooled2")
		val (uncooled1, uncooled2, uncooled3) = createSites(Carriers.uncooled, "uncooled1", "uncooled2", "uncooled3")
		val (filter1, filter2) = createSites(Carriers.filters, "filter1", "filter2")
		val pcr1 = createSite(Carriers.pcr1, 1, "pcr1")
		val pcr2 = createSite(Carriers.pcr2, 1, "pcr2")
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
		val eppendorfs = new PlateModel("Block 20Pos 1.5 ml Eppendorf", 4, 5, 1500) // FIXME: nVolume?
		val test4x3 = new PlateModel("test 4x3 Plate", 4, 3, 500) // FIXME: nVolume?
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
	
	
	val mover = new BsseMoveDevice
	val pipetter = new BssePipetteDevice(TipModels.tipModel50, TipModels.tipModel1000)
	
	val devices = Seq(
		mover,
		pipetter
	)
	
	val processors = Seq(
		new L3P_CleanPending(pipetter),
		new L3P_Mix(pipetter),
		new L3P_MovePlate(mover),
		new L3P_Pipette(pipetter),
		new L3P_Shake_HPShaker("shaker"),
		new L3P_TipsDrop("WASTE"),
		new L3P_TipsReplace,
		new L3P_TipsWash_BSSE(pipetter, pipetter.plateDeconAspirate, pipetter.plateDeconDispense)
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
				nWasteVolume_? = None,
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
				nWasteVolume_? = None,
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
				nWasteVolume_? = None,
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
				nWasteVolume_? = None,
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
				nWasteVolume_? = None,
				nWasteDelay = nWasteDelay,
				nCleanerVolume = 5, // FIXME: how should this be calculated? -- ellis, 2011-06-16
				nCleanerDelay = nCleanerDelay,
				nAirgapVolume = nAirgapVolume,
				nAirgapSpeed = nAirgapSpeed,
				nRetractSpeed = nRetractSpeed,
				bFastWash = true)
		)
	}
}