package roboliq.commands.pipette.low

import roboliq.core._
import roboliq.events._
import roboliq.processor._


case class WashTipsCmd(
	description_? : Option[String],
	washProgram: WashProgram,
	tips: List[TipState]
)

case class WashTipsToken(
	washProgram: String,
	tips: List[TipState]
) extends CmdToken

class WashTipsHandler extends CommandHandler[WashTipsCmd]("pipette.low.mix") {
	def handleCmd(cmd: WashTipsCmd): RqReturn = {
		output(
			WashTipsToken(cmd.washProgram.id, cmd.tips),
			cmd.tips.flatMap(tip => {
				TipCleanEvent(tip, cmd.washProgram) :: Nil
			})
		)
	}
}
