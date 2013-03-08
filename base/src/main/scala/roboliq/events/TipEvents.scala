package roboliq.events

import spray.json._
import roboliq.core._, roboliq.entity._, roboliq.processor._



/** Represents an aspiration event. */
case class TipAspirateEvent(
	tip: TipState,
	/** Source well ID. */
	src: VesselState,
	/** Volume in liters to aspirate. */
	volume: LiquidVolume
) extends Event[TipState] {
	def getStateOrId = Right(tip)
	//def kind = "tip.aspirate"
	/*def toJson: JsValue = {
		JsObject(Map(
			"kind" -> JsString("tip.aspirate"),
			"tip" -> JsString(tip.id),
			"src" -> JsString(src.vessel.id),
			"volume" -> JsString(volume.toString)
		))
	}*/
}

class TipAspirateEventHandler extends EventHandler[TipState, TipAspirateEvent]("tip.aspirate") {
	def handleEvent(state0: TipState, event: TipAspirateEvent): RqResult[TipState] = {
		val liquid = event.src.content.liquid
		for {
			content_# <- state0.content.addContentByVolume(event.src.content, event.volume)
		} yield {
			TipState(
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
		}
	}
}

/** Represents an aspiration event. */
case class TipDispenseEvent(
	tip: TipState,
	/** Source well ID. */
	dest: VesselState,
	/** Volume in liters to aspirate. */
	volume: LiquidVolume,
	/** Position of the tip upon dispense. */
	pos: PipettePosition.Value
) extends Event[TipState] {
	def getStateOrId = Right(tip)
	/*def toJson: JsValue = {
		JsObject(Map(
			"kind" -> JsString("tip.dispense"),
			"tip" -> JsString(tip.id),
			"dest" -> JsString(dest.vessel.id),
			"volume" -> JsString(volume.toString),
			"pos" -> JsString(pos.toString)
		))
	}*/
}

class TipDispenseEventHandler extends EventHandler[TipState, TipDispenseEvent]("tip.dispense") {
	def handleEvent(state0: TipState, event: TipDispenseEvent): RqResult[TipState] = {
		val liquid = event.dest.content.liquid
		for {
			content_# <- state0.content.removeVolume(event.volume)
			state_# = dispense(state0, content_#, liquid, event.pos)
		} yield state_#
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
) extends Event[TipState] {
	def getStateOrId = Right(tip)
	/*def toJson: JsValue = {
		JsObject(Map(
			"kind" -> JsString("tip.mix"),
			"tip" -> JsString(tip.id),
			"src" -> JsString(src.vessel.id),
			"volume" -> JsString(volume.toString)
		))
	}*/
}

class TipMixEventHandler extends EventHandler[TipState, TipMixEvent]("tip.mix") {
	def handleEvent(state0: TipState, event: TipMixEvent) = {
		val liquid = event.src.content.liquid
		RqSuccess(state0.copy(
			src_? = Some(event.src),
			contamInside = state0.contamInside ++ liquid.contaminants,
			nContamInsideVolume = LiquidVolume.max(state0.nContamInsideVolume, event.volume),
			contamOutside = state0.contamOutside ++ liquid.contaminants,
			srcsEntered = state0.srcsEntered + liquid,
			cleanDegree = CleanIntensity.None,
			cleanDegreePrev = state0.cleanDegreePrev,
			cleanDegreePending = CleanIntensity.max(state0.cleanDegreePending, liquid.tipCleanPolicy.exit)
		))
	}
}

case class TipCleanEvent(
	tip: TipState,
	washProgram: WashProgram
) extends Event[TipState] {
	def getStateOrId = Right(tip)
}

class TipCleanEventHandler extends EventHandler[TipState, TipCleanEvent]("tip.clean") {
	def handleEvent(state0: TipState, event: TipCleanEvent) = {
		val w = event.washProgram
		val s = event.tip
		RqSuccess(s.copy(
			content = VesselContent.Empty,
			contamInside = s.contamInside -- w.contaminantsRemoved,
			contamOutside = s.contamOutside -- w.contaminantsRemoved,
			srcsEntered = Set(),
			destsEntered = Set(),
			cleanDegree = w.intensity,
			cleanDegreePrev = w.intensity,
			cleanDegreePending = if (s.cleanDegreePending <= w.intensity) CleanIntensity.None else s.cleanDegreePending
		))
	}
}
