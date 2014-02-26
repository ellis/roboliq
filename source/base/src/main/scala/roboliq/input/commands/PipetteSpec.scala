package roboliq.input.commands

import roboliq.core._
import roboliq.entities._


case class PipetteSpec(
	sources: PipetteSources,
	destinations: PipetteDestinations,
	volume_l: List[LiquidVolume],
	pipettePolicy_? : Option[String],
	sterilize_? : Option[CleanIntensity.Value],
	cleanBefore_? : Option[CleanIntensity.Value],
	sterilizeBetween_? : Option[CleanIntensity.Value],
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
	
}

case class PipetteSpecList(
	step_l: List[PipetteSpec]
)