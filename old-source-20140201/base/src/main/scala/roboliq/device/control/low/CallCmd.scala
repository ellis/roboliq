package roboliq.device.control.low

import scala.reflect.runtime.{universe => ru}
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._


/** Call a subroutine within the robot control software (i.e. a Subroutine() in Evoware). */
case class CallCmd(
	text: String
) extends Cmd {
	def cmd = "control.low.call"
	def typ = ru.typeOf[this.type]
}

case class CallToken(
	text: String
) extends CmdToken

class CallHandler extends CommandHandler[CallCmd]("control.low.call") {
	def handleCmd(cmd: CallCmd) = {
		output(
			CallToken(cmd.text)
		)
	}
}
