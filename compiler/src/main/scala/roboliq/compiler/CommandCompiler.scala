package roboliq.compiler

import roboliq.common._
import roboliq.commands.PlateHandlingConfig


abstract class CommandCompiler {
	type CmdType <: Command
	
	val cmdType: java.lang.Class[_]
}

class CompilerContextL4(
	compiler0: roboliq.compiler.Compiler,
	val states: RobotState
) {
	val nCompilerDepth = compiler0.nDepth
	lazy val subCompiler = compiler0.createSubCompiler()
}

abstract class CommandCompilerL4 extends CommandCompiler {
	def addKnowledge(kb: KnowledgeBase, cmd: Command)
	def compileL4(ctx: CompilerContextL4, cmd: Command): Result[Seq[Command]]
}

class CompilerContextL3(
	compiler0: roboliq.compiler.Compiler,
	val states: RobotState
) {
	val nCompilerDepth = compiler0.nDepth
	lazy val subCompiler = compiler0.createSubCompiler()
}

abstract class CommandCompilerL3 extends CommandCompiler {
	final def compileL3(ctx: CompilerContextL3, _cmd: Command): Result[Seq[Command]] = {
		if (!_cmd.getClass().eq(cmdType))
			sys.error("Wrong command type")
		val cmd = _cmd.asInstanceOf[CmdType]
		compile(ctx, cmd)
	}
	
	def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]]
}

abstract class CommandCompilerL2 extends CommandCompiler {
	final def updateStateL2(builder: StateBuilder, _cmd: Command) {
		if (!_cmd.getClass().eq(cmdType))
			sys.error("Wrong command type")
		val cmd = _cmd.asInstanceOf[CmdType]
		updateState(builder, cmd)
	}
	
	final def scoreL2(state0: RobotState, state1: RobotState, _cmd: Command): Int = {
		if (!_cmd.getClass().eq(cmdType))
			sys.error("Wrong command type")
		val cmd = _cmd.asInstanceOf[CmdType]
		score(state0, state1, cmd)
	}

	def updateState(builder: StateBuilder, cmd: CmdType)
	def score(state0: RobotState, state1: RobotState, cmd: CmdType): Int
}

/*
abstract class CommandTranslatorL2 extends CommandCompiler {
	def updateStateL2(builder: StateBuilder, _cmd: Command) {
		if (!_cmd.getClass().eq(cmdType))
			sys.error("Wrong command type")
		val cmd = _cmd.asInstanceOf[CmdType]
		updateState(builder, cmd)
	}
	
	def scoreL2(state0: RobotState, state1: RobotState, _cmd: Command): Int = {
		if (!_cmd.getClass().eq(cmdType))
			sys.error("Wrong command type")
		val cmd = _cmd.asInstanceOf[CmdType]
		score(state0, state1, cmd)
	}

	def updateState(builder: StateBuilder, cmd: CmdType)
	def score(state0: RobotState, state1: RobotState, cmd: CmdType): Int
}
*/