package roboliq.compiler

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._


class CompileNode(val cmd: Command, val res: CompileResult, val translation: Seq[Command], val children: Seq[CompileNode]) {
	def collectFinal(): Seq[CompileFinal] = {
		res match {
			case c @ CompileFinal(_, _, _) => Seq(c)
			case c @ CompileTranslation(_, _) => children.flatMap(_.collectFinal())
			case _ => Seq()
		}
	}

	def collectL2(): Seq[CommandL2] = {
		cmd match {
			case cmd2: CommandL2 => Seq(cmd2)
			case _ => children.flatMap(_.collectL2())
		}
	}
}


class Compiler {
	//var robot: Robot = _
	//var state: RobotState = _
	private var m_handlers = new HashMap[java.lang.Class[_], CommandCompiler]
	//private var m_handlers: List[CommandCompiler] = Nil
	//private var m_handlers2: List[CommandCompilerL3] = Nil
	//private var m_handlers3: List[CommandCompilerL4] = Nil
	//var kb: KnowledgeBase = null
	//var state0: RobotState = null
	
	def register(handler: CommandCompiler) {
		m_handlers(handler.cmdType) = handler
		//m_handlers = handler :: m_handlers
		/*if (handler.isInstanceOf[CommandCompilerL2])
			m_handlers1 = handler.asInstanceOf[CommandCompilerL2] :: m_handlers1
		if (handler.isInstanceOf[CommandCompilerL3])
			m_handlers2 = handler.asInstanceOf[CommandCompilerL3] :: m_handlers2
		if (handler.isInstanceOf[CommandCompilerL4])
			m_handlers3 = handler.asInstanceOf[CommandCompilerL4] :: m_handlers3*/
	}
	
	def compile(states: RobotState, cmds: Seq[Command]): Either[CompileError, Seq[CompileNode]] = {
		compileCommands(states, cmds) match {
			case Left(err) => Left(err)
			case Right((nodes, state1)) => Right(nodes)
		}
	}
	
	/*def score(states: RobotState, cmds: Seq[CompileFinal]): Either[CompileError, Int] = {
		compile(states, cmds) match {
			case Left(err) => Left(err)
			case Right(nodes) =>
				val cmds2 = nodes.flatMap(_.collectL2())
		}
	}*/
	
	/*
	def compileL2(state0: RobotState, cmd: Command): Either[CompileError, CompileFinal] = {
		m_handlers.get(cmd.getClass()) match {
			case Some(handler1 : CommandCompilerL2) =>
				val builder = new StateBuilder(state0)
				handler1.updateStateL2(builder, cmd)
				Right(CompileFinal(cmd, builder.toImmutable))
			case Some(_) =>
				Left(CompileError(cmd, Seq("command compiler must be of type CommandCompilerL2")))
			case None =>
				Left(CompileError(cmd, Seq("no handler for command "+cmd.getClass().getCanonicalName())))
		}
	}
	
	def compileL2(state0: RobotState, cmds: Seq[Command]): Either[CompileError, Seq[CompileFinal]] = {
		var ress = new ArrayBuffer[CompileFinal]
		var state = state0
		for (cmd <- cmds) {
			compileL2(state, cmd) match {
				case Right(res) => ress += res
				case Left(sError) => return Left(sError)
			}
		}
		Right(ress)
	}*/
	
	private def compileCommands(state0: RobotState, cmds: Seq[Command]): Either[CompileError, Tuple2[Seq[CompileNode], RobotState]] = {
		var state = state0
		val ress = cmds.map(cmd => {
			val either = compileCommand(state, cmd)
			if (either.isLeft)
				return Left(either.left.get)
				
			val (node, state1) = either.right.get
			state = state1
			//println("cmd: "+cmd.getClass().getName())
			//println("state: "+state.map.filter(!_._1.isInstanceOf[Well]))
			node
		})
		Right(ress, state)
	}
	
	private def compileCommand(state0: RobotState, cmd: Command): Either[CompileError, Tuple2[CompileNode, RobotState]] = {
		val res = compileToResult(state0, cmd)
		res match {
			case CompileFinal(_, _, state1) =>
				//println("cmd: "+cmd.getClass().getName())
				//println("final: "+state1.map.filter(!_._1.isInstanceOf[Well]))
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
	
	private def compileToResult(states: RobotState, cmd: Command): CompileResult = {
		m_handlers.get(cmd.getClass()) match {
			case Some(handler) =>
				val handler = m_handlers(cmd.getClass())
				callHandlerCompile(states, handler, cmd)
			case None =>
				cmd match {
					case cmd4: CommandL4 =>
						cmd4.toL3(states) match {
							case Left(lsErrors) => CompileError(cmd, lsErrors)
							case Right(cmd3) => CompileTranslation(cmd, Seq(cmd3))
						}
					case cmd2: CommandL2 =>
						cmd2.toL1(states) match {
							case Left(lsErrors) => CompileError(cmd, lsErrors)
							case Right(cmd1) =>
								val builder = new StateBuilder(states)
								cmd2.updateState(builder)
								//println("cmd: "+cmd2.getClass().getName())
								//println("result: "+builder.toImmutable.map.filter(!_._1.isInstanceOf[Well]))
								CompileFinal(cmd2, cmd1, builder.toImmutable)
						}
					case _ =>
						new CompileError(
								cmd = cmd,
								errors = List("Unhandled command: "+cmd.getClass().getCanonicalName()))
				}
		}
	}
	
	/*private def addKnowledge(kb: KnowledgeBase, cmds: Seq[Command]) {
		cmds.foreach(_ match {
			case cmd4: CommandL4 => cmd4.addKnowledge(kb)
			case _ =>
		})
	}*/
	
	private def callHandlerCompile(state0: RobotState, handler: CommandCompiler, cmd: Command): CompileResult = {
		handler match {
			case handler1 : CommandCompilerL2 =>
				val builder = new StateBuilder(state0)
				handler1.updateStateL2(builder, cmd)
				val cmd2 = cmd.asInstanceOf[CommandL2]
				cmd2.toL1(state0) match {
					case Left(lsErrors) => CompileError(cmd, lsErrors)
					case Right(cmd1) => CompileFinal(cmd2, cmd1, builder.toImmutable)
				}
			case handler2 : CommandCompilerL3 =>
				val ctx = new CompilerContextL3(this, state0)
				handler2.compileL3(ctx, cmd)
			case handler3 : CommandCompilerL4 =>
				val ctx = new CompilerContextL4(this, state0)
				handler3.compileL4(ctx, cmd)
		}
	}
	
	def scoreNodes(states0: RobotState, nodes: Seq[CompileNode]): Either[CompileError, Int] = {
		val ress = nodes.flatMap(_.collectFinal())
		scoreFinals(states0, ress)
	}

	def scoreFinals(states0: RobotState, ress: Seq[CompileFinal]): Either[CompileError, Int] = {
		var n = 0
		var states = states0
		for (res <- ress) {
			scoreFinal(states, res) match {
				case Left(err) => Left(err)
				case Right(n2) => n += n2
			}
			states = res.state1
		}
		Right(n)
	}

	def scoreFinal(states: RobotState, res: CompileFinal): Either[CompileError, Int] = {
		m_handlers.get(res.cmd.getClass()) match {
			case Some(handler2: CommandCompilerL2) =>
				val n = handler2.scoreL2(states, res.state1, res.cmd)
				Right(n)
			case _ =>
				Right(1)
				//Left(CompileError(res.cmd, Seq("INTERNAL: no registered scorer for command "+res.cmd.getClass().getName())))
		}
	}

	/*
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
	*/
}
