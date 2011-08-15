package roboliq.compiler

import roboliq.robot.RobotState


sealed abstract class CompileResult//(warnings: Seq[String], errors: Seq[String], cmds: Seq[Command])

case class CompilePending(cmd: Command) extends CompileResult
case class CompileSuccess(cmd: Command, state0: RobotState, translation: Seq[CompileResult]) extends CompileResult
case class CompileError(cmd: Command, errors: Seq[String]) extends CompileResult
