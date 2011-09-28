package roboliq.robots.evoware.devices.roboseal

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands._
import roboliq.commands.move._
import roboliq.commands.seal._
import roboliq.compiler._
import roboliq.robots.evoware.commands._


abstract class PlateDevice extends Device {
	def fixedLocation_? : Option[String]
	def isPlateCompatible(plate: PlateConfigL2): Boolean
	//def isPlatePreMoveRequired(plateState: PlateStateL2): Boolean
	//def canAccessPlate(plate: PlateStateL2)
}

class PlateCommandDLP(
	val device: PlateDevice,
	val location: String,
	val idProgram: String
)

sealed trait PlateHandlingAction
case class PlateHandlingAction_Move(location: String) extends PlateHandlingAction
case class PlateHandlingAction_Cover() extends PlateHandlingAction
case class PlateHandlingAction_Uncover() extends PlateHandlingAction

abstract class L3P_PlateCommand(device: PlateDevice) extends CommandCompilerL3 {
	def getPlate(cmd: CmdType): PlateConfigL2
	def getPlateHandling(cmd: CmdType): PlateHandlingConfig
	
	//def isPlateCompatible(plate: PlateConfigL2): Boolean = device.isPlateCompatible(plate)
	//def isPlatePreMoveRequired(plateState: PlateStateL2): Boolean = device.isPlatePreMoveRequired(plateState)
	def chooseDeviceLocationProgram(ctx: CompilerContextL3, cmd: CmdType): Result[PlateCommandDLP]
	
	override def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		val cmds = new ArrayBuffer[Command]

		for {
			dlp <- chooseDeviceLocationProgram(ctx, cmd)
			trans <- compilePlateCommand(ctx, cmd, dlp)
		} yield {
			val plate = getPlate(cmd)
			val plateState = plate.state(ctx.states)
			if (plateState.location != dlp.location) {
				cmds += L3C_MovePlate(new L3A_MovePlateArgs(plate, ValueArg(dlp.location), None))
			}
			cmds ++= trans
			val plateHandling = getPlateHandling(cmd)
			cmds ++= plateHandling.getPostHandlingCommands(ctx.states, plate)
		}
	}
	
	def compilePlateCommand(ctx: CompilerContextL3, cmd: CmdType, dlp: PlateCommandDLP): Result[Seq[Command]]
}


class L3P_Seal_RoboSeal(device: RoboSealDevice) extends L3P_PlateCommand(device) {
	type CmdType = L3C_Seal
	val cmdType = classOf[CmdType]
	
	def getPlate(cmd: CmdType): PlateConfigL2 = cmd.args.plate
	def getPlateHandling(cmd: CmdType): PlateHandlingConfig = cmd.args.plateHandling
	def chooseDeviceLocationProgram(ctx: CompilerContextL3, cmd: CmdType): Result[PlateCommandDLP] = {
		val idProgram = cmd.args.idProgram_?.getOrElse(device.idProgramDefault)
		Success(new PlateCommandDLP(device, device.location, device.idProgramDefault))
	}
	def compilePlateCommand(ctx: CompilerContextL3, cmd: CmdType, dlp: PlateCommandDLP): Result[Seq[Command]] = {
		val args2 = new L12A_EvowareFactsArgs(device.idDevice, device.idDevice+"_Seal", dlp.idProgram)
		Success(Seq(L2C_EvowareFacts(args2)))
	}
}
