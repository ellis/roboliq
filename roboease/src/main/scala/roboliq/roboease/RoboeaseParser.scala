package roboliq.roboease

class RoboeaseParser(config: RoboeaseConfig) {
	val p = new ParserFile(
		config.mapTables,
		config.mapTipModel,
		config.mapLcToPolicy
	)

	def parse(sSource: String) = p.parse(sSource)
}