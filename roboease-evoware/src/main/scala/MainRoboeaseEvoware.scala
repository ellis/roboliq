import java.io.FileReader

import scala.util.parsing.combinator._
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.compiler._
import roboliq.devices.pipette._
import roboliq.roboease
import roboliq.roboease._
import roboliq.robots.evoware._
import roboliq.labs.weizmann._
import roboliq.labs.weizmann.station1._


object Main extends App {
	test2()
	
	def test2() {
		import roboliq.roboease._
		import roboliq.compiler._
		
		val lsFiles = Seq(
			System.getProperty("user.home")+"/src/TelAviv/scripts/Rotem_Script01.conf",
			System.getProperty("user.home")+"/src/TelAviv/scripts/Rotem_Script02.conf",
			System.getProperty("user.home")+"/src/TelAviv/scripts/Rotem_Script03.conf",
			System.getProperty("user.home")+"/src/TelAviv/scripts/Rotem_Script04.conf"
		)
		
		val lsFileOverride = Seq(
			//System.getProperty("user.home")+"/src/TelAviv/scripts/temp.conf"
		)
		
		if (lsFileOverride.isEmpty)
			lsFiles.foreach(test2)
		else
			test2(lsFileOverride.head)
	}
	
	def test2(sSourcePath: String) {
		val p = new RoboeaseParser(new Config2Roboease)

		RoboeaseHack.bEmulateEvolab = true
		
		val sSource = scala.io.Source.fromFile(sSourcePath).mkString
		p.parse(sSource) match {
			case Left(err) =>
				err.print()
			case Right(res) =>
				val kb = res.kb
				val cmds = res.cmds.map(_.cmd)
				val evowareConfig = new Config3Translator()
				val compilerConfig = new Config4Compiler()
				val toolchain = new WeizmannToolchain(compilerConfig, evowareConfig)
				toolchain.compile(kb, cmds) match {
					case Left(err) => err.print()
					case Right(succT: TranslatorStageSuccess) =>
						val sFilename = sSourcePath + ".esc"
						case class LabwareItem(sLabel: String, sType: String, iGrid: Int, iSite: Int)
						def toLabwareItem(a: roboease.Labware): LabwareItem = {
							LabwareItem(a.sLabel, a.sType, a.rack.grid, a.rack.site)
						}
						val mapLabware = p.mapLabware.mapValues(toLabwareItem)
						val s = translator.saveWithHeader(succT.cmds, p.sHeader, mapLabware, sFilename)
						println(s)
					case Right(succ) => succ.print()
				}
		}
	}
}
