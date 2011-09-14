package roboliq.roboease

class RoboeaseParser(config: RoboeaseConfig) extends ParserFile(
		config.mapTables,
		config.mapTipModel,
		config.mapLcToPolicy
	)
