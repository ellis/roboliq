package roboliq.compiler

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.protocol.CommonProtocol


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


class Compiler(val processors: Seq[CommandCompiler], val nDepth: Int = 0) {
	var bDebug = false
	private var nIndent = nDepth
	private var m_handlers: Map[java.lang.Class[_], CommandCompiler] = processors.map(p => p.cmdType -> p).toMap
	
	def createSubCompiler(): Compiler = {
		new Compiler(processors, nDepth + 1)
	}
	
	def compile(states: RobotState, cmds: Seq[Command]): Either[CompileError, Seq[CompileNode]] = {
		compileCommands(states, cmds) match {
			case Left(err) => Left(err)
			case Right((nodes, state1)) => Right(nodes)
		}
	}
	
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
		if (bDebug) {
			print("* ")
			print("  " * nIndent)
			println(cmd.toDebugString)
		}
		nIndent += 1
		val res = compileToResult(state0, cmd)
		val node = res match {
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
		nIndent -= 1
		node
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
}

object Compiler {
	def compile(robot: Robot, translator: Translator, protocol: CommonProtocol) {
		val kb = protocol.kb

		if (protocol.m_protocol.isDefined) protocol.m_protocol.get()
		protocol.__findPlateLabels()
		if (protocol.m_customize.isDefined) protocol.m_customize.get()
		robot.devices.foreach(_.addKnowledge(kb))
		
		kb.concretize() match {
			case Left(errors) =>
				println("Missing information:")
				kb.printErrors(errors)
				println()
			case Right(map31) =>
				val state0 = map31.createRobotState()
				
				val compiler = new Compiler(robot.processors)
				compiler.bDebug = true
				
				compiler.compile(state0, protocol.cmds) match {
					case Left(err) =>
						println("Compilation errors:")
						err.errors.foreach(println)
						println()
					case Right(nodes) =>
						val finals = nodes.flatMap(_.collectFinal())
						val cmds1 = finals.map(_.cmd1)
						println("Output:")
						cmds1.foreach(cmd => println(cmd.getClass().getSimpleName()))
						println()
						
						translator.translate(cmds1) match {
							case Left(errs) =>
								println(errs)
							case Right(cmds0) => cmds0.foreach(println)
						}
				}
		}
	}
	
	def compile(robot: Robot, protocol: CommonProtocol) {
		val kb = protocol.kb

		protocol.m_protocol.get()
		protocol.m_customize.get()
		robot.devices.foreach(_.addKnowledge(kb))
		
		kb.concretize() match {
			case Left(errors) =>
				println("Missing information:")
				kb.printErrors(errors)
				println()
			case Right(map31) =>
				val state0 = map31.createRobotState()
				
				val compiler = new Compiler(robot.processors)
				
				compiler.compile(state0, protocol.cmds) match {
					case Left(err) =>
						println("Compilation errors:")
						err.errors.foreach(println)
						println()
					case Right(nodes) =>
						val finals = nodes.flatMap(_.collectFinal())
						val cmds1 = finals.map(_.cmd1)
						println("Output:")
						cmds1.foreach(cmd => println(cmd.getClass().getSimpleName()))
						println()
				}
		}
	}
	
	def compile(robot: Robot, compiler: Compiler, translator: Translator, kb: KnowledgeBase, cmds: Seq[Command]) {
		robot.devices.foreach(_.addKnowledge(kb))
		
		kb.concretize() match {
			case Left(errors) =>
				println("Missing information:")
				kb.printErrors(errors)
				println()
			case Right(map31) =>
				val state0 = map31.createRobotState()
				
				compiler.compile(state0, cmds) match {
					case Left(err) =>
						println("Compilation errors:")
						err.errors.foreach(println)
						println()
					case Right(nodes) =>
						val finals = nodes.flatMap(_.collectFinal())
						val cmds1 = finals.map(_.cmd1)
						println("Output:")
						cmds1.foreach(cmd => println(cmd.getClass().getSimpleName()))
						println()
						
						translator.translate(cmds1) match {
							case Left(errs) =>
								println(errs)
							case Right(cmds0) => cmds0.foreach(println)
						}
				}
		}
	}
}