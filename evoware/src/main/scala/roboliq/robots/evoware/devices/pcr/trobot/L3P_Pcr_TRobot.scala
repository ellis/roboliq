package roboliq.robots.evoware.devices.pcr.trobot

import roboliq.common._
import roboliq.commands.pcr._
import roboliq.compiler._


class L3P_PcrClose(deviceDefault: PcrDevice_TRobot) extends CommandCompilerL3 {
	type CmdType = PcrClose.L3C
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		val device = cmd.args.device_?.getOrElse(deviceDefault).asInstanceOf[PcrDevice_TRobot]
		val args2 = new L12F_PcrClose.L2A(
			device,
			()
		)
		Success(Seq(L12F_PcrClose.L2C(args2)))
	}
}

class L3P_PcrOpen(deviceDefault: PcrDevice_TRobot) extends CommandCompilerL3 {
	type CmdType = PcrOpen.L3C
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		val device = cmd.args.device_?.getOrElse(deviceDefault).asInstanceOf[PcrDevice_TRobot]
		val args2 = new L12F_PcrOpen.L2A(
			device,
			()
		)
		Success(Seq(L12F_PcrOpen.L2C(args2)))
	}
}

class L3P_PcrRun(deviceDefault: PcrDevice_TRobot) extends CommandCompilerL3 {
	type CmdType = PcrRun.L3C
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		val device = cmd.args.device_?.getOrElse(deviceDefault).asInstanceOf[PcrDevice_TRobot]
		val (iDir, iProg) = cmd.args.program.split(",") match {
			case Array(sDir, sProg) =>
				(sDir.toInt, sProg.toInt)
			case _ => return Error("must supply proper PCR program name")
		}
		val args2 = new L12F_PcrRun.L2A(
			device,
			(iDir, iProg)
		)
		Success(Seq(L12F_PcrRun.L2C(args2)))
	}
}
