package roboliq.events

import spray.json._
import roboliq.core._, roboliq.entity._, roboliq.processor._



/** Represents an aspiration event. */
case class VesselAddEvent(
	vessel: VesselState,
	content: VesselContent
) extends Event {
}

class VesselAddEventHandler {// extends EventHandler {
	import RqFunctionHandler._
	
	def fnargs(event: VesselAddEvent) = {
		fnRequire () {
			val state0 = event.vessel
			val state_# = state0.copy(content = state0.content + event.content)
			for { json <- ConversionsDirect.toJson[VesselState](state_#) }
			yield List(EventItem_State(TKP("vesselState", event.vessel.id, Nil), json))
		}
	}
}

/** Represents an aspiration event. */
case class VesselRemoveEvent(
	vessel: Vessel,
	/** Volume in liters to remove. */
	volume: LiquidVolume
) extends Event {
}

class VesselRemoveEventHandler {// extends EventHandler {
	import RqFunctionHandler._
	
	def fnargs(event: VesselRemoveEvent) = {
		fnRequire (lookup[VesselState](event.vessel.id)) { state0 =>
			for {
				content_# <- state0.content.removeVolume(event.volume)
				state_# = state0.copy(content = content_#)
				json <- ConversionsDirect.toJson[VesselState](state_#)
			} yield List(EventItem_State(TKP("vesselState", event.vessel.id, Nil), json))
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
