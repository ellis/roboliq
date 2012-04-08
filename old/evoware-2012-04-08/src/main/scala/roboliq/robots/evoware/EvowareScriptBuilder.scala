package roboliq.robots.evoware

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

//import roboliq.common.Command


class EvowareScriptBuilder {
	//val mapLocToLabware = new HashMap[CarrierSite, LabwareObject]
	val mapCmdToLabwareInfo = new HashMap[Object, List[Tuple3[CarrierSite, String, LabwareModel]]]
	val cmds = new ArrayBuffer[Object]
}

class EvowareScript(
	val mapCmdToLabwareInfo: Map[Object, List[Tuple3[CarrierSite, String, LabwareModel]]],
	val cmds: Seq[Object]
)
