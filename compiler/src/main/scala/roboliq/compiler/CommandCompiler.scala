package roboliq.compiler

import roboliq.common._

abstract class CommandCompiler {
	type CmdType <: Command
	
	val cmdType: java.lang.Class[_]
}

class CompilerContextL3(
		val compiler: Compiler,
		val states: RobotState
		)

abstract class CommandCompilerL3 extends CommandCompiler {
	def addKnowledge(kb: KnowledgeBase, cmd: Command)
	def compileL3(ctx: CompilerContextL3, cmd: Command): CompileResult
}

class CompilerContextL2(
		val compiler: Compiler,
		val states: RobotState
		)

abstract class CommandCompilerL2 extends CommandCompiler {
	def compileL2(ctx: CompilerContextL2, _cmd: Command): CompileResult = {
		if (!_cmd.getClass().eq(cmdType))
			sys.error("Wrong command type")
		val cmd = _cmd.asInstanceOf[CmdType]
		compile(ctx, cmd)
	}
	
	def compile(ctx: CompilerContextL2, cmd: CmdType): CompileResult
}

abstract class CommandCompilerL1 extends CommandCompiler {
	def updateStateL1(builder: StateBuilder, _cmd: Command) {
		if (!_cmd.getClass().eq(cmdType))
			sys.error("Wrong command type")
		val cmd = _cmd.asInstanceOf[CmdType]
		updateState(builder, cmd)
	}
	
	def scoreL1(state0: RobotState, state1: RobotState, _cmd: Command): Int = {
		if (!_cmd.getClass().eq(cmdType))
			sys.error("Wrong command type")
		val cmd = _cmd.asInstanceOf[CmdType]
		score(state0, state1, cmd)
	}

	def updateState(builder: StateBuilder, cmd: CmdType)
	def score(state0: RobotState, state1: RobotState, cmd: CmdType): Int
}

abstract class CommandTranslatorL1 extends CommandCompiler {
	def updateStateL1(builder: StateBuilder, _cmd: Command) {
		if (!_cmd.getClass().eq(cmdType))
			sys.error("Wrong command type")
		val cmd = _cmd.asInstanceOf[CmdType]
		updateState(builder, cmd)
	}
	
	def scoreL1(state0: RobotState, state1: RobotState, _cmd: Command): Int = {
		if (!_cmd.getClass().eq(cmdType))
			sys.error("Wrong command type")
		val cmd = _cmd.asInstanceOf[CmdType]
		score(state0, state1, cmd)
	}

	def updateState(builder: StateBuilder, cmd: CmdType)
	def score(state0: RobotState, state1: RobotState, cmd: CmdType): Int
}
