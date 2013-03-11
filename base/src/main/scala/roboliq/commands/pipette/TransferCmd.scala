package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scalaz._
import Scalaz._
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._


case class TransferCmd(
	description_? : Option[String],
	source: List[Source],
	destination: List[Source],
	amount: List[LiquidVolume],
	pipettePolicy_? : Option[String] = None,
	// TODO: add tipPolicy_? too (tip handling overrides)
	preMixSpec_? : Option[MixSpecOpt] = None,
	postMixSpec_? : Option[MixSpecOpt] = None
)

class TransferHandler_Fixed extends CommandHandler[TransferCmd]("pipette.transfer") {
	def handleCmd(cmd: TransferCmd): RqReturn = {
		fnRequire(lookupAll[TipState]) { tip_l =>
			val l = cmd.source.zip(cmd.destination).zip(cmd.amount)
			output(
				l.flatMap(tuple => {
					val ((src, dst), volume) = tuple
					val policy = PipettePolicy(cmd.pipettePolicy_?.get, PipettePosition.WetContact)
					val twvpA = TipWellVolumePolicy(tip_l.head, src.vessels.head, volume, policy)
					val twvpD = TipWellVolumePolicy(tip_l.head, dst.vessels.head, volume, policy)
					low.AspirateCmd(None, List(twvpA)) ::
					low.DispenseCmd(None, List(twvpD)) :: Nil
				})
			)
		}
	}
}
