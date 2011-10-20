package roboliq.labs.bsse

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._
import roboliq.robots.evoware._

import station1._


object Main extends App {
	val station = new StationConfig
	//val protocol = new examples.Example05(station)
	val protocol = new examples.ExampleOpenhouse(station)
	val toolchain = new BsseToolchain(station)
	toolchain.compileProtocol(protocol, true) match {
		case Left(err) => err.print()
		case Right(succT: TranslatorStageSuccess) =>
			//val sFilename = "example05.esc"
			val sFilename = "example_openhouse.esc"
			val script = succT.internal.asInstanceOf[EvowareScriptBuilder]
			val translator = new EvowareTranslator(toolchain.evowareConfig)
			val s = translator.saveWithHeader(script.cmds.toSeq, station.sHeader, script.mapLocToLabware.toMap, sFilename)
			println(s)
		case Right(succ) => succ.print()
	}
}
