package roboliq.device.control.low

import scala.reflect.runtime.{universe => ru}
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._


/** Call an external script or executable */
case class ExecCmd(
	text: String,
	waitTillDone: Boolean,
	checkResult: Boolean
) extends Cmd {
	def cmd = "control.low.exec"
	def typ = ru.typeOf[this.type]
}

case class ExecToken(
	text: String,
	waitTillDone: Boolean,
	checkResult: Boolean
) extends CmdToken

class ExecHandler extends CommandHandler[ExecCmd]("control.low.exec") {
	def handleCmd(cmd: ExecCmd) = {
		output(
			ExecToken(cmd.text, cmd.waitTillDone, cmd.checkResult)
		)
	}
}
