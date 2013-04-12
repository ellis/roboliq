package roboliq.labs.weizmann.station1

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.move._
import roboliq.devices.pipette._
import roboliq.robots.evoware._
import roboliq.robots.evoware.devices._
import roboliq.labs.weizmann.devices._


/*class WeizmannToolchain(
	val compilerConfig: CompilerConfig,
	val evowareConfig: EvowareConfig
) extends EvowareToolchain
*/

class WeizmannToolchain(station: StationConfig) extends EvowareToolchain { toolchain =>
	def devices = station.devices
	def processors = station.processors
	val evowareConfig = new EvowareConfig(station.sites, station.mapWashProgramArgs)
	val compilerConfig = new CompilerConfig {
		val devices = station.devices
		val processors = station.processors
		def createTranslator: Translator = new EvowareTranslator(evowareConfig)
	}
}