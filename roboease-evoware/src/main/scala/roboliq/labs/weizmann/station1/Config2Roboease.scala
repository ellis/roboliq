package roboliq.labs.weizmann.station1

import roboliq.commands.pipette._
import roboliq.roboease.RoboeaseConfig


class Config2Roboease extends RoboeaseConfig {
	val mapTables = WeizmannTables.map
	val mapTipModel = {
		import Config1Models._
		Map(
			"10" -> tipSpec10,
			"20" -> tipSpec20,
			"50" -> tipSpec50,
			"200" -> tipSpec200,
			"1000" -> tipSpec1000
		)
	}
	val pipettePolicies = Seq(
		PipettePolicy("PIE_AUTAIR", PipettePosition.Free),
		PipettePolicy("PIE_AUTAIR_LowVol", PipettePosition.Free),
		PipettePolicy("PIE_AUTBOT", PipettePosition.WetContact),
		PipettePolicy("PIE_AUTBOT_SLOW", PipettePosition.WetContact),
		PipettePolicy("PIE_BOTBOT_SLOW", PipettePosition.WetContact),
		PipettePolicy("PIE_TROUGH_AUTAIR", PipettePosition.Free),
		PipettePolicy("PIE_MIX", PipettePosition.WetContact),
		PipettePolicy("PIE_MIX_AUT", PipettePosition.WetContact)
	)
	val mapLcToPolicy = pipettePolicies.map(spec => spec.id -> spec).toMap
}