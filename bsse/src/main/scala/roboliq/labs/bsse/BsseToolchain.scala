package roboliq.labs.bsse

import roboliq.common._
import roboliq.compiler._
import roboliq.devices.move._
import roboliq.devices.pipette._
import roboliq.robots.evoware._
import roboliq.robots.evoware.devices._
import roboliq.labs.bsse._

class BsseToolchain(stationConfig: station1.StationConfig) extends EvowareToolchain { toolchain =>
	def devices = stationConfig.devices
	def processors = stationConfig.processors
	val evowareConfig = new EvowareConfig(
		stationConfig.tableFile,
		stationConfig.mapLabelToSite,
		stationConfig.mapLabelToLabware)
	val compilerConfig = new CompilerConfig {
		val devices = stationConfig.devices
		val processors = stationConfig.processors
		def createTranslator: Translator = new EvowareTranslator(evowareConfig)
	}
}