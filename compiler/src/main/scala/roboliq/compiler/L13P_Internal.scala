package roboliq.compiler

import roboliq.common._
import roboliq.commands._


class L3P_SaveCurrentLocation extends CommandCompilerL3 {
	type CmdType = L3C_SaveCurrentLocation
	val cmdType = classOf[CmdType]
	
	def compile(ctx: CompilerContextL3, cmd: CmdType): CompileResult = {
		CompileTranslation(cmd, Seq(L2C_SaveCurrentLocation(cmd.plate, cmd.mem)))
	}
}

class L3P_Timer extends CommandCompilerL3 {
	type CmdType = L3C_Timer
	val cmdType = classOf[CmdType]
	
	def compile(ctx: CompilerContextL3, cmd: CmdType): CompileResult = {
		val args = new L12A_TimerArgs(cmd.args.nSeconds)
		CompileTranslation(cmd, Seq(L2C_Timer(args)))
	}
}