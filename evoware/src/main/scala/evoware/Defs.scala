package evoware

case class Site(val iGrid: Int, val iSite: Int)

case class WashProgramArgs(
	iWasteGrid: Int, iWasteSite: Int,
	iCleanerGrid: Int, iCleanerSite: Int,
	nWasteVolume_? : Option[Double],
	nWasteDelay: Int,
	nCleanerVolume: Double,
	nCleanerDelay: Int,
	nAirgapVolume: Int,
	nAirgapSpeed: Int,
	nRetractSpeed: Int,
	bFastWash: Boolean,
	bUNKNOWN1: Boolean = false,
	bEmulateEvolab: Boolean = false
)