package roboliq.robots.evoware

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.core.ObjBase


class EvowareScriptBuilder(val ob: ObjBase) {
	val cmds = new ArrayBuffer[Object]
	val mapCmdToLabwareInfo = new HashMap[Object, List[Tuple3[CarrierSite, String, LabwareModel]]]
	val state = new EvowareState
	
	def toImmutable: EvowareScript =
		new EvowareScript(cmds.toSeq, mapCmdToLabwareInfo.toMap, state)
}

class EvowareScript(
	val cmds: Seq[Object],
	val mapCmdToLabwareInfo: Map[Object, List[Tuple3[CarrierSite, String, LabwareModel]]],
	val state: EvowareState
)
