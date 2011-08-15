package roboliq.level3

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.compiler._
import roboliq.robot._


class Compiler {
	//var robot: Robot = _
	//var state: RobotState = _
	private var m_handlers: List[CommandHandlerL3] = Nil
	//private var m_handlers2: List[CommandHandlerL2] = Nil
	//private var m_handlers3: List[CommandHandlerL3] = Nil
	var kb: KnowledgeBase = null
	//var state0: RobotState = null
	
	def register(handler: CommandHandler) {
		m_handlers = handler :: m_handlers
		/*if (handler.isInstanceOf[CommandHandlerL1])
			m_handlers1 = handler.asInstanceOf[CommandHandlerL1] :: m_handlers1
		if (handler.isInstanceOf[CommandHandlerL2])
			m_handlers2 = handler.asInstanceOf[CommandHandlerL2] :: m_handlers2
		if (handler.isInstanceOf[CommandHandlerL3])
			m_handlers3 = handler.asInstanceOf[CommandHandlerL3] :: m_handlers3*/
	}
	
	def compileRootL3(kb: KnowledgeBase, root: CompileResult) {
		kb.concretize()
		addKnowledge(kb, root)
		extractKnowledge
	}
	
	private def compileList(kb: KnowledgeBase, map31: Map[ObjectL3, roboliq.parts.ObjectL1], ress: Seq[CompileResult]): Unit = {
		
		ress
	}
	
	private def compileResult(kb: KnowledgeBase, map31: Map[ObjectL3, roboliq.parts.ObjectL1], res: CompileResult): Unit = {
		res match {
			case CompileError(_, _) =>
			case CompilePending(cmd) =>
				addKnowledge(kb, cmd)
			case CompileSuccess(cmd, ress) =>
				addKnowledge(kb, cmd)
				ress.foreach(res2 => addKnowledge(kb, res2))
		}
	}

	def addKnowledge(kb: KnowledgeBase, res: CompileResult) {
		res match {
			case CompileError(_, _) =>
			case CompilePending(cmd) =>
				addKnowledge(kb, cmd)
			case CompileSuccess(cmd, ress) =>
				addKnowledge(kb, cmd)
				ress.foreach(res2 => addKnowledge(kb, res2))
		}
	}
	
	private def addKnowledge(kb: KnowledgeBase, cmd: Command) {
		m_handlers.exists(_.addKnowledge(kb, cmd))
	}
	
	def translateL3toL2(map31: Map[ObjectL3, roboliq.parts.ObjectL1])
	
	def compile(state0: RobotState, cmd: Command): CompileResult = {
		def x(handlers: List[CommandHandler]): CompileResult =  handlers match {
			case Nil =>
				new CompileError(
						cmd = cmd,
						errors = List("Unhandled command: "+cmd.getClass().getCanonicalName()))
			case handler :: rest =>
				callHandlerCompile(handler, state0, cmd) match {
					case Some(res) => res
					case None => x(rest)
				}
		}
		x(m_handlers)
	}
	
	def compileMore(state0: RobotState, res: CompileResult): Option[CompileResult] = {
		var state = state0
		res match {
			case CompileError(_, _) => None
			case CompilePending(cmd) => Some(compile(state, cmd))
			case CompileSuccess(cmd, ress) =>
				val ress_? = ress.map(res => compileMore(state, res))
				if (ress_?.exists(_.isDefined))
					Some(CompileSuccess(cmd, state, ress_?.map(_.get)))
				else
					None
		}
	}
	
	private def callHandlerCompile(handler: CommandHandler, state0: RobotState, cmd: Command): Option[CompileResult] = {
		if (handler.isInstanceOf[CommandHandlerL1]) {
			val res = handler.asInstanceOf[CommandHandlerL1].compile1(cmd)
			if (res.isDefined)
				return res
		}
		
		if (handler.isInstanceOf[CommandHandlerL2]) {
			val res = handler.asInstanceOf[CommandHandlerL2].compile2(state0, cmd)
			if (res.isDefined)
				return res
		}
		
		if (handler.isInstanceOf[CommandHandlerL3]) {
			val res = handler.asInstanceOf[CommandHandlerL3].compile3(kb, cmd)
			if (res.isDefined)
				return res
		}
		
		return None
	}
}
