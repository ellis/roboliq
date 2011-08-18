package roboliq.level3

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._


class CompileNode(cmd: Command, res: CompileResult, translation: Seq[Command], children: Seq[CompileNode])


class Compiler {
	//var robot: Robot = _
	//var state: RobotState = _
	private var m_handlers: List[CommandCompiler] = Nil
	//private var m_handlers2: List[CommandCompilerL2] = Nil
	//private var m_handlers3: List[CommandCompilerL3] = Nil
	//var kb: KnowledgeBase = null
	//var state0: RobotState = null
	
	def register(handler: CommandCompiler) {
		m_handlers = handler :: m_handlers
		/*if (handler.isInstanceOf[CommandCompilerL1])
			m_handlers1 = handler.asInstanceOf[CommandCompilerL1] :: m_handlers1
		if (handler.isInstanceOf[CommandCompilerL2])
			m_handlers2 = handler.asInstanceOf[CommandCompilerL2] :: m_handlers2
		if (handler.isInstanceOf[CommandCompilerL3])
			m_handlers3 = handler.asInstanceOf[CommandCompilerL3] :: m_handlers3*/
	}
	
	def compileL3(kb: KnowledgeBase, map31: ObjMapper, state0: RobotState, cmds: Seq[Command]): Seq[CompileNode] = {
		val (rs, _) = compileCommands(kb, map31, Some(state0), cmds)
		rs
	}
	
	def compileL1(state0: RobotState, cmd: Command): Option[CompileFinal] = {
		val ctx = new CompilerContextL1(state0)
		for (handler <- m_handlers) {
			if (handler.isInstanceOf[CommandCompilerL1]) {
				val state1 = handler.asInstanceOf[CommandCompilerL1].updateState(ctx, cmd)
				return Some(CompileFinal(cmd, state1))
			}
		}
		None
	}
	
	def compileL1(state0: RobotState, cmds: Seq[Command]): Seq[CompileFinal] = {
		var ress = new ArrayBuffer[CompileFinal]
		var state = state0
		for (cmd <- cmds) {
			compileL1(state, cmd) match {
				case Some(res) => ress += res
				case None => return Nil
			}
		}
		ress
	}
	
	private def compileCommands(kb: KnowledgeBase, map31: ObjMapper, state0_? : Option[RobotState], cmds: Seq[Command]): Tuple2[Seq[CompileNode], Option[RobotState]] = {
		var state_? : Option[RobotState] = state0_?
		val ress = cmds.map(cmd => {
			val (node, state1_?) = compileCommand(kb, map31, state_?, cmd)
			state_? = state1_?
			node
		})
		(ress, state_?)
	}
	
	private def compileCommand(kb: KnowledgeBase, map31: ObjMapper, state0_? : Option[RobotState], cmd: Command): Tuple2[CompileNode, Option[RobotState]] = {
		val res = compile(kb, map31, state0_?, cmd)
		res match {
			case CompileFinal(_, state1) =>
				(new CompileNode(cmd, res, Nil, Nil), Some(state1))
			case CompileTranslation(_, translation) =>
				val (children, state1_?) = compileCommands(kb, map31, state0_?, translation)
				(new CompileNode(cmd, res, translation, children), state1_?)
			case _ =>
				(new CompileNode(cmd, res, Nil, Nil), None)
		}
	}

	private def addKnowledge(kb: KnowledgeBase, cmd: Command) {
		for (handler <- m_handlers) {
			if (handler.isInstanceOf[CommandCompilerL3]) {
				handler.asInstanceOf[CommandCompilerL3].addKnowledge(kb, cmd)
			}
		}
	}
	
	private def addKnowledge(kb: KnowledgeBase, cmds: Seq[Command]) {
		cmds.foreach(cmd => addKnowledge(kb, cmd))
	}
	
	private def compile(kb: KnowledgeBase, map31: ObjMapper, state0_? : Option[RobotState], cmd: Command): CompileResult = {
		def x(handlers: List[CommandCompiler]): CompileResult =  handlers match {
			case Nil =>
				new CompileError(
						cmd = cmd,
						errors = List("Unhandled command: "+cmd.getClass().getCanonicalName()))
			case handler :: rest =>
				callHandlerCompile(kb, map31, state0_?, handler, cmd) match {
					case Some(res) => res
					case None => x(rest)
				}
		}
		x(m_handlers)
	}
	
	private def callHandlerCompile(kb: KnowledgeBase, map31: ObjMapper, state0_? : Option[RobotState], handler: CommandCompiler, cmd: Command): Option[CompileResult] = {
		if (handler.isInstanceOf[CommandCompilerL3]) {
			val ctx = new CompilerContextL3(this, kb, map31, state0_?)
			val res = handler.asInstanceOf[CommandCompilerL3].compileL3(ctx, cmd)
			return Some(res)
		}
		
		state0_? match {
			case Some(state0) =>
				if (handler.isInstanceOf[CommandCompilerL2]) {
					val ctx = new CompilerContextL2(this, state0)
					val res = handler.asInstanceOf[CommandCompilerL2].compileL2(ctx, cmd)
					return Some(res)
				}
				
				if (handler.isInstanceOf[CommandCompilerL1]) {
					val ctx = new CompilerContextL1(state0)
					val state1 = handler.asInstanceOf[CommandCompilerL1].updateState(ctx, cmd)
					return Some(CompileFinal(cmd, state1))
				}
				
				None
			case _ =>
				Some(CompilePending(cmd))
		}
	}
	
	def score(state0: RobotState, res: CompileFinal): Option[Int] = {
		for (handler <- m_handlers) {
			if (handler.isInstanceOf[CommandCompilerL1]) {
				val n = handler.asInstanceOf[CommandCompilerL1].score(state0, res)
				return Some(n)
			}
		}
		None
	}

	def score(state0: RobotState, ress: Seq[CompileFinal]): Option[Int] = {
		var n = 0
		var state = state0
		for (res <- ress) {
			score(state, res) match {
				case Some(n2) => n += n2
				case None => return None
			}
		}
		Some(n)
	}
}
