package roboliq.robots.evoware.devices.trobot

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands._
import roboliq.commands.pcr._
import roboliq.compiler._
import roboliq.robots.evoware.commands._


class L3P_Thermocycle_TRobot(device: TRobotDevice) extends L3P_PlateCommand(device) {
	type CmdType = PcrThermocycle.L3C
	val cmdType = classOf[CmdType]
	
	def getPlate(cmd: CmdType): PlateConfigL2 = cmd.args.plate
	
	def getPlateHandling(cmd: CmdType): PlateHandlingConfig = cmd.args.plateHandling
	
	def chooseDeviceLocationProgram(ctx: CompilerContextL3, cmd: CmdType): Result[PlateCommandDLP] = {
		Success(new PlateCommandDLP(device, device.location, cmd.args.program))
	}
	
	private def getOpen(dlp: PlateCommandDLP): Command = {
		val args = new PcrOpen.L3A(Some(dlp.device), ())
		PcrOpen.L3C(args)
	}
	
	private def getClose(dlp: PlateCommandDLP): Command = {
		val args = new PcrClose.L3A(Some(dlp.device), ())
		PcrClose.L3C(args)
	}
	
	private def getRun(dlp: PlateCommandDLP): Command = {
		println("dlp.idProgram: "+dlp.idProgram)
		val args = new PcrRun.L3A(Some(dlp.device), dlp.idProgram)
		println("args: "+args)
		PcrRun.L3C(args)
	}
	
	override def compilePreCommand(dlp: PlateCommandDLP): Result[Seq[Command]] = Success(Seq(getOpen(dlp)))
	override def compilePostCommand(dlp: PlateCommandDLP): Result[Seq[Command]] = Success(Seq(getClose(dlp)))

	def compilePlateCommand(ctx: CompilerContextL3, cmd: CmdType, dlp: PlateCommandDLP): Result[Seq[Command]] = {
		val cmds = new ArrayBuffer[Command]
		cmds += getClose(dlp)
		cmds += getRun(dlp)
		cmds += getOpen(dlp)
		Success(Seq())
	}
}
