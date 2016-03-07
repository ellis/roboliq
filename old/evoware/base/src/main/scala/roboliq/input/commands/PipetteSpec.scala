package roboliq.input.commands

import roboliq.core._
import roboliq.entities._


case class PipetteSpec(
	sources: PipetteSources,
	destinations: PipetteDestinations,
	volume_l: List[LiquidVolume],
	pipettePolicy_? : Option[String],
	clean_? : Option[CleanIntensity.Value],
	cleanBefore_? : Option[CleanIntensity.Value],
	cleanBetween_? : Option[CleanIntensity.Value],
	cleanAfter_? : Option[CleanIntensity.Value],
	tipModel_? : Option[TipModel]
) {
	/**
	 * Split this spec into multiple specs, whenever a prior destination well is used as a source well. 
	 */
	def split(): List[PipetteSpec] = {
		val i = splitIndex
		if (i <= 0) {
			List(this)
		}
		else {
			val spec1 = this.copy(
				sources = PipetteSources(sources.sources.take(i)),
				destinations = PipetteDestinations(destinations.l.take(i))
			)
			val spec2 = this.copy(
				sources = PipetteSources(sources.sources.drop(i)),
				destinations = PipetteDestinations(destinations.l.drop(i))
			)
			spec1 :: spec2.split
		}
	}
	
	private def splitIndex(): Int = {
		var destSeen_l = Set[Well]()
		for (((src, dst), i) <- (sources.sources zip destinations.l).zipWithIndex) {
			if (!src.l.map(_.well).toSet.intersect(destSeen_l).isEmpty)
				return i
			destSeen_l += dst.well
		}
		-1
	}
	
	def getWellEvents: List[WorldStateEvent] = List(new WorldStateEvent {
		def update(state: WorldStateBuilder): RqResult[Unit] = {
			// Add liquid to destination
			val n = destinations.l.length
			val source_l = if (sources.sources.length == 1) List.fill(n)(sources.sources.head.l.head) else sources.sources.map(_.l.head)
			val volume2_l = if (volume_l.length == 1) List.fill(n)(volume_l.head) else volume_l
			for ((dst, src, volume) <- (destinations.l, source_l, volume2_l).zipped) {
				val srcAliquot = state.well_aliquot_m.getOrElse(src.well, Aliquot.empty)
				val dstAliquot0 = state.well_aliquot_m.getOrElse(dst.well, Aliquot.empty)
	            val amount = Distribution.fromVolume(volume)
				val aliquot = Aliquot(srcAliquot.mixture, amount)
				val x = for {
					dstAliquot1 <- dstAliquot0.add(aliquot)
				} yield {
					//println(s"update: ${dst.well.label} ${dstAliquot0} + ${aliquot} -> ${dstAliquot1}")
					state.well_aliquot_m(dst.well) = dstAliquot1
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

case class PipetteSpecList(
	step_l: List[PipetteSpec]
)