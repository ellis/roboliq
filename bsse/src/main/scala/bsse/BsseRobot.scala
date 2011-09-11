package bsse

import roboliq.common._
import roboliq.compiler._
import roboliq.devices.pipette._
import evoware.EvowareSystem
import evoware.SiteObj
import evoware.WashProgramArgs


class BsseSystem(val sites: Iterable[SiteObj]) extends RoboliqSystem with EvowareSystem {
	val pipetter = new BssePipetteDevice
	
	val devices = Seq(
		pipetter
	)
	
	val processors = Seq(
		new L3P_CleanPending(pipetter),
		new L3P_Mix(pipetter),
		new L3P_Pipette(pipetter),
		new L3P_TipsDrop("WASTE"),
		new L3P_TipsReplace,
		new L3P_TipsWash_BSSE(pipetter, pipetter.plateDeconAspirate, pipetter.plateDeconDispense)
	)

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
