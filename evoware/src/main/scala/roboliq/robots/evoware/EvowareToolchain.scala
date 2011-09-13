package roboliq.robots.evoware

import roboliq.common._
import roboliq.compiler._
import roboliq.protocol._


trait EvowareToolchain {
	def getDevices: Seq[Device]
	def getEvowareSystem: EvowareSystem
	def getProcessors: Seq[CommandCompiler]
	
	def compile(protocol: CommonProtocol, bDebug: Boolean = false): Either[CompileStageError, CompileStageResult] = {
		prepareProtocol(protocol)
		
		val system = getEvowareSystem
		val translator = new EvowareTranslator(system)
	
		val processors = getProcessors
		val compiler = new Compiler(processors)
	
		Compiler.compile(protocol.kb, Some(compiler), Some(translator), protocol.cmds)
	}
	
	def prepareProtocol(protocol: CommonProtocol) {
		runProtocol(protocol)
		addDeviceKnowledge(protocol.kb, getDevices)
		addCommandKnowledge(protocol.kb, protocol.cmds)
		
		// run customization
		if (protocol.m_customize.isDefined) protocol.m_customize.get()
	}
	
	def runProtocol(protocol: CommonProtocol) {
		if (protocol.m_protocol.isDefined) protocol.m_protocol.get()
		protocol.__findPlateLabels()
	}
	
	/** Knowledge from devices */
	def addDeviceKnowledge(kb: KnowledgeBase, devices: Seq[Device]) {
		devices.foreach(_.addKnowledge(kb))
	}
	
	/** Knowledge from commands */
	def addCommandKnowledge(kb: KnowledgeBase, cmds: Seq[Command]) {
		cmds.foreach(_ match {
			case c: CommandL4 => c.addKnowledge(kb)
			case _ =>
		})
	}	
}
