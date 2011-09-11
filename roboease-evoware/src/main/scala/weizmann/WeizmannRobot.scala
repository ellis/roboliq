package weizmann

import roboliq.common._
import roboliq.compiler._
import roboliq.devices.pipette._


class WeizmannRobot private (
	devices: Seq[Device],
	processors: Seq[CommandCompiler],
	val pipetter: WeizmannPipetteDevice
) extends Robot(devices, processors)

object WeizmannRobot {
	def apply(): WeizmannRobot = {
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
			
		new WeizmannRobot(devices, processors, pipetter)
	}
}
