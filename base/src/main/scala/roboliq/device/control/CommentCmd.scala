package roboliq.device.control

import scala.reflect.runtime.{universe => ru}
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._


case class CommentCmd(
	text: String
) extends Cmd {
	def cmd = "control.comment"
	def typ = ru.typeOf[this.type]
}

case class CommentToken(
	text: String
) extends CmdToken

class CommentHandler extends CommandHandler[CommentCmd]("control.comment") {
	def handleCmd(cmd: CommentCmd) = {
		output(
			CommentToken(cmd.text)
		)
	}
}
