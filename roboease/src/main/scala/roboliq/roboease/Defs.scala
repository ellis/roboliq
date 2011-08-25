package roboliq.roboease

import roboliq.common._


case class Rack(
		name: String,
		nRows: Int,
		nCols: Int,
		grid: Int, site: Int, nVolumeMax: Double, carrierType: String
)

case class LineError(iLine: Int, iCol_? : Option[Int], sLine: String, sError: String)

case class RoboeaseResult(
	val kb: KnowledgeBase,
	val cmds: Seq[RoboeaseCommand]
)

case class RoboeaseError(
	val kbErrors: Seq[Tuple2[Obj, Seq[String]]],
	val errors: Seq[LineError]
)

case class RoboeaseCommand(iLine: Int, sLine: String, cmd: Command)
