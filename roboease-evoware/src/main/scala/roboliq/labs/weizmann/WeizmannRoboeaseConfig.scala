package roboliq.labs.weizmann

import roboliq.commands.pipette._


object WeizmannRoboeaseConfig {
	val robot = WeizmannSystem()
	val mapTables = WeizmannTables.map
	val mapTipModel = Map(
		"10" -> robot.pipetter.tipSpec10,
		"20" -> robot.pipetter.tipSpec20,
		"50" -> robot.pipetter.tipSpec50,
		"200" -> robot.pipetter.tipSpec200,
		"1000" -> robot.pipetter.tipSpec1000
	)
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