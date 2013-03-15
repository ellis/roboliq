import java.io.File
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
			System.getProperty("user.home")+"/src/weizmann/scripts/Rotem_Script01.conf",
			System.getProperty("user.home")+"/src/weizmann/scripts/Rotem_Script02.conf",
			System.getProperty("user.home")+"/src/weizmann/scripts/Rotem_Script03.conf",
			System.getProperty("user.home")+"/src/weizmann/scripts/Rotem_Script04.conf",
			null
		).filter(_ != null)
		
		val lsFileOverride = Seq(
			//System.getProperty("user.home")+"/src/weizmann/scripts/Rotem_Script01.conf"
			//System.getProperty("user.home")+"/src/weizmann/scripts/temp.conf"
			System.getProperty("user.home")+"/src/weizmann/pcr/Intronome_Script08.conf"
		)
		
		if (lsFileOverride.isEmpty)
			lsFiles.foreach(test2)
		else
			test2(lsFileOverride.head)
	}
	
	def test2(sSourcePath: String) {
		val station = new StationConfig
		val roboeaseConfig = new Config2Roboease(station)
		val p = new RoboeaseParser(
			dirProc = new File(System.getProperty("user.home")+"/src/weizmann/Evolab/procEvo/"),
			dirLog = new File(System.getProperty("user.home")+"/src/roboliq/"),
			roboeaseConfig
		)

		RoboeaseHack.bEmulateEvolab = true
		
		p.parseFile(sSourcePath) match {
			case Left(err) =>
				err.print()
			case Right(res) =>
				val kb = res.kb
				val cmds = res.cmds.map(_.cmd)
				val toolchain = new WeizmannToolchain(station)
				toolchain.compile(kb, cmds) match {
					case Left(err) => err.print()
					case Right(succT: TranslatorStageSuccess) =>
						val sFilename = sSourcePath + ".esc"
						def toLabwareItem(a: roboease.Labware): LabwareItem = {
							LabwareItem(a.sLabel, a.sType, a.rack.grid, a.rack.site)
						}
						val mapLabware = p.mapLabware.mapValues(toLabwareItem)
						val translator = new EvowareTranslator(toolchain.evowareConfig)
						val s = translator.saveWithHeader(succT.cmds, p.sHeader, mapLabware, sFilename)
						println(s)
					case Right(succ) => succ.print()
				}
		}
	}
}
