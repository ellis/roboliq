package roboliq.robots.evoware.devices.trobot

import roboliq.common._
import roboliq.commands.pcr._
import roboliq.compiler._
import roboliq.robots.evoware.commands._


class L3P_PcrClose(deviceDefault: TRobotDevice) extends CommandCompilerL3 {
	type CmdType = PcrClose.L3C
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		val device = cmd.args.device_?.getOrElse(deviceDefault).asInstanceOf[TRobotDevice]
		val args2 = new L12A_EvowareFactsArgs(device.idDevice, device.idDevice+"_LidClose", "")
		Success(Seq(L2C_EvowareFacts(args2)))
	}
}

class L3P_PcrOpen(deviceDefault: TRobotDevice) extends CommandCompilerL3 {
	type CmdType = PcrOpen.L3C
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		val device = cmd.args.device_?.getOrElse(deviceDefault).asInstanceOf[TRobotDevice]
		val args2 = new L12A_EvowareFactsArgs(device.idDevice, device.idDevice+"_LidOpen", "")
		Success(Seq(L2C_EvowareFacts(args2)))
	}
}

class L3P_PcrRun(deviceDefault: TRobotDevice) extends CommandCompilerL3 {
	type CmdType = PcrRun.L3C
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		println("cmd: "+cmd,cmd.toDebugString)
		val device = cmd.args.device_?.getOrElse(deviceDefault).asInstanceOf[TRobotDevice]
		println("cmd.args.program: " + cmd.args.program)
		val (iDir, iProg) = cmd.args.program.split(",") match {
			case Array(sDir, sProg) =>
				(sDir.toInt, sProg.toInt)
			case _ => return Error("must supply proper PCR program name: dir,prog")
		}
		val program = iDir.toString+"."+iProg
		val args2 = new L12A_EvowareFactsArgs(device.idDevice, device.idDevice+"_RunProgram", program)
		Success(Seq(L2C_EvowareFacts(args2)))
	}
}
