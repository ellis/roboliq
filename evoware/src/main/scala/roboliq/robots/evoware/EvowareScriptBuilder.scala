package roboliq.robots.evoware

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common.Command


class EvowareScriptBuilder {
	//val mapLocToLabware = new HashMap[CarrierSite, LabwareObject]
	val mapCmdToLabwareInfo = new HashMap[Command, List[Tuple3[CarrierSite, String, LabwareModel]]]
	val cmds = new ArrayBuffer[Command]
}

class EvowareScript(
	//val tableFile: EvowareTableFile,
	val mapCmdToLabwareInfo: Map[Command, List[Tuple3[CarrierSite, String, LabwareModel]]],
	val cmds: Seq[Command]
)
