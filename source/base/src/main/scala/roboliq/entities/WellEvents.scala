package roboliq.entities

import scalaz._
import Scalaz._
import roboliq.core._

sealed trait WellEvent

case class WellEvent_TipDetectVolume(
	time: java.util.Date,
	tipIndex: Int,
	tipModel: String,
	site: String,
	labwareModel: String,
	row: Int,
	col: Int,
	measurement: BigDecimal
) extends WellEvent {
	
}

case class WellEvent_Aspirate(
	time: java.util.Date,
	spec: String,
	tipIndex: Int,
	tipModel: String,
	site: String,
	labwareModel: String,
	row: Int,
	col: Int,
	volume: LiquidVolume
) extends WellEvent

case class WellEvent_Dispense(
	time: java.util.Date,
	spec: String,
	tipIndex: Int,
	tipModel: String,
	tipVolume0: LiquidVolume,
	tipVolume: LiquidVolume,
	site: String,
	labwareModel: String,
	row: Int,
	col: Int,
	multipipetteStep: Int,
	volume: LiquidVolume
) extends WellEvent

trait WorldStateEvent {
	def update(builder: WorldStateBuilder): RqResult[Unit]
}

object WorldStateEvent {
	def update(event_l: List[WorldStateEvent], state: WorldStateBuilder): RqResult[Unit] = {
		def step(event_l: List[WorldStateEvent]): RqResult[Unit] = {
			event_l match {
				case Nil => RqSuccess(())
				case event :: rest =>
					for {
						_ <- event.update(state)
						_ <- step(rest)
					} yield ()
			}
		}
		step(event_l)
	}
	
	def update(event_l: List[WorldStateEvent], state: WorldState): RqResult[WorldState] = {
		val builder = state.toMutable
		for {
			_ <- update(event_l, builder)
		} yield builder.toImmutable
	}
}

case class WellAspirateEvent(
	well: Well,
	volume: LiquidVolume
) extends WorldStateEvent {
	def update(state: WorldStateBuilder): RqResult[Unit] = {
        val wellAliquot0 = state.well_aliquot_m.getOrElse(well, Aliquot.empty)
        val amount = Distribution.fromVolume(volume)
		for {
			wellAliquot1 <- wellAliquot0.remove(amount)
		} yield {
			state.well_aliquot_m(well) = wellAliquot1
		}
	}
}

case class WellDispenseEvent(
	well: Well,
	aliquot: Aliquot
) extends WorldStateEvent {
	def update(state: WorldStateBuilder): RqResult[Unit] = {
        val wellAliquot0 = state.well_aliquot_m.getOrElse(well, Aliquot.empty)
		for {
			wellAliquot1 <- wellAliquot0.add(aliquot)
		} yield {
			state.well_aliquot_m(well) = wellAliquot1
		}
	}
}
