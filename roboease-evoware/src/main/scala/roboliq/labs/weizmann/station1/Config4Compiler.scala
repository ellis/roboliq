package roboliq.labs.weizmann.station1

import roboliq.common._
import roboliq.compiler._
import roboliq.devices.move._
import roboliq.devices.pipette._
import roboliq.robots.evoware._
import roboliq.robots.evoware.devices._
import roboliq.labs.weizmann.devices._


class Config4Compiler(evoware: EvowareConfig) extends CompilerConfig {
	private val tipModels = {
		import Config1Models._
		Seq(tipSpec200, tipSpec10, tipSpec20, tipSpec50, tipSpec1000)
		//Seq(tipSpec1000)
	}

	val mover = new EvowareMoveDevice
	val pipetter = new WeizmannPipetteDevice(tipModels)
	
	val devices = Seq(
		mover,
		pipetter
	)
	
	val processors = Seq(
		new L3P_CleanPending(pipetter),
		new L3P_Mix(pipetter),
		new L3P_MovePlate(mover),
		new L3P_Pipette(pipetter),
		//new L3P_Shake_HPShaker("shaker"),
		new L3P_TipsDrop("WASTE"),
		new L3P_TipsReplace
	)
	
	val translator = new EvowareTranslator(evoware)
	
	def createTranslator: Translator = translator
}
