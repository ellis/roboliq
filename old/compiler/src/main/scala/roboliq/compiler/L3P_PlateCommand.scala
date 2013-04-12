package roboliq.compiler

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands._
import roboliq.commands.move._
import roboliq.commands.seal._


class PlateCommandDLP(
	val device: PlateDevice,
	val location: String,
	val idProgram: String
)

/*sealed trait PlateHandlingAction
case class PlateHandlingAction_Move(location: String) extends PlateHandlingAction
case class PlateHandlingAction_Cover() extends PlateHandlingAction
case class PlateHandlingAction_Uncover() extends PlateHandlingAction*/

abstract class L3P_PlateCommand(device: PlateDevice) extends CommandCompilerL3 {
	val bReturnPlateToOriginalLocation: Boolean
	
	def getPlate(cmd: CmdType): Plate
	def getPlateHandling(cmd: CmdType): PlateHandlingConfig
	
	//def isPlateCompatible(plate: Plate): Boolean = device.isPlateCompatible(plate)
	//def isPlatePreMoveRequired(plateState: PlateStateL2): Boolean = device.isPlatePreMoveRequired(plateState)
	def chooseDeviceLocationProgram(ctx: CompilerContextL3, cmd: CmdType): Result[PlateCommandDLP]
	
	override def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		val cmds = new ArrayBuffer[Command]

		for {
			dlp <- chooseDeviceLocationProgram(ctx, cmd)
			pre <- compilePreCommand(dlp)
			trans <- compilePlateCommand(ctx, cmd, dlp)
			post <- compilePostCommand(dlp)
		} yield {
			cmds ++= pre
			val plate = getPlate(cmd)
			val plateState = plate.state(ctx.states)
			if (plateState.location != dlp.location) {
				cmds += L3C_MovePlate(new L3A_MovePlateArgs(plate, ValueArg(dlp.location), None))
			}
			cmds ++= trans
			if (bReturnPlateToOriginalLocation) {
				cmds += L3C_MovePlate(new L3A_MovePlateArgs(plate, ValueArg(plateState.location), None))
			}
			val plateHandling = getPlateHandling(cmd)
			cmds ++= plateHandling.getPostHandlingCommands(ctx.states, plate)
			cmds ++= post
		}
	}
	
	def compilePreCommand(dlp: PlateCommandDLP): Result[Seq[Command]] = Success(Seq())
	def compilePostCommand(dlp: PlateCommandDLP): Result[Seq[Command]] = Success(Seq())
	def compilePlateCommand(ctx: CompilerContextL3, cmd: CmdType, dlp: PlateCommandDLP): Result[Seq[Command]]
}
