package weizmann

import roboliq.common._
import roboliq.compiler._
import roboliq.devices.pipette._


object WeizmannRobot {
	def apply(): Robot = {
		val plateDeconAspirate, plateDeconDispense = new Plate
			
		val pipetter = new WeizmannPipetteDevice
		val devices = Seq(
			pipetter
			)
		val processors = Seq(
			new L3P_TipsReplace,
			new L3P_TipsDrop("WASTE"),
			new L3P_TipsWash_BSSE(pipetter, pipetter.plateDeconAspirate, pipetter.plateDeconDispense),
			new L3P_Pipette(pipetter),
			new L3P_Mix(pipetter)
			)
			
		new Robot(devices, processors)
	}
}
