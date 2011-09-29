package roboliq.robots.evoware.devices.trobot

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands._
import roboliq.commands.move._
import roboliq.commands.pcr._
import roboliq.compiler._
import roboliq.robots.evoware.commands._


class L3P_Thermocycle_TRobot(device: TRobotDevice) extends CommandCompilerL3 {
	type CmdType = PcrThermocycle.L3C
	val cmdType = classOf[CmdType]
	
	override def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		for {
			dlp <- chooseDeviceLocationProgram(ctx, cmd)
		} yield {
			val plate = getPlate(cmd)
			val plateState0 = plate.state(ctx.states)

			Seq(
				getOpen(dlp),
				L3C_MovePlate(new L3A_MovePlateArgs(plate, ValueArg(dlp.location), None)),
				getClose(dlp),
				getRun(dlp),
				getOpen(dlp),
				L3C_MovePlate(new L3A_MovePlateArgs(plate, ValueArg(plateState0.location), None)),
				getClose(dlp)
			)
		}
	}
	
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
		val args = new PcrRun.L3A(Some(dlp.device), dlp.idProgram)
		PcrRun.L3C(args)
	}
}
