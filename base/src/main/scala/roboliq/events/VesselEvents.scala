package roboliq.events

import roboliq.core._, roboliq.entity._, roboliq.processor._


/** Represents an aspiration event. */
case class VesselAddEvent(
	vessel: VesselState,
	content: VesselContent
) extends Event[VesselState] {
	def getStateOrId = Right(vessel)
}

class VesselAddEventHandler extends EventHandlerAB[VesselState, VesselAddEvent]("vessel.add") {
	def handleEvent(state0: VesselState, event: VesselAddEvent) = {
		RqSuccess(state0.copy(content = state0.content + event.content))
	}
}

/** Represents an aspiration event. */
case class VesselRemoveEvent(
	vessel: VesselState,
	/** Volume in liters to remove. */
	volume: LiquidVolume
) extends Event[VesselState] {
	def getStateOrId = Right(vessel)
}

class VesselRemoveEventHandler extends EventHandlerAB[VesselState, VesselRemoveEvent]("vessel.remove") {
	def handleEvent(state0: VesselState, event: VesselRemoveEvent) = {
		for {
			content_# <- state0.content.removeVolume(event.volume)
			state_# = state0.copy(content = content_#)
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
