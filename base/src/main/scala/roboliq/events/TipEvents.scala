package roboliq.events

import spray.json._
import roboliq.core._
import roboliq.processor._



/** Represents an aspiration event. */
case class TipAspirateEvent(
	tip: Tip,
	/** Source well ID. */
	src: VesselState,
	/** Volume in liters to aspirate. */
	volume: LiquidVolume
) extends Event {
	def toJson: JsValue = {
		JsObject(Map(
			"kind" -> JsString("tip.aspirate"),
			"tip" -> JsString(tip.id),
			"src" -> JsString(src.vessel.id),
			"volume" -> JsString(volume.toString)
		))
	}
}

class TipAspirateEventHandler {// extends EventHandler {
	import RqFunctionHandler._
	
	def fnargs(event: TipAspirateEvent) = {
		fnRequire (lookup[TipState](event.tip.id)) { state0 =>
			val liquid = event.src.content.liquid
			val content_# = state0.content.addContentByVolume(event.src.content, event.volume)
			val state_# = TipState(
				state0.conf,
				state0.model_?,
				Some(event.src),
				content_#,
				state0.contamInside ++ liquid.contaminants,
				LiquidVolume.max(state0.nContamInsideVolume, content_#.volume),
				state0.contamOutside ++ liquid.contaminants,
				state0.srcsEntered + liquid,
				state0.destsEntered,
				CleanIntensity.None,
				state0.cleanDegreePrev,
				CleanIntensity.max(state0.cleanDegreePending, liquid.tipCleanPolicy.exit)
			)
			for { json <- ConversionsDirect.toJson[TipState](state_#) }
			yield List(EventItem_State(TKP("tipState", event.tip.id, Nil), json))
		}
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
		fnRequire (lookup[TipState](event.tip.id)) { state0 =>
			val liquid = event.dest.content.liquid
			val content_# = state0.content.removeVolume(event.volume)
			val state_# = dispense(state0, content_#, liquid, event.pos)
			for { json <- ConversionsDirect.toJson[TipState](state_#) }
			yield List(EventItem_State(TKP("tipState", event.tip.id, Nil), json))
		}
	}

	private def dispense(state0: TipState, content: VesselContent, liquidDest: Liquid, pos: PipettePosition.Value): TipState = {
		pos match {
			case PipettePosition.WetContact => dispenseIn(state0, content, liquidDest)
			case _ => dispenseFree(state0, content)
		}
	}
	
	private def dispenseFree(state0: TipState, content: VesselContent): TipState = {
		state0.copy(
			content = content,
			cleanDegree = CleanIntensity.None
		)
	}
	
	private def dispenseIn(state0: TipState, content: VesselContent, liquid2: Liquid): TipState = {
		state0.copy(
			content = content,
			contamOutside = state0.contamOutside ++ liquid2.contaminants,
			destsEntered = state0.destsEntered + liquid2,
			cleanDegree = CleanIntensity.None,
			cleanDegreePending = CleanIntensity.max(state0.cleanDegreePending, liquid2.tipCleanPolicy.exit)
		)
	}
}
