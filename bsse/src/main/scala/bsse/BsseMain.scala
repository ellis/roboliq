package bsse

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._

import evoware._


object Main extends App {
	val protocol = new examples.Example02
	if (protocol.m_protocol.isDefined) protocol.m_protocol.get()
	protocol.__findPlateLabels()

	// Knowledge from devices
	val system = new BsseSystem(protocol.lab.sites)
	system.devices.foreach(_.addKnowledge(protocol.kb))

	// Knowledge from commands
	protocol.cmds.foreach(_ match {
		case c: CommandL4 => c.addKnowledge(protocol.kb)
		case _ =>
	})

	if (protocol.m_customize.isDefined) protocol.m_customize.get()
	
	val translator = new EvowareTranslator(system)

	val compiler = new Compiler(system.processors)
	compiler.bDebug = true

	Compiler.compile(protocol.kb, Some(compiler), Some(translator), protocol.cmds) match {
		case Left(err) => err.print()
		case Right(succ) => succ.print()
	}
}
