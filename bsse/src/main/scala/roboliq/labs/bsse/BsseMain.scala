package roboliq.labs.bsse

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._

import station1._


object Main extends App {
	val station = new StationConfig
	val protocol = new examples.Example03(station)
	val toolchain = new BsseToolchain(station)
	toolchain.compileProtocol(protocol, true) match {
		case Left(err) => err.print()
		case Right(succT: TranslatorStageSuccess) =>
			//val sFilename = sSourcePath + ".esc"
			val sFilename = "example03.esc"
			//case class LabwareItem(sLabel: String, sType: String, iGrid: Int, iSite: Int)
			def toLabwareItem(plate): roboliq.robots.evoware.LabwareItem = {
				LabwareItem(a.sLabel, a.sType, a.rack.grid, a.rack.site)
			}
			//val mapLabware = EvowareTranslatorHeader.LabwareMap(p.mapLabware.mapValues(toLabwareItem).toSeq : _*)
			val mapLabware = p.mapLabware.mapValues(toLabwareItem)
			val s = compilerConfig.translator.saveWithHeader(succT.cmds, p.sHeader, mapLabware, sFilename)
			println(s)
		case Right(succ) => succ.print()
	}
}
