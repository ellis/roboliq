package roboliq.labs.bsse

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._
import roboliq.robots.evoware._

import station1._


object Main extends App {
	val configFile = new EvowareConfigFile("/home/ellisw/tmp/tecan/carrier.cfg")
	val station = new StationConfig(configFile, "/home/ellisw/src/roboliq/ellis_pcr1_corrected.esc")
	//val protocol = new examples.Example01(station)
	//val protocol = new examples.Example05(station)
	//val protocol = new examples.ExampleOpenhouse3(station)
	val protocol = new examples.PcrScript3(station)
	val toolchain = new BsseToolchain(station)
	toolchain.compileProtocol(protocol, true) match {
		case Left(err) => err.print()
		case Right(succT: TranslatorStageSuccess) =>
			val sFilename = "ellis_pcr3.esc"
			//val sFilename = "example01.esc"
			val script = succT.internal.asInstanceOf[EvowareScriptBuilder]
			val translator = new EvowareTranslator(toolchain.evowareConfig)
			val s = translator.saveWithHeader(script.cmds.toSeq, station.sHeader, script.mapLocToLabware.toMap, sFilename)
			println(s)
		case Right(succ) => succ.print()
	}
}
