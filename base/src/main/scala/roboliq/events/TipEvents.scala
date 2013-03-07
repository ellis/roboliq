package roboliq.events

import spray.json._
import roboliq.core._, roboliq.entity._, roboliq.processor._



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
			for {
				content_# <- state0.content.addContentByVolume(event.src.content, event.volume)
				state_# = TipState(
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
				json <- ConversionsDirect.toJson[TipState](state_#)
			} yield List(EventItem_State(TKP("tipState", event.tip.id, Nil), json))
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
			"kind" -> JsString("tip.dispense"),
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
			for {
				content_# <- state0.content.removeVolume(event.volume)
				state_# = dispense(state0, content_#, liquid, event.pos)
				json <- ConversionsDirect.toJson[TipState](state_#)
			} yield List(EventItem_State(TKP("tipState", event.tip.id, Nil), json))
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

/** Represents an aspiration event. */
case class TipMixEvent(
	tip: TipState,
	/** Source well ID. */
	src: VesselState,
	/** Volume in liters to aspirate. */
	volume: LiquidVolume
) extends Event {
	def toJson: JsValue = {
		JsObject(Map(
			"kind" -> JsString("tip.mix"),
			"tip" -> JsString(tip.id),
			"src" -> JsString(src.vessel.id),
			"volume" -> JsString(volume.toString)
		))
	}
}

class TipMixEventHandler {// extends EventHandler {
	import RqFunctionHandler._
	
	def fnargs(event: TipMixEvent) = {
		fnRequire() {
			val state0 = event.tip
			val liquid = event.src.content.liquid
			val state_# = state0.copy(
				src_? = Some(event.src),
				contamInside = state0.contamInside ++ liquid.contaminants,
				nContamInsideVolume = LiquidVolume.max(state0.nContamInsideVolume, event.volume),
				contamOutside = state0.contamOutside ++ liquid.contaminants,
				srcsEntered = state0.srcsEntered + liquid,
				cleanDegree = CleanIntensity.None,
				cleanDegreePrev = state0.cleanDegreePrev,
				cleanDegreePending = CleanIntensity.max(state0.cleanDegreePending, liquid.tipCleanPolicy.exit)
			)
			for {
				json <- ConversionsDirect.toJson[TipState](state_#)
			} yield List(EventItem_State(TKP("tipState", event.tip.id, Nil), json))
		}
	}
}

case class TipCleanEvent(
	tip: TipState,
	washProgram: WashProgram
) extends Event

class TipCleanEventHandler {
	import RqFunctionHandler._
	
	def fnargs(event: TipCleanEvent) = fnRequire() {
		val w = event.washProgram
		val s = event.tip
		val state_# = s.copy(
			content = VesselContent.Empty,
			contamInside = s.contamInside -- w.contaminantsRemoved,
			contamOutside = s.contamOutside -- w.contaminantsRemoved,
			srcsEntered = Set(),
			destsEntered = Set(),
			cleanDegree = w.intensity,
			cleanDegreePrev = w.intensity,
			cleanDegreePending = if (s.cleanDegreePending <= w.intensity) CleanIntensity.None else s.cleanDegreePending
		)
		for {
			json <- ConversionsDirect.toJson[TipState](state_#)
		} yield List(EventItem_State(TKP("tipState", event.tip.id, Nil), json))
	}
}
