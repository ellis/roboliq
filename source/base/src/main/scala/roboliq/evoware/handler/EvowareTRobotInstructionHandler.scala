package roboliq.evoware.handler

import roboliq.commands.ReaderRun
import roboliq.evoware.translator.L0C_Facts
import roboliq.commands.DeviceSiteOpen
import roboliq.commands.DeviceClose
import roboliq.commands.DeviceSiteClose
import roboliq.evoware.translator.TranslationItem
import org.apache.commons.io.FileUtils
import roboliq.input.Context
import roboliq.input.Instruction
import roboliq.commands.DeviceRun
import roboliq.input.Converter

private case class ProgramParams(
	dir: Int,
	program: Int
)

class EvowareTRobotInstructionHandler(carrierE: roboliq.evoware.parser.Carrier) extends EvowareDeviceInstructionHandler {
	import EvowareDeviceInstructionHandler._
	
	def handleInstruction(
		instruction: Instruction,
		identToAgentObject_m: Map[String, Object]
	): Context[List[TranslationItem]] = {
		for {
			deviceName <- Context.from(carrierE.deviceName_?, s"Evoware device name missing for carrier `${carrierE.sName}`")
			l <- instruction match {
				case _: DeviceClose =>
					Context.unit(List(TranslationItem(L0C_Facts(deviceName, deviceName+"_LidClose", ""), Nil)))
				case _: DeviceSiteClose =>
					Context.unit(List(TranslationItem(L0C_Facts(deviceName, deviceName+"_LidClose", ""), Nil)))
				case _: DeviceSiteOpen =>
					Context.unit(List(TranslationItem(L0C_Facts(deviceName, deviceName+"_LidOpen", ""), Nil)))
				case inst: DeviceRun =>
					run(identToAgentObject_m, inst, deviceName)
				case _ =>
					Context.error(s"TRobot unable to handle instruction: "+instruction)
			}
		} yield l
	}

	private def run(
		identToAgentObject_m: Map[String, Object],
		inst: DeviceRun,
		deviceName: String
	): Context[List[TranslationItem]] = {
		for {
			jsProgram <- Context.from(inst.program_?, s"You must supply a program for the thermocycler")
			program <- Context.getEntityAs[ProgramParams](jsProgram)
		} yield {
			List(TranslationItem(L0C_Facts(deviceName, deviceName+"_RunProgram", s"${program.dir}.${program.program}"), Nil))
		}
	}
}
