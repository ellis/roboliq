package roboliq.labs.weizmann

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.move._
import roboliq.devices.pipette._
import roboliq.robots.evoware._
import roboliq.robots.evoware.devices._
import roboliq.labs.weizmann.devices._


class WeizmannToolchain(
	val compilerConfig: CompilerConfig,
	val evowareConfig: EvowareConfig
) extends EvowareToolchain