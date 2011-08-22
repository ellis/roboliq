package roboliq.compiler

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._


class CompileNode(cmd: Command, res: CompileResult, translation: Seq[Command], children: Seq[CompileNode]) {
	def collectFinal(): Seq[CompileFinal] = {
		res match {
			case c @ CompileFinal(_, _) => Seq(c)
			case c @ CompileTranslation(_, _) => children.flatMap(_.collectFinal())
			case _ => Seq()
		}
	}
}


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
	
	def compileL3(kb: KnowledgeBase, map31: ObjMapper, state0: RobotState, cmds: Seq[Command]): Either[CompileError, Seq[CompileNode]] = {
		compileCommands(state0, cmds) match {
			case Left(err) => Left(err)
			case Right((nodes, state1)) => Right(nodes)
		}
	}
	
	def compileL1(state0: RobotState, cmd: Command): Either[CompileError, CompileFinal] = {
		m_handlers.get(cmd.getClass()) match {
			case Some(handler1 : CommandCompilerL1) =>
				val builder = new StateBuilder(state0)
				handler1.updateStateL1(builder, cmd)
				Right(CompileFinal(cmd, builder.toImmutable))
			case Some(_) =>
				Left(CompileError(cmd, Seq("command compiler must be of type CommandCompilerL1")))
			case None =>
				Left(CompileError(cmd, Seq("no handler for command "+cmd.getClass().getCanonicalName())))
		}
	}
	
	def compileL1(state0: RobotState, cmds: Seq[Command]): Either[CompileError, Seq[CompileFinal]] = {
		var ress = new ArrayBuffer[CompileFinal]
		var state = state0
		for (cmd <- cmds) {
			compileL1(state, cmd) match {
				case Right(res) => ress += res
				case Left(sError) => return Left(sError)
			}
		}
		Right(ress)
	}
	
	private def compileCommands(state0: RobotState, cmds: Seq[Command]): Either[CompileError, Tuple2[Seq[CompileNode], RobotState]] = {
		var state = state0
		val ress = cmds.map(cmd => {
			val either = compileCommand(state, cmd)
			if (either.isLeft)
				return Left(either.left.get)
				
			val (node, state1) = either.right.get
			state = state1
			node
		})
		Right(ress, state)
	}
	
	private def compileCommand(state0: RobotState, cmd: Command): Either[CompileError, Tuple2[CompileNode, RobotState]] = {
		val res = compile(state0, cmd)
		res match {
			case CompileFinal(_, state1) =>
				Right(new CompileNode(cmd, res, Nil, Nil) -> state1)
			case CompileTranslation(_, translation) =>
				compileCommands(state0, translation) match {
					case Left(err) =>
						Left(err)
					case Right((children, state1)) =>
						Right(new CompileNode(cmd, res, translation, children) -> state1)
				}
			case err @ CompileError(_, _) =>
				Left(err)
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
	
	private def compile(state0: RobotState, cmd: Command): CompileResult = {
		m_handlers.get(cmd.getClass()) match {
			case Some(handler) =>
				val handler = m_handlers(cmd.getClass())
				callHandlerCompile(state0, handler, cmd)
			case None =>
				new CompileError(
						cmd = cmd,
						errors = List("Unhandled command: "+cmd.getClass().getCanonicalName()))
		}
	}
	
	private def callHandlerCompile(state0: RobotState, handler: CommandCompiler, cmd: Command): CompileResult = {
		handler match {
			case handler1 : CommandCompilerL1 =>
				val builder = new StateBuilder(state0)
				handler1.updateStateL1(builder, cmd)
				return CompileFinal(cmd, builder.toImmutable)
			case handler2 : CommandCompilerL2 =>
				val ctx = new CompilerContextL2(this, state0)
				handler2.compileL2(ctx, cmd)
			case handler3 : CommandCompilerL3 =>
				val ctx = new CompilerContextL3(this, state0)
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
