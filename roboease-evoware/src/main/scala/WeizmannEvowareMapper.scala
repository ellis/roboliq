import roboliq.common._
import roboliq.compiler._
import roboliq.devices.pipette._
import roboliq.roboease._
import _root_.evoware._


object WeizmannEvowareMapper {
	def apply(racks: Iterable[Rack]): EvowareMapper = {
		val mapSites = racks.map(rack => rack.name -> Site(rack.grid, rack.site)).toMap

		val washProgramArgs0 = new WashProgramArgs(
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
			bUNKNOWN1 = true,
			bEmulateEvolab = true
		)	
		
		new EvowareMapper(mapSites, Map(0 -> washProgramArgs0))
	}
}