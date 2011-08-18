package roboliq.level3

import roboliq.common._


trait CommandCompiler

class CompilerContextL1(
		val state0: RobotState
		)

class CompilerContextL2(
		val compiler: Compiler,
		val state0: RobotState
		)

class CompilerContextL3(
		val compiler: Compiler,
		val kb: KnowledgeBase,
		val map31: ObjMapper,
		val state0_? : Option[RobotState]
		)

trait CommandCompilerL1 extends CommandCompiler {
	def updateState(ctx: CompilerContextL1, cmd: Command): RobotState
	def score(state0: RobotState, res: CompileFinal): Int
}

trait CommandCompilerL2 extends CommandCompiler {
	def compileL2(ctx: CompilerContextL2, cmd: Command): CompileResult
}

trait CommandCompilerL3 extends CommandCompiler {
	def addKnowledge(kb: KnowledgeBase, cmd: Command)
	def compileL3(ctx: CompilerContextL3, cmd: Command): CompileResult
}
