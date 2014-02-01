package roboliq.robots.evoware

import roboliq.core._
//import roboliq.compiler._
//import roboliq.protocol._


trait EvowareToolchain {
	val compilerConfig: CompilerConfig
	val evowareConfig: EvowareConfig
	
	//import compilerConfig._
	
	def compile(kb: KnowledgeBase, cmds: Seq[Command], bDebug: Boolean = false): Either[CompileStageError, CompileStageResult] = {
		addKnowledge(kb, compilerConfig.devices, cmds)
		
		val translator = compilerConfig.createTranslator
	
		val compiler = new Compiler(compilerConfig.processors)
	
		Compiler.compile(kb, Some(compiler), Some(translator), cmds)
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
