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
		
		val p = new ParserFile(
			WeizmannRoboeaseConfig.mapTables,
			WeizmannRoboeaseConfig.mapTipModel,
			WeizmannRoboeaseConfig.mapLcToPolicy
		)

		RoboeaseHack.bEmulateEvolab = true
		
		val sSourcePath = System.getProperty("user.home")+"/src/TelAviv/scripts/Rotem_Script03.conf"
		val sSource = scala.io.Source.fromFile(sSourcePath).mkString
		//val sSource = scala.io.Source.fromFile(System.getProperty("user.home")+"/src/TelAviv/scripts/temp.conf").mkString
		p.parse(sSource) match {
			case Left(err) =>
				err.print()
			case Right(res) =>
				val kb = res.kb
				val cmds = res.cmds.map(_.cmd)

				val robot = WeizmannRoboeaseConfig.robot
				robot.devices.foreach(_.addKnowledge(kb))
				
				val compiler = new Compiler(robot.processors)
				compiler.bDebug = true
				
				val evowareMapper = WeizmannEvowareMapper(p.racks)
				val translator = new EvowareTranslator(evowareMapper)
			
				Compiler.compile(kb, Some(compiler), None, cmds) match {
					case Left(errC) => errC.print()
					case Right(succC: CompilerStageSuccess) =>
						val finals = succC.nodes.flatMap(_.collectFinal())
						val cmds1 = finals.map(_.cmd1)
						translator.translate(cmds1) match {
							case Left(errT) => errT.print()
							case Right(succT) =>
								val sFilename = sSourcePath + ".esc"
								case class LabwareItem(sLabel: String, sType: String, iGrid: Int, iSite: Int)
								def toLabwareItem(a: roboease.Labware): _root_.evoware.LabwareItem = {
									_root_.evoware.LabwareItem(a.sLabel, a.sType, a.rack.grid, a.rack.site)
								}
								val mapLabware = p.mapLabware.mapValues(toLabwareItem)
								val s = translator.saveWithHeader(succT.cmds, p.sHeader, mapLabware, sFilename)
								println(s)
						}
					case Right(succ) => succ.print()
				}
		}
	}
}
