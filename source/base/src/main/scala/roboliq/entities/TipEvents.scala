package roboliq.entities

import roboliq.core._

trait Event[A]
abstract class EventHandlerAB[A, B](id: String)

/** Represents an aspiration event. */
case class TipAspirateEvent(
	tip: Tip,
	/** Source well ID. */
	src: Well,
	mixture: Mixture,
	/** Volume to aspirate. */
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

class TipAspirateEventHandler extends EventHandlerAB[TipState, TipAspirateEvent]("tip.aspirate") {
	def handleEvent(state0: TipState, event: TipAspirateEvent): RsResult[TipState] = {
		//val liquid = event.src.content.liquid
		for {
			content_~ <- state0.content.add(Aliquot(event.mixture, Distribution.fromVolume(event.volume)))
			volume_~ <- content_~.distribution.toVolume
		} yield {
			TipState(
				state0.conf,
				state0.model_?,
				Some(event.src),
				content_~,
				state0.contamInside ++ event.mixture.contaminants,
				LiquidVolume.max(state0.nContamInsideVolume, volume_~),
				state0.contamOutside ++ event.mixture.contaminants,
				state0.srcsEntered + event.mixture,
				state0.destsEntered,
				CleanIntensity.None,
				state0.cleanDegreePrev,
				CleanIntensity.max(state0.cleanDegreePending, event.mixture.tipCleanPolicy.exit)
			)
		}
	}
}

/** Represents an dispense event. */
case class TipDispenseEvent(
	tip: Tip,
	/** Destination mixture */
	mixtureDst: Mixture,
	/** Volume in liters to dispense. */
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

class TipDispenseEventHandler extends EventHandlerAB[TipState, TipDispenseEvent]("tip.dispense") {
	def handleEvent(state0: TipState, event: TipDispenseEvent): RqResult[TipState] = {
		for {
			content_# <- state0.content.remove(Distribution.fromVolume(event.volume))
			state_# = dispense(state0, content_#, event.mixtureDst, event.pos)
		} yield state_#
	}

	private def dispense(state0: TipState, content: Aliquot, mixtureDst: Mixture, pos: PipettePosition.Value): TipState = {
		pos match {
			case PipettePosition.WetContact => dispenseIn(state0, content, mixtureDst)
			case _ => dispenseFree(state0, content)
		}
	}
	
	private def dispenseFree(state0: TipState, content: Aliquot): TipState = {
		state0.copy(
			content = content,
			cleanDegree = CleanIntensity.None
		)
	}
	
	private def dispenseIn(state0: TipState, content: Aliquot, mixtureDst: Mixture): TipState = {
		state0.copy(
			content = content,
			contamOutside = state0.contamOutside ++ mixtureDst.contaminants,
			destsEntered = state0.destsEntered + mixtureDst,
			cleanDegree = CleanIntensity.None,
			cleanDegreePending = CleanIntensity.max(state0.cleanDegreePending, mixtureDst.tipCleanPolicy.exit)
		)
	}
}

/** Represents an aspiration event. */
case class TipMixEvent(
	tip: Tip,
	/** Mixture in target well. */
	mixture: Mixture,
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

class TipMixEventHandler extends EventHandlerAB[TipState, TipMixEvent]("tip.mix") {
	def handleEvent(state0: TipState, event: TipMixEvent) = {
		RqSuccess(state0.copy(
			src_? = None,
			contamInside = state0.contamInside ++ event.mixture.contaminants,
			nContamInsideVolume = LiquidVolume.max(state0.nContamInsideVolume, event.volume),
			contamOutside = state0.contamOutside ++ event.mixture.contaminants,
			srcsEntered = state0.srcsEntered + event.mixture,
			cleanDegree = CleanIntensity.None,
			cleanDegreePending = CleanIntensity.max(state0.cleanDegreePending, event.mixture.tipCleanPolicy.exit)
		))
	}
}

case class TipCleanEvent(
	tip: Tip,
	intensity: CleanIntensity.Value
	//washProgram: WashProgram
) extends Event[TipState] {
	def getStateOrId = Right(tip)
}

class TipCleanEventHandler extends EventHandlerAB[TipState, TipCleanEvent]("tip.clean") {
	def handleEvent(state0: TipState, event: TipCleanEvent): RqResult[TipState] = {
		//val w = event.washProgram
		val s = state0
		RqSuccess(s.copy(
			content = Aliquot.empty,
			contamInside = Set(), //s.contamInside -- w.contaminantsRemoved,
			contamOutside = Set(), //s.contamOutside -- w.contaminantsRemoved,
			srcsEntered = Set(),
			destsEntered = Set(),
			cleanDegree = event.intensity,
			cleanDegreePrev = event.intensity,
			cleanDegreePending = if (s.cleanDegreePending <= event.intensity) CleanIntensity.None else s.cleanDegreePending
		))
	}
}

case class TipAspirateEvent2(event: TipAspirateEvent) extends WorldStateEvent {
	def update(state: WorldStateBuilder): RqResult[Unit] = {
		val tipState0 = state.tip_state_m.getOrElse(event.tip, TipState.createEmpty(event.tip))
		for {
			tipState1 <- new TipAspirateEventHandler().handleEvent(tipState0, event)
		} yield {
			state.tip_state_m(event.tip) = tipState1
		}
	}
}

case class TipDispenseEvent2(event: TipDispenseEvent) extends WorldStateEvent {
	def update(state: WorldStateBuilder): RqResult[Unit] = {
		val tipState0 = state.tip_state_m.getOrElse(event.tip, TipState.createEmpty(event.tip))
		for {
			tipState1 <- new TipDispenseEventHandler().handleEvent(tipState0, event)
		} yield {
			state.tip_state_m(event.tip) = tipState1
		}
	}
}
