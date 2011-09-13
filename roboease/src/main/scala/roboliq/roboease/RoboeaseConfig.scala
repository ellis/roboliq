package roboliq.roboease

import roboliq.commands.pipette.PipettePolicy
import roboliq.commands.pipette.TipModel


trait RoboeaseConfig {
	val mapTables: Map[String, Table]
	val mapTipModel: Map[String, TipModel]
	val mapLcToPolicy: Map[String, PipettePolicy]
}