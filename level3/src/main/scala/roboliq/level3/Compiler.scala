package roboliq.level3

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._


class CompileNode(cmd: Command, res: CompileResult, translation: Seq[Command], children: Seq[CompileNode])


class Compiler {
	//var robot: Robot = _
	//var state: RobotState = _
	private var m_handlers = new HashMap[java.lang.Class[_], CommandCompiler]
	//private var m_handlers: List[CommandCompiler] = Nil
	//private var m_handlers2: List[CommandCompilerL2] = Nil
	//private var m_handlers3: List[CommandCompilerL3] = Nil
	//var kb: KnowledgeBase = null
	//var state0: RobotState = null
	
	def register(handler: CommandCompiler) {
		m_handlers(handler.cmdType) = handler
		//m_handlers = handler :: m_handlers
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
		m_handlers.get(cmd.getClass()) match {
			case Some(handler1 : CommandCompilerL1) =>
				val builder = new StateBuilder(state0)
				handler1.updateStateL1(builder, cmd)
				Some(CompileFinal(cmd, builder.toImmutable))
			case _ => None
		}
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
		m_handlers.get(cmd.getClass()) match {
			case Some(handler) =>
				val handler = m_handlers(cmd.getClass())
				callHandlerCompile(kb, map31, state0_?, handler, cmd)
			case None =>
				new CompileError(
						cmd = cmd,
						errors = List("Unhandled command: "+cmd.getClass().getCanonicalName()))
		}
	}
	
	private def callHandlerCompile(kb: KnowledgeBase, map31: ObjMapper, state0_? : Option[RobotState], handler: CommandCompiler, cmd: Command): CompileResult = {
		handler match {
			case handler1 : CommandCompilerL1 =>
				state0_? match {
					case Some(state0) =>
						val builder = new StateBuilder(state0)
						handler1.updateStateL1(builder, cmd)
						return CompileFinal(cmd, builder.toImmutable)
					case None =>
						CompilePending(cmd)
				}
			case handler2 : CommandCompilerL2 =>
				state0_? match {
					case Some(state0) =>
						val ctx = new CompilerContextL2(this, map31, state0)
						handler2.compileL2(ctx, cmd)
					case None =>
						CompilePending(cmd)
				}
			case handler3 : CommandCompilerL3 =>
				val ctx = new CompilerContextL3(this, kb, map31, state0_?)
				handler3.compileL3(ctx, cmd)
		}
	}
	
	def score(state0: RobotState, res: CompileFinal): Option[Int] = {
		m_handlers.get(res.cmd.getClass()) match {
			case Some(handler1 : CommandCompilerL1) =>
				val n = handler1.scoreL1(state0, res.state1, res.cmd)
				Some(n)
			case _ =>
				None
		}
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
