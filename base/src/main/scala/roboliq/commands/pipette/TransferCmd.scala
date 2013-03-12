package roboliq.commands.pipette

import scalaz._
import Scalaz._
import roboliq.core._,roboliq.entity._,roboliq.processor._,roboliq.events._


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

class TransferHandler extends CommandHandler[TransferCmd]("pipette.transfer") {
	def handleCmd(cmd: TransferCmd): RqReturn = {
		val source_l = (cmd.source ++ cmd.destination).distinct
		val l = cmd.source.zip(cmd.destination).zip(cmd.amount)
		fnRequireList(source_l.map(source => lookup[VesselSituatedState](source.id))) { vss_l =>
			val vss_m = vss_l.map(vss => vss.id -> vss).toMap
			fnRequire(lookupAll[TipState]) { tip_l =>
				output(
					l.flatMap(tuple => {
						val ((src, dst), volume) = tuple
						val policy = PipettePolicy(cmd.pipettePolicy_?.get, PipettePosition.WetContact)
						val twvpA = TipWellVolumePolicy(tip_l.head, vss_m(src.vessels.head.id), volume, policy)
						val twvpD = TipWellVolumePolicy(tip_l.head, vss_m(dst.vessels.head.id), volume, policy)
						low.AspirateCmd(None, List(twvpA)) ::
						low.DispenseCmd(None, List(twvpD)) :: Nil
					})
				)
			}
		}
	}
}
