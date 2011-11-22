package roboliq.labs.weizmann.station1

import roboliq.commands.pipette._
import roboliq.roboease._
import roboliq.robots.evoware.roboeaseext._


class Config2Roboease(stationConfig: StationConfig) extends RoboeaseConfig {
	val mapTables2 = Map[String, EvowareRoboeaseTable](
		"TABLE_DNE" -> Table_DNE
	)
	
	val mapTables = mapTables2.mapValues(_.roboeaseTable)
	
	val mapTipModel = {
		import stationConfig.TipModels._
		Map(
			"10" -> tipModel10,
			"20" -> tipModel20,
			"50" -> tipModel50,
			"200" -> tipModel200,
			"1000" -> tipModel1000
		)
	}
	val pipettePolicies = Seq(
		PipettePolicy("PIE_AUTAIR", PipettePosition.Free),
		PipettePolicy("PIE_AUTAIR_LowVol", PipettePosition.Free),
		PipettePolicy("PIE_AUTAIR_PCR", PipettePosition.Free),
		PipettePolicy("PIE_AUTBOT", PipettePosition.WetContact),
		PipettePolicy("PIE_AUTBOT_SLOW", PipettePosition.WetContact),
		PipettePolicy("PIE_BOTBOT_SLOW", PipettePosition.WetContact),
		PipettePolicy("PIE_TROUGH_AUTAIR", PipettePosition.Free),
		PipettePolicy("PIE_MIX", PipettePosition.WetContact),
		PipettePolicy("PIE_MIX_AUT", PipettePosition.WetContact)
	)
	val mapLcToPolicy = pipettePolicies.map(spec => spec.id -> spec).toMap
	val mapPlateModel = List(stationConfig.LabwareModels.plateDeepWell, stationConfig.LabwareModels.plateBiorad).map(o => o.id -> o).toMap
}