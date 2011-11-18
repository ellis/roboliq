package roboliq.roboease

class RoboeaseParser(
	dirProc: java.io.File,
	dirLog: java.io.File,
	config: RoboeaseConfig
) extends ParserFile(
	dirProc,
	dirLog,
	config.mapTables,
	config.mapTipModel,
	config.mapLcToPolicy,
	config.mapPlateModel
)
