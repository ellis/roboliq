package roboliq.commands.pipette.low

import roboliq.core._
import roboliq.processor._


case class WashTipsCmd(
	description_? : Option[String],
	washProgram: WashProgram,
	tips: List[Tip]
)

case class WashTipsToken(
	washProgram: String,
	tips: List[Tip]
) extends CmdToken

class WashTipsHandler extends CommandHandler("pipette.low.mix") {
	val fnargs = cmdAs[WashTipsCmd] { cmd =>
		val event_l = cmd.tips.flatMap(item => {
			TipCleanEvent(item.tip, item.well.vesselState, LiquidVolume.empty) :: Nil
		})
		RqSuccess(List(
			ComputationItem_Token(WashTipsToken(cmd.washProgram.id, cmd.tips)),
			ComputationItem_Events(event_l)
		))
	}
}
