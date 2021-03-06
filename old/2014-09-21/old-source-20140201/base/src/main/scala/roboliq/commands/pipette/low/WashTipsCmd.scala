package roboliq.commands.pipette.low

import scala.reflect.runtime.{universe => ru}
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._


case class WashTipsCmd(
	description_? : Option[String],
	washProgram: WashProgram,
	tips: List[TipState]
) extends Cmd {
	def cmd = "pipette.low.washTips"
	def typ = ru.typeOf[this.type]
}

case class WashTipsToken(
	washProgram: String,
	tips: List[TipState]
) extends CmdToken

class WashTipsHandler extends CommandHandler[WashTipsCmd]("pipette.low.washTips") {
	def handleCmd(cmd: WashTipsCmd): RqReturn = {
		val tip_l = cmd.tips.sortBy(_.conf)
		output(
			WashTipsToken(cmd.washProgram.id, tip_l),
			tip_l.flatMap(tip => {
				TipCleanEvent(tip, cmd.washProgram) :: Nil
			})
		)
	}
	
	//def run(cmd: WashTipsCmd): List[Either[Event[_], Cmd]]
}
