package roboliq.robots.evoware.devices.roboseal

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands.move._
import roboliq.commands.seal._
import roboliq.compiler._
import roboliq.robots.evoware.commands._


abstract class PlateDevice(val location: String) extends Device {
	def fixedLocation_? : Option[String] = Some(location)
	def isPlateCompatible(plate: PlateConfigL2): Boolean
	def isPlatePreMoveRequired(plateState: PlateStateL2): Boolean
	def canAccessPlate(plate: PlateStateL2)
}

class PlateCommandDLP(
	val device: PlateDevice,
	val location: String,
	val idProgram: String
)

sealed trait PlateHandlingAction
case class PlateHandlingAction_Move(location: String) extends PlateHandlingAction
case class PlateHandlingAction_Cover extends PlateHandlingAction
case class PlateHandlingAction_Uncover extends PlateHandlingAction

abstract class L3P_PlateCommand(device: PlateDevice) extends CommandCompilerL3 {
	type CmdType = L3C_Seal
	val cmdType = classOf[CmdType]
	
	def plate: PlateConfigL2
	
	def isPlateCompatible(plate: PlateConfigL2): Boolean = device.isPlateCompatible(plate)
	def isPlatePreMoveRequired(plateState: PlateStateL2): Boolean = device.isPlatePreMoveRequired(plateState)
	def chooseDeviceLocationProgram(ctx: CompilerContextL3, cmd: CmdType): PlateCommandDLP
	
	override def compile(ctx: CompilerContextL3, cmd: CmdType): CompileResult = {
		val cmds = new ArrayBuffer[Command]

		val dlp = chooseDeviceLocationProgram(ctx, cmd)
		val plateState = plate.state(ctx.states)
		if (plateState.location != dlp.location) {
			cmds += L3C_MovePlate(new L3A_MovePlateArgs(plate, ValueArg(dlp.location), None))
		}
		CompileError(cmd, Seq())
	}
}


class L3P_Seal_RoboSeal(device: RoboSealDevice) extends CommandCompilerL3 {
	type CmdType = L3C_Seal
	val cmdType = classOf[CmdType]
	
	def compile(ctx: CompilerContextL3, cmd: CmdType): CompileResult = {
		val cmds = new ArrayBuffer[Command]
		import cmd.args._

		if (idDevice_?.getOrElse(device.idDevice) != device.idDevice)
			return CompileError(cmd, Seq("bad device ID \""+idDevice_?.get+"\", required \""+device.idDevice+"\""))
		
		cmds ++= plateHandling.getPreHandlingCommands(ctx.states, plate)

		val idDevice = device.idDevice
		val idProgram = cmd.args.idProgram_?.getOrElse(device.idProgramDefault)
		val args2 = new L12A_EvowareFactsArgs(idDevice, idDevice+"_Seal", idProgram)
		cmds += L2C_EvowareFacts(args2)

		cmds ++= plateHandling.getPostHandlingCommands(ctx.states, plate)

		CompileTranslation(cmd, cmds.toSeq)
	}
}
