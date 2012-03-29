package roboliq.robots.evoware.devices.roboseal

import roboliq.common._
import roboliq.commands._
import roboliq.commands.seal._
import roboliq.compiler._
import roboliq.robots.evoware.commands._


class L3P_Seal_RoboSeal(device: RoboSealDevice) extends L3P_PlateCommand(device) {
	type CmdType = L3C_Seal
	val cmdType = classOf[CmdType]
	
	val bReturnPlateToOriginalLocation = true
	
	def getPlate(cmd: CmdType): Plate = cmd.args.plate
	
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
