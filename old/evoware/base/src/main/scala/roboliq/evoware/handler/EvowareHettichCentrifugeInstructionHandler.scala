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
import roboliq.commands.DeviceCarouselMoveTo
import roboliq.commands.DeviceInitialize
import roboliq.commands.CentrifugeRun

class EvowareHettichCentrifugeInstructionHandler(carrierE: roboliq.evoware.parser.Carrier) extends EvowareDeviceInstructionHandler {
	import EvowareDeviceInstructionHandler._
	
	def handleInstruction(
		instruction: Instruction,
		identToAgentObject_m: Map[String, Object]
	): Context[List[TranslationItem]] = {
		for {
			deviceName <- Context.from(carrierE.deviceName_?, s"Evoware device name missing for carrier `${carrierE.sName}`")
			l <- instruction match {
				case inst: DeviceCarouselMoveTo =>
					Context.unit(List(TranslationItem(L0C_Facts(deviceName, deviceName+"_MoveToPos", inst.id), Nil)))
				case _: DeviceClose =>
					Context.unit(List(TranslationItem(L0C_Facts(deviceName, deviceName+"_Close", ""), Nil)))
				case _: DeviceInitialize =>
					Context.unit(List(TranslationItem(L0C_Facts(deviceName, deviceName+"_Init", ""), Nil)))
				case _: DeviceSiteClose =>
					Context.unit(List(TranslationItem(L0C_Facts(deviceName, deviceName+"_Close", ""), Nil)))
				case _: DeviceSiteOpen =>
					Context.unit(List(TranslationItem(L0C_Facts(deviceName, deviceName+"_Open", ""), Nil)))
				case inst: CentrifugeRun =>
					run(identToAgentObject_m, inst, deviceName)
			}
		} yield l
	}

	private def run(
		identToAgentObject_m: Map[String, Object],
		cmd: CentrifugeRun,
		deviceName: String
	): Context[List[TranslationItem]] = {
		for {
			_ <- Context.unit(0) // dummy statement -- might want to check program parameters here instead
		} yield {
			val p = cmd.program
			val x = List[Int](
				p.rpm_?.getOrElse(3000),
				p.duration_?.getOrElse(30),
				p.spinUpTime_?.getOrElse(9),
				p.spinDownTime_?.getOrElse(9),
				p.temperature_?.getOrElse(25)
			).mkString(",")
			// Token
			val token = L0C_Facts(deviceName, deviceName+"_Execute1", x)
			// Return
			val item = TranslationItem(token, Nil)
			List(item)
		}
	}

}
