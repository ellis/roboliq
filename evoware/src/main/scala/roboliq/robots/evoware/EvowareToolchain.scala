package roboliq.robots.evoware

import roboliq.common._
import roboliq.compiler._
import roboliq.protocol._


trait EvowareToolchain {
	val compilerConfig: CompilerConfig
	val evowareConfig: EvowareConfig
	
	//import compilerConfig._
	
	def compileProtocol(protocol: CommonProtocol, bDebug: Boolean = false): Either[CompileStageError, CompileStageResult] = {
		prepareProtocol(protocol)
		
		val translator = compilerConfig.createTranslator
	
		val compiler = new Compiler(compilerConfig.processors)
		compiler.bDebug = true
	
		Compiler.compile(protocol.kb, Some(compiler), Some(translator), protocol.cmds)
	}
	
	def compile(kb: KnowledgeBase, cmds: Seq[Command], bDebug: Boolean = false): Either[CompileStageError, CompileStageResult] = {
		addKnowledge(kb, compilerConfig.devices, cmds)
		
		val translator = compilerConfig.createTranslator
	
		val compiler = new Compiler(compilerConfig.processors)
		compiler.bDebug = true
	
		Compiler.compile(kb, Some(compiler), Some(translator), cmds)
	}
	
	def prepareProtocol(protocol: CommonProtocol) {
		runProtocol(protocol)
		addKnowledge(protocol.kb, compilerConfig.devices, protocol.cmds)
		
		// run customization
		if (protocol.m_customize.isDefined) protocol.m_customize.get()
	}
	
	def runProtocol(protocol: CommonProtocol) {
		if (protocol.m_protocol.isDefined) protocol.m_protocol.get()
		protocol.__findPlateLabels()
	}
	
	def addKnowledge(kb: KnowledgeBase, devices: Seq[Device], cmds: Seq[Command]) {
		addDeviceKnowledge(kb, devices)
		addCommandKnowledge(kb, cmds)
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
