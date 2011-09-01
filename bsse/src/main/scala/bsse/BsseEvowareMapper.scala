package bsse

import roboliq.common._
//import roboliq.compiler._
//import roboliq.devices.pipette._
import _root_.evoware._


object BsseEvowareMapper {
	def apply(mapSites: Map[String, Site]): EvowareMapper = {
		// TODO: Set these values depending on the tip kind
		val nWasteDelay = 500
		val nCleanerVolume = 10.0
		val nCleanerDelay = 500
		val nAirgapVolume = 10
		val nAirgapSpeed = 70
		val nRetractSpeed = 30
		val washes: Map[Int, WashProgramArgs] = Map(
				// Light wash, part 1
				1 -> new WashProgramArgs(
					iWasteGrid = 2, iWasteSite = 1,
					iCleanerGrid = 2, iCleanerSite = 2,
					nWasteVolume_? = None,
					nWasteDelay = 500,
					nCleanerVolume = 1,
					nCleanerDelay = 500,
					nAirgapVolume = 20,
					nAirgapSpeed = 70,
					nRetractSpeed = 30,
					bFastWash = true,
					bUNKNOWN1 = true,
					bEmulateEvolab = true),
		val washProgramA
			T0_Wash(
				mTips,
				iWasteGrid = 2, iWasteSite = 1,
				iCleanerGrid = 2, iCleanerSite = 2,
				nWasteVolume = nContamInsideVolume + tipKind.nWashVolumeExtra,
				nWasteDelay = nWasteDelay,
				nCleanerVolume = 4, // FIXME: how should this be calculated? -- ellis, 2011-06-16
				nCleanerDelay = nCleanerDelay,
				nAirgapVolume = nAirgapVolume,
				nAirgapSpeed = nAirgapSpeed,
				nRetractSpeed = nRetractSpeed,
				bFastWash = true
			),
			T0_Wash(
				mTips,
				iWasteGrid = 1, iWasteSite = 1,
				iCleanerGrid = 1, iCleanerSite = 2,
				nWasteVolume = 4, // FIXME: how should this be calculated? -- ellis, 2011-06-16
				nWasteDelay = nWasteDelay,
				nCleanerVolume = 1, // FIXME: how should this be calculated? -- ellis, 2011-06-16
				nCleanerDelay = nCleanerDelay,
				nAirgapVolume = nAirgapVolume,
				nAirgapSpeed = nAirgapSpeed,
				nRetractSpeed = nRetractSpeed,
				bFastWash = false
			)
		
		new EvowareMapper(mapSites, Map(0 -> washProgramArgs0))
	}
}
