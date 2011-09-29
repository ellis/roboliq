package roboliq.robots.evoware.devices.pcr.trobot

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands._
import roboliq.commands.pcr._
import roboliq.compiler._
import roboliq.robots.evoware.commands._


class L3P_Thermocycle_TRobot(device: PcrDevice_TRobot) extends L3P_PlateCommand(device) {
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
	
	override def compilePreCommand(dlp: PlateCommandDLP): Result[Seq[Command]] = Success(Seq(getOpen(dlp)))
	override def compilePostCommand(dlp: PlateCommandDLP): Result[Seq[Command]] = Success(Seq(getClose(dlp)))

	def compilePlateCommand(ctx: CompilerContextL3, cmd: CmdType, dlp: PlateCommandDLP): Result[Seq[Command]] = {
		val cmds = new ArrayBuffer[Command]
		cmds += getOpen(dlp)
		val args2 = new L12A_EvowareFactsArgs(device.idDevice, device.idDevice+"_RunProgram", dlp.idProgram)
		cmds += L2C_EvowareFacts(args2)
		cmds += getClose(dlp)
		Success(Seq())
	}
}
