import java.io.FileReader

import scala.util.parsing.combinator._
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.compiler._
import roboliq.devices.pipette._
import roboliq.roboease
import roboliq.roboease._
import _root_.evoware._

import weizmann._


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
		val p = new ParserFile(
			WeizmannRoboeaseConfig.mapTables,
			WeizmannRoboeaseConfig.mapTipModel,
			WeizmannRoboeaseConfig.mapLcToPolicy
		)

		RoboeaseHack.bEmulateEvolab = true
		
		val sSource = scala.io.Source.fromFile(sSourcePath).mkString
		p.parse(sSource) match {
			case Error(err) =>
				err.print()
			case Success(res) =>
				val kb = res.kb
				val cmds = res.cmds.map(_.cmd)

				val robot = WeizmannRoboeaseConfig.robot
				robot.devices.foreach(_.addKnowledge(kb))
				
				val compiler = new Compiler(robot.processors)
				compiler.bDebug = true
				
				val evowareMapper = WeizmannEvowareMapper(p.racks)
				val translator = new EvowareTranslator(evowareMapper)
			
				Compiler.compile(kb, Some(compiler), None, cmds) match {
					case Error(errC) => errC.print()
					case Success(succC: CompilerStageSuccess) =>
						val finals = succC.nodes.flatMap(_.collectFinal())
						val cmds1 = finals.map(_.cmd1)
						translator.translate(cmds1) match {
							case Error(errT) => errT.print()
							case Success(succT) =>
								val sFilename = sSourcePath + ".esc"
								case class LabwareItem(sLabel: String, sType: String, iGrid: Int, iSite: Int)
								def toLabwareItem(a: roboease.Labware): _root_.evoware.LabwareItem = {
									_root_.evoware.LabwareItem(a.sLabel, a.sType, a.rack.grid, a.rack.site)
								}
								val mapLabware = p.mapLabware.mapValues(toLabwareItem)
								val s = translator.saveWithHeader(succT.cmds, p.sHeader, mapLabware, sFilename)
								println(s)
						}
					case Success(succ) => succ.print()
				}
		}
	}
}
