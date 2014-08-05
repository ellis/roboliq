package roboliq.commands

import roboliq.core._
import roboliq.entities._
import scala.collection.mutable.SortedSet
import roboliq.input.Instruction

case class AgentActivate() extends Instruction {
	val effects = Nil
	val data = Nil
}

case class AgentDeactivate() extends Instruction {
	val effects = Nil
	val data = Nil
}

case class DeviceSiteClose(
	device: Device,
	site: Site
) extends Instruction {
	val effects = Nil
	val data = Nil
}

case class DeviceSiteOpen(
	device: Device,
	site: Site
) extends Instruction {
	val effects = Nil
	val data = Nil
}

case class EvowareSubroutine(path: String) extends Instruction {
	val effects = Nil
	val data = Nil
}

case class Log(text: String) extends Instruction {
	val effects = Nil
	val data = Nil
}

case class PipetterAspirate(
	val item_l: List[TipWellVolumePolicy]
) extends Instruction {
	val effects = List(new WorldStateEvent {
		def update(state: WorldStateBuilder): RqResult[Unit] = {
			for (item <- item_l) {
				val wellAliquot0 = state.well_aliquot_m.getOrElse(item.well, Aliquot.empty)
				val tipState0 = state.tip_state_m.getOrElse(item.tip, TipState.createEmpty(item.tip))
				val amount = Distribution.fromVolume(item.volume)
				val tipEvent = TipAspirateEvent(item.tip, item.well, wellAliquot0.mixture, item.volume)
				val x = for {
					wellAliquot1 <- wellAliquot0.remove(amount)
					tipState1 <- new TipAspirateEventHandler().handleEvent(tipState0, tipEvent)
				} yield {
					state.well_aliquot_m(item.well) = wellAliquot1
					state.tip_state_m(item.tip) = tipState1
					//println(s"aspirate: ${item.well.label} ${wellAliquot0} - ${amount} -> ${wellAliquot1}")
				}
				x match {
					case RqError(e, w) => return RqError(e, w)
					case _ =>
				}
			}
			RqSuccess(())
		}
	})
	val data = Nil
}

case class PipetterDispense(
	item_l: List[TipWellVolumePolicy],
	data: List[Object]
) extends Instruction {
	val effects = List(new WorldStateEvent {
		def update(state: WorldStateBuilder): RqResult[Unit] = {
			for (item <- item_l) {
				val wellAliquot0 = state.well_aliquot_m.getOrElse(item.well, Aliquot.empty)
				val tipState0 = state.tip_state_m.getOrElse(item.tip, TipState.createEmpty(item.tip))
				val amount = Distribution.fromVolume(item.volume)
				val tipEvent = TipDispenseEvent(item.tip, wellAliquot0.mixture, item.volume, item.policy.pos)
				val aliquot = Aliquot(tipState0.content.mixture, amount)
				val x = for {
					tipState1 <- new TipDispenseEventHandler().handleEvent(tipState0, tipEvent)
					wellAliquot1 <- wellAliquot0.add(aliquot)
				} yield {
					state.well_aliquot_m(item.well) = wellAliquot1
					state.tip_state_m(item.tip) = tipState1
					//println(s"dispense: ${item.well.label} ${wellAliquot0} + ${aliquot} -> ${wellAliquot1}")
				}
				x match {
					case RqError(e, w) => return RqError(e, w)
					case _ =>
				}
			}
			RqSuccess(())
		}
	})
}

case class PipetterTipsRefresh(
	device: Pipetter,
	// tip, clean intensity, tipModel_?
	item_l: List[(Tip, CleanIntensity.Value, Option[TipModel])]
) extends Instruction {
	val effects = List(new WorldStateEvent {
		def update(state: WorldStateBuilder): RqResult[Unit] = {
			for (item <- item_l) {
				val event = TipCleanEvent(item._1, item._2)
				val tipState0 = state.getTipState(item._1)
				val tipState_? = new TipCleanEventHandler().handleEvent(tipState0, event)
				tipState_? match {
					case RqError(e, w) => return RqError(e, w)
					case RqSuccess(tipState, _) =>
						state.tip_state_m(item._1) = tipState
				}
			}
			RqSuccess(())
		}
	})
	val data = Nil
}

object PipetterTipsRefresh {
	def combine(l: List[PipetterTipsRefresh]): List[PipetterTipsRefresh] = {
		l.groupBy(_.device).toList.map(pair => {
			val (device, refresh_l) = pair
			val item_l = refresh_l.flatMap(_.item_l)
			val tipToItems_m = item_l.groupBy(_._1)
			val item_l_~ = tipToItems_m.toList.sortBy(_._1).map(pair => {
				val (tip, item_l) = pair
				val intensity = CleanIntensity.max(item_l.map(_._2))
				(tip, intensity, item_l.last._3)
			})
			PipetterTipsRefresh(device, item_l_~)
		})
	}
}

/*case class PipetterTipsRefreshItem(
	tip: Tip,
	intensity: CleanIntensity.Value,
	
	// tip, clean intensity, tipModel_?
	item_l: List[(Tip, CleanIntensity.Value, Option[TipModel])]
) extends Command*/

case class PeelerRun(
	deviceIdent: String,
	specIdent: String,
	labwareIdent: String,
	siteIdent: String
) extends Instruction {
	val effects = List(new WorldStateEvent {
		def update(state: WorldStateBuilder): RqResult[Unit] = {
			// FIXME:
			//state1.labware_isSealed_l -= labware
			RqSuccess(())
		}
	})
	val data = Nil
}

case class Prompt(text: String) extends Instruction {
	val effects = Nil
	val data = Nil
}

case class SealerRun(
	deviceIdent: String,
	specIdent: String,
	labwareIdent: String,
	siteIdent: String
) extends Instruction {
	val effects = Nil
	val data = Nil
}

/**
 * @param object_l [(labwareIdent, siteIdent)]
 */
case class ShakerRun(
	device: Shaker,
	spec: ShakerSpec,
	labwareToSite_l: List[(Labware, Site)]
) extends Instruction {
	val effects = Nil
	val data = Nil
}

case class ThermocyclerClose(
	deviceIdent: String
) extends Instruction {
	val effects = Nil
	val data = Nil
}

case class ThermocyclerOpen(
	deviceIdent: String
) extends Instruction {
	val effects = Nil
	val data = Nil
}

case class ThermocyclerRun(
	deviceIdent: String,
	specIdent: String/*,
	plateIdent: String*/
) extends Instruction {
	val effects = Nil
	val data = Nil
}

case class TransporterRun(
	deviceIdent: String,
	labware: Labware,
	model: LabwareModel,
	origin: Site,
	destination: Site,
	vectorIdent: String
) extends Instruction {
	val effects = List(new WorldStateEvent {
		def update(state: WorldStateBuilder): RqResult[Unit] = {
			state.labware_location_m(labware) = destination
			RqSuccess(())
		}
	})
	val data = Nil
}
