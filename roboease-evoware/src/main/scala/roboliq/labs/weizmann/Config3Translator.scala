package roboliq.labs.weizmann

import roboliq.robots.evoware._


object Config3Translator {
	val mapWashProgramArgs = Map(
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

	def apply(sites: Iterable[SiteObj]): EvowareConfig = {
		new EvowareConfig(sites, mapWashProgramArgs)
	}
}