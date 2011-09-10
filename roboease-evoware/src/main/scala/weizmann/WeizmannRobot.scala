package weizmann

import roboliq.common._
import roboliq.compiler._
import roboliq.devices.pipette._


object WeizmannRobot {
	def apply(): Robot = {
		val pipetter = new WeizmannPipetteDevice
		val devices = Seq(
			pipetter
			)
		val processors = Seq(
			new L3P_CleanPending(pipetter),
			new L3P_Mix(pipetter),
			new L3P_Pipette(pipetter),
			new L3P_TipsReplace,
			new L3P_TipsDrop("WASTE")
			)
			
		new Robot(devices, processors)
	}
}
