package roboliq.robots.evoware.devices.centrifuge

import roboliq.common._
import roboliq.commands.centrifuge._
import roboliq.compiler._
import roboliq.robots.evoware.commands._


class L3P_CentrifugeClose(deviceDefault: CentrifugeDevice) extends CommandCompilerL3 {
	type CmdType = CentrifugeClose.L3C
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		val device = cmd.args.device_?.getOrElse(deviceDefault).asInstanceOf[CentrifugeDevice]
		val args2 = new L12A_EvowareFactsArgs(device.idDevice, device.idDevice+"_Close", "")
		Success(Seq(L2C_EvowareFacts(args2)))
	}
}

class L3P_CentrifugeMoveTo(deviceDefault: CentrifugeDevice) extends CommandCompilerL3 {
	type CmdType = CentrifugeMoveTo.L3C
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		val device = cmd.args.device_?.getOrElse(deviceDefault).asInstanceOf[CentrifugeDevice]
		val args2 = new L12A_EvowareFactsArgs(device.idDevice, device.idDevice+"_MoveToPos", (cmd.args.program + 1).toString)
		Success(Seq(L2C_EvowareFacts(args2)))
	}
}

class L3P_CentrifugeOpen(deviceDefault: CentrifugeDevice) extends CommandCompilerL3 {
	type CmdType = CentrifugeOpen.L3C
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		val device = cmd.args.device_?.getOrElse(deviceDefault).asInstanceOf[CentrifugeDevice]
		val args2 = new L12A_EvowareFactsArgs(device.idDevice, device.idDevice+"_Open", "")
		Success(Seq(L2C_EvowareFacts(args2)))
	}
}

class L3P_CentrifugeRun(deviceDefault: CentrifugeDevice) extends CommandCompilerL3 {
	type CmdType = CentrifugeRun.L3C
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		println("cmd: "+cmd,cmd.toDebugString)
		val device = cmd.args.device_?.getOrElse(deviceDefault).asInstanceOf[CentrifugeDevice]
		println("cmd.args.program: " + cmd.args.program)
		val program = cmd.args.program
		val args2 = new L12A_EvowareFactsArgs(device.idDevice, device.idDevice+"_Execute1", program)
		Success(Seq(L2C_EvowareFacts(args2)))
	}
}
