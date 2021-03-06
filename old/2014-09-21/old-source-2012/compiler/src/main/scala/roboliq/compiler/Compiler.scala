package roboliq.compiler

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands
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

case class CompilerStageSuccess(nodes: Seq[CompileNode], log: Log = Log.empty) extends CompileStageResult {
	def print() {
		val finals = nodes.flatMap(_.collectFinal())
		val cmds1 = finals.map(_.cmd1)
		println("Output:")
		cmds1.foreach(cmd => println(cmd.getClass().getSimpleName()))
		println()
	}
}

class Compiler(val processors: Seq[CommandCompiler], val nDepth: Int = 0) {
	var bDebug = false
	private var nIndent = nDepth
	private val m_processorsInternal = List(
		new L3P_SaveCurrentLocation,
		new L3P_Timer
	)
	private val m_handlers: Map[java.lang.Class[_], CommandCompiler] =
		(m_processorsInternal ++ processors).map(p => p.cmdType -> p).toMap
	
	def createSubCompiler(): Compiler = {
		new Compiler(processors, nDepth + 1)
	}
	
	/*
	def apply(in: Either[CompileStageError, _ <: CompileStageResult]): Either[CompileStageError, CompilerStageSuccess] = {
		in match {
			case Left(err) => Left(err)
			case Right(succ) => apply(succ) 
		}
	}
	*/
	
	def compile(in: CompileStageResult, cmds: Seq[Command]): Either[CompileStageError, CompilerStageSuccess] = {
		in match {
			case succ: KnowledgeStageSuccess =>
				compile(succ.states0, cmds) match {
					case Left(err) =>
						val item = new LogItem(err.cmd, err.errors)
						Left(CompileStageError(new Log(Seq(item), Seq(), Seq())))
					case Right(nodes) =>
						Right(CompilerStageSuccess(nodes))
				}
			case _ => Left(CompileStageError(Log("unrecognized CompileStageResult input")))
		}
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
	
	private def retTranslation1(cmd: Command, res: Result[Command]) = res match {
		case Error(lsError) => CompileError(cmd, lsError)
		case Success(cmd2) => CompileTranslation(cmd, Seq(cmd2))
	}
	
	private def retTranslations(cmd: Command, res: Result[Seq[_ <: Command]]) = res match {
		case Error(lsError) => CompileError(cmd, lsError)
		case Success(cmds2) => CompileTranslation(cmd, cmds2)
	}
	
	private def compileToResult(states: RobotState, cmd: Command): CompileResult = {
		m_handlers.get(cmd.getClass()) match {
			case Some(handler) =>
				val handler = m_handlers(cmd.getClass())
				callHandlerCompile(states, handler, cmd)
			case None =>
				cmd match {
					case cmd4: CommandL4 =>
						retTranslation1(cmd, cmd4.toL3(states))
					case cmd2: CommandL2 =>
						cmd2.toL1(states) match {
							case Error(lsErrors) => CompileError(cmd, lsErrors)
							case Success(cmd1) =>
								val builder = new StateBuilder(states)
								cmd2.updateState(builder)
								//println("cmd: "+cmd2.getClass().getName())
								//println("result: "+builder.toImmutable.map.filter(!_._1.isInstanceOf[Well]))
								CompileFinal(cmd2, cmd1, builder.toImmutable)
						}
					case _ =>
						new CompileError(
								cmd = cmd,
								errors = List("Unhandled command: "+cmd.toDebugString))
				}
		}
	}
	
	private def callHandlerCompile(state0: RobotState, handler: CommandCompiler, cmd: Command): CompileResult = {
		handler match {
			case handler2 : CommandCompilerL2 =>
				val builder = new StateBuilder(state0)
				handler2.updateStateL2(builder, cmd)
				val cmd2 = cmd.asInstanceOf[CommandL2]
				cmd2.toL1(state0) match {
					case Error(lsErrors) => CompileError(cmd, lsErrors)
					case Success(cmd1) => CompileFinal(cmd2, cmd1, builder.toImmutable)
				}
			case handler3 : CommandCompilerL3 =>
				val ctx = new CompilerContextL3(this, state0)
				handler3.compileL3(ctx, cmd) match {
					case Error(lsErrors) => CompileError(cmd, lsErrors)
					case Success(cmds2) => CompileTranslation(cmd, cmds2)
				}
			case handler4 : CommandCompilerL4 =>
				val ctx = new CompilerContextL4(this, state0)
				handler4.compileL4(ctx, cmd) match {
					case Error(lsErrors) => CompileError(cmd, lsErrors)
					case Success(cmds3) => CompileTranslation(cmd, cmds3)
				}
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
	def compile(kb: KnowledgeBase, compiler_? : Option[Compiler], translator_? : Option[Translator], cmds: Seq[Command]): Either[CompileStageError, CompileStageResult] = {
		kb.concretize() match {
			case Left(errK) => Left(errK)
			case Right(succK) =>
				println(succK.states0.toDebugString)
				compiler_? match {
					case None => Right(succK)
					case Some(compiler) =>
						compiler.compile(succK, cmds) match {
							case Left(errC) => Left(errC)
							case Right(succC) =>
								translator_? match {
									case None => Right(succC)
									case Some(translator) =>
										val finals = succC.nodes.flatMap(_.collectFinal())
										//println("finals")
										//println(finals)
										val cmds1 = finals.map(_.cmd1)
										//println("Output:")
										//cmds1.foreach(cmd => println(cmd.getClass().getSimpleName()))
										//println()
										
										translator.translate(cmds1) match {
											case Left(errT) => Left(errT)
											case Right(succT) => Right(succT)
										}
								}
						}
				}
		}
	}

	/*
	def compile(robot: Robot, protocol: CommonProtocol) {
		val kb = protocol.kb

		protocol.m_protocol.get()
		protocol.__findPlateLabels()
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
	*/
}
