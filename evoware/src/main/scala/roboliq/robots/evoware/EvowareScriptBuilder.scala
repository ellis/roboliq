package roboliq.robots.evoware

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.core.ObjBase


class EvowareScriptBuilder(val ob: ObjBase) {
	val mapCmdToLabwareInfo = new HashMap[Object, List[Tuple3[CarrierSite, String, LabwareModel]]]
	val cmds = new ArrayBuffer[Object]
	
	def toImmutable: EvowareScript =
		new EvowareScript(mapCmdToLabwareInfo.toMap, cmds.toSeq)
}

class EvowareScript(
	val mapCmdToLabwareInfo: Map[Object, List[Tuple3[CarrierSite, String, LabwareModel]]],
	val cmds: Seq[Object]
)
