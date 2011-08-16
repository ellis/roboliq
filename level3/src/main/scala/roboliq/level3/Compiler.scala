package roboliq.level3

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.compiler._
import roboliq.robot._

class CompileNode(cmd: Command, res: CompileResult, translation: Seq[Command], children: Seq[CompileNode])


class Compiler {
	//var robot: Robot = _
	//var state: RobotState = _
	private var m_handlers: List[CommandHandler] = Nil
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
	
	def compileL3(kb: KnowledgeBase, state0: RobotState, cmds: Seq[Command]): Seq[CompileNode] = {
		kb.concretize()
		val map31 = kb.map31.toMap
		val (rs, _) = compileCommands(kb, map31, Some(state0), cmds)
		rs
	}
	
	def compileL1(state0: RobotState, cmd: Command): Option[CompileFinal] = {
		for (handler <- m_handlers) {
			if (handler.isInstanceOf[CommandHandlerL1]) {
				handler.asInstanceOf[CommandHandlerL1].updateState(state0, cmd) match {
					case Some(state1) => return Some(CompileFinal(cmd, state1))
					case None =>
				}
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
	
	private def compileCommands(kb: KnowledgeBase, map31: Map[ObjectL3, roboliq.parts.ObjectL1], state0_? : Option[RobotState], cmds: Seq[Command]): Tuple2[Seq[CompileNode], Option[RobotState]] = {
		var state_? : Option[RobotState] = state0_?
		val ress = cmds.map(cmd => {
			val (node, state1_?) = compileCommand(kb, map31, state_?, cmd)
			state_? = state1_?
			node
		})
		(ress, state_?)
	}
	
	private def compileCommand(kb: KnowledgeBase, map31: Map[ObjectL3, roboliq.parts.ObjectL1], state0_? : Option[RobotState], cmd: Command): Tuple2[CompileNode, Option[RobotState]] = {
		val res = compile(state0_?, cmd)
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
			if (handler.isInstanceOf[CommandHandlerL3]) {
				handler.asInstanceOf[CommandHandlerL3].addKnowledge(kb, cmd)
			}
		}
	}
	
	private def addKnowledge(kb: KnowledgeBase, cmds: Seq[Command]) {
		cmds.foreach(cmd => addKnowledge(kb, cmd))
	}
	
	private def compile(state0_? : Option[RobotState], cmd: Command): CompileResult = {
		def x(handlers: List[CommandHandler]): CompileResult =  handlers match {
			case Nil =>
				new CompileError(
						cmd = cmd,
						errors = List("Unhandled command: "+cmd.getClass().getCanonicalName()))
			case handler :: rest =>
				callHandlerCompile(handler, state0_?, cmd) match {
					case Some(res) => res
					case None => x(rest)
				}
		}
		x(m_handlers)
	}
	
	private def callHandlerCompile(handler: CommandHandler, state0_? : Option[RobotState], cmd: Command): Option[CompileResult] = {
		if (handler.isInstanceOf[CommandHandlerL3]) {
			val res = handler.asInstanceOf[CommandHandlerL3].compile3(kb, cmd)
			if (res.isDefined)
				return res
		}
		
		state0_? match {
			case Some(state0) =>
				if (handler.isInstanceOf[CommandHandlerL2]) {
					val res = handler.asInstanceOf[CommandHandlerL2].compile2(state0, cmd)
					if (res.isDefined)
						return res
				}
				
				if (handler.isInstanceOf[CommandHandlerL1]) {
					val state1_? = handler.asInstanceOf[CommandHandlerL1].updateState(state0, cmd)
					if (state1_?.isDefined)
						return Some(CompileFinal(cmd, state1_?.get))
				}
				
				None
			case _ =>
				Some(CompilePending(cmd))
		}
	}
	
	def score(state0: RobotState, res: CompileFinal): Option[Int] = {
		for (handler <- m_handlers) {
			if (handler.isInstanceOf[CommandHandlerL1]) {
				handler.asInstanceOf[CommandHandlerL1].score(state0, res) match {
					case Some(n) => return Some(n)
					case None =>
				}
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
