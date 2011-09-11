package bsse

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._

import evoware._


object Main extends App {
	val robot = BsseRobot()
	
	val protocol = new examples.Example02
	if (protocol.m_protocol.isDefined) protocol.m_protocol.get()
	protocol.__findPlateLabels()
	if (protocol.m_customize.isDefined) protocol.m_customize.get()
	robot.devices.foreach(_.addKnowledge(protocol.kb))
	
	val evowareMapper = BsseEvowareMapper(protocol.lab.mapSites)
	val translator = new EvowareTranslator(evowareMapper)

	val compiler = new Compiler(robot.processors)
	compiler.bDebug = true

	Compiler.compile(protocol.kb, Some(compiler), Some(translator), protocol.cmds) match {
		case Left(err) => err.print()
		case Right(succ) => succ.print()
	}
}
