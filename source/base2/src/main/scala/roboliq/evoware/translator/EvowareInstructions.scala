package roboliq.evoware.translator

case class PipetterItem(
	syringe: Int,
	well: String,
	volume: String
)

case class PipetterAspirate(
	equipment: String,
	program: String,
	items: List[PipetterItem]
) {
/*
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
*/
}

case class SealerRun(
	equipment: String,
	program: String,
	`object`: String
)

case class TransporterMovePlate(
	equipment: String,
	program_? : Option[String],
	`object`: String,
	destination: String,
	evowareMoveBackToHome_? : Option[Boolean]
	//evowareLidHandling_? : Option[LidHandling.Value]
)
