package roboliq.robots.evoware

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common.Command


class EvowareScriptBuilder {
	var sHeaderDefault: String = null
	val mapLocToLabware = new HashMap[Tuple2[Int, Int], LabwareItem]
	val cmds = new ArrayBuffer[Command]
}

class EvowareScript(
	val sHeaderDefault: String,
	val mapLocToLabware: Map[Tuple2[Int, Int], LabwareItem],
	val cmds: Seq[Command]
)
