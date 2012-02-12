package roboliq.labs.bsse

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._
import roboliq.robots.evoware._

import station1._


object Main extends App {
	val sHome = System.getProperty("user.home")
	val configFile = new EvowareConfigFile(sHome+"/tmp/tecan/carrier.cfg")
	val station = new StationConfig(configFile, sHome+"/src/roboliq/ellis_pcr1_corrected.esc")
	//val protocol = new examples.Example01(station)
	//val protocol = new examples.Example05(station)
	//val protocol = new examples.ExampleOpenhouse3(station)
	val protocol = new examples.PcrExample4
	val toolchain = new BsseToolchain(station)
	val (kb, cmds) = examples.ExampleRunner.run(protocol.l)
	toolchain.compile(kb, cmds, true) match {
		case Left(err) => err.print()
		case Right(succT: TranslatorStageSuccess) =>
			val sFilename = "ellis_pcr4.esc"
			val script = succT.internal.asInstanceOf[EvowareScriptBuilder]
			val translator = new EvowareTranslator(toolchain.evowareConfig)
			val s = translator.saveWithHeader(script.cmds.toSeq, station.tableFile, script.mapCmdToLabwareInfo.toMap, sFilename)
			println(s)
		case Right(succ) => succ.print()
	}
}
