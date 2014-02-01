package roboliq.roboease

import roboliq.common
import roboliq.common._
import roboliq.commands.pipette.PipettePolicy


case class Rack(
		id: String,
		nRows: Int,
		nCols: Int,
		grid: Int, site: Int, nVolumeMax: Double, carrierType: String
)

case class Labware(
	sLabel: String,
	sType: String,
	rack: Rack
)

/**
 * @param underlying roboliq reagent
 * @param policy Default pipette policy for reagent
 */
case class Reagent(
	id: String,
	reagent: common.Reagent,
	wells: IndexedSeq[Well],
	policy_? : Option[PipettePolicy]
)

case class LineError(file: java.io.File, iLine: Int, iCol_? : Option[Int], sLine: String, sError: String)

case class RoboeaseResult(
	val kb: KnowledgeBase,
	val cmds: Seq[RoboeaseCommand]
)

case class RoboeaseError(
	val kbErrors: CompileStageError,
	val errors: Seq[LineError]
)

case class RoboeaseCommand(iLine: Int, sLine: String, cmd: Command)

// REFACTOR: remove sHeader, because it belongs to Evoware instead 
class Table(val sHeader: String, val racks: Seq[Rack])

class MixDef(val reagent: Reagent, val items: List[Tuple2[Reagent, Double]])

case class CmdLog(cmds: Seq[Command], log: Seq[String])
object CmdLog {
	def apply(cmd: Command): CmdLog = CmdLog(Seq(cmd), Seq())
	def apply(cmds: Seq[Command]): CmdLog = CmdLog(cmds, Seq())
}
object CmdLogImplicits {
	def cmdToCmdLog(o: Command): CmdLog = CmdLog(o)
}