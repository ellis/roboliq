package roboliq.device.control

import scala.reflect.runtime.{universe => ru}
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._


case class PromptCmd(
	text: String
) extends Cmd {
	def cmd = "control.prompt"
	def typ = ru.typeOf[this.type]
}

case class PromptToken(
	text: String
) extends CmdToken

class PromptHandler extends CommandHandler[PromptCmd]("control.prompt") {
	def handleCmd(cmd: PromptCmd) = {
		output(
			PromptToken(cmd.text)
		)
	}
}
