package roboliq.commands2.pipette

import scala.reflect.runtime.{universe => ru}
import roboliq.core._
import RqPimper._
import roboliq.processor2._
import scala.collection.JavaConversions._
import scala.Option.option2Iterable
import spray.json._
import roboliq.commands.pipette.TipWellVolumePolicy


case class DispenseCmd(
	description_? : Option[String],
	items: List[TipWellVolumePolicy] //FIXME: This should be TipWellVolumePolicyMixspec
)

case class DispenseToken(
	val items: List[TipWellVolumePolicy]
) extends CmdToken

class DispenseHandler extends CommandHandler("pipetter.dispense") {
	val fnargs = cmdAs[DispenseCmd] { cmd =>
		val events = cmd.items.flatMap(item => {
			TipDispenseEvent(item.tip, item.well.vesselState, item.volume, item.policy.pos) :: Nil
			//WellAddEventBean(item.well, src, item.volume) :: Nil
		})
		//val (doc, docMarkdown) = SpirateTokenItem.toAspriateDocString(cmd.items, ctx.ob, ctx.states)
		//Expand2Tokens(List(new AspirateToken(lItem.toList)), events.toList, doc, docMarkdown)
		RqSuccess(List(
			ComputationItem_Token(DispenseToken(cmd.items)),
			ComputationItem_Events(events)
		))
	}
}

/** Represents an aspiration event. */
case class TipDispenseEvent(
	tip: Tip,
	/** Source well ID. */
	dest: VesselState,
	/** Volume in liters to aspirate. */
	volume: LiquidVolume,
	/** Position of the tip upon dispense. */
	pos: PipettePosition.Value
) extends Event {
	def toJson: JsValue = {
		JsObject(Map(
			"kind" -> JsString("tip.aspirate"),
			"tip" -> JsString(tip.id),
			"dest" -> JsString(dest.vessel.id),
			"volume" -> JsString(volume.toString),
			"pos" -> JsString(pos.toString)
		))
	}
}

class TipDispenseEventHandler {// extends EventHandler {
	import RqFunctionHandler._
	
	def fnargs(event: TipDispenseEvent) = {
		fnRequire (lookup[TipState0](event.tip.id)) { state0 =>
			val liquid = event.dest.content.liquid
			val volumeNew = state0.volume + event.volume
			val state_# = dispense(state0, volumeNew, liquid, event.pos)
			RqSuccess(List(EventItem_State(TKP("tipState", event.tip.id, Nil), Conversions.tipStateToJson(state_#))))
		}
	}

	private def dispense(state0: TipState0, nVolumeDisp: LiquidVolume, liquidDest: Liquid, pos: PipettePosition.Value): TipState0 = {
		pos match {
			case PipettePosition.WetContact => dispenseIn(state0, nVolumeDisp, liquidDest)
			case _ => dispenseFree(state0, nVolumeDisp)
		}
	}
	
	private def dispenseFree(state0: TipState0, nVolume2: LiquidVolume): TipState0 = {
		val (liquid, nVolume) = getLiquidAndVolumeAfterDispense(state0, nVolume2)
		state0.copy(
			liquid = liquid,
			volume = nVolume,
			cleanDegree = CleanIntensity.None
		)
	}
	
	private def dispenseIn(state0: TipState0, nVolume2: LiquidVolume, liquid2: Liquid): TipState0 = {
		val (liquid, nVolume) = getLiquidAndVolumeAfterDispense(state0, nVolume2)
		state0.copy(
			liquid = liquid,
			volume = nVolume,
			contamOutside = state0.contamOutside ++ liquid2.contaminants,
			destsEntered = state0.destsEntered + liquid2,
			cleanDegree = CleanIntensity.None,
			cleanDegreePending = CleanIntensity.max(state0.cleanDegreePending, liquid2.tipCleanPolicy.exit)
		)
	}
	
	private def getLiquidAndVolumeAfterDispense(state0: TipState0, nVolume2: LiquidVolume): Tuple2[Liquid, LiquidVolume] = {
		val nVolume3 = state0.volume - nVolume2
		if (nVolume3 < LiquidVolume.nl(1)) {
			(Liquid.empty, LiquidVolume.empty)
		}
		else {
			(state0.liquid, nVolume3)
		}
	}
}
