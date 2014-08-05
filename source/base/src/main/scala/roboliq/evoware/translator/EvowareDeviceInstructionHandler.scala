package roboliq.evoware.translator

import roboliq.commands.DeviceSiteClose
import roboliq.commands.DeviceSiteOpen
import roboliq.input.Context
import roboliq.input.Instruction

trait EvowareDeviceInstructionHandler {
	def handleInstruction(
		instruction: Instruction,
		identToAgentObject_m: Map[String, Object]
	): Context[List[TranslationItem]]

	protected def getAgentObject[A](ident: String, identToAgentObject_m: Map[String, Object], error: => String): Context[A] = {
		Context.from(identToAgentObject_m.get(ident).map(_.asInstanceOf[A]), error)
	}
}

class EvowareInfiniteM200InstructionHandler(carrierE: roboliq.evoware.parser.Carrier) extends EvowareDeviceInstructionHandler {
	def handleInstruction(
		instruction: Instruction,
		identToAgentObject_m: Map[String, Object]
	): Context[List[TranslationItem]] = {
		for {
			deviceName <- Context.from(carrierE.deviceName_?, s"Evoware device name missing for carrier `${carrierE.sName}`")
			l <- instruction match {
				case DeviceSiteClose(_, _) =>
					Context.unit(List(TranslationItem(L0C_Facts(deviceName, deviceName+"_Close", ""), Nil)))
				case DeviceSiteOpen(_, _) =>
					Context.unit(List(TranslationItem(L0C_Facts(deviceName, deviceName+"_Open", ""), Nil)))
			}
		} yield l
	}
}