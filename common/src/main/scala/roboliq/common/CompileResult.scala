package roboliq.common


sealed abstract class CompileResult//(warnings: Seq[String], errors: Seq[String], cmds: Seq[Command])

//case class CompilePending(cmd: Command) extends CompileResult
case class CompileFinal(cmd: CommandL2, cmd1: Command, state1: RobotState) extends CompileResult
case class CompileTranslation(cmd: Command, translation: Seq[Command]) extends CompileResult
case class CompileError(cmd: Command, errors: Seq[String]) extends CompileResult
