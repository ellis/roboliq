package roboliq.commands.pcr

import roboliq.common._
import roboliq.commands._

case class L4C_PcrMix(args: L4A_PcrMixArgs) extends CommandL4 {
	type L3Type = L3C_PcrMix
	
	val setup = new L4A_PcrMixSetup

	def addKnowledge(kb: KnowledgeBase) {
		// TODO: note that plate will occupy the target location
		// TODO: request plate compatibility with this device
	}
	
	def toL3(states: RobotState): Result[L3Type] = {
		for {
			items3 <- Result.sequence(args.items.map(_.toL3(states)))
			dests3 <- args.dest.getWells(states)
			plateHandling <- setup.plateHandling.toL3(states)
			well_masterMix_? <- if (setup.well_masterMix != null) Some(setup.well_masterMix) else None,
		} yield {
			L3C_PcrMix(new L3A_PcrMixArgs(
				items = items3,
				dests = dests3,
				v1 = args.v1,
				well_masterMix_? = if (setup.well_masterMix != null) Some(setup.well_masterMix) else None,
				plateHandling = plateHandling
			))
		}
	}
}

case class L3C_PcrMix(args: L3A_PcrMixArgs) extends CommandL3

class L4A_PcrMixSetup {
	var well_masterMix: WellPointer = null
	val plateHandling = new PlateHandlingSetup
}

class L4A_PcrMixArgs(
	val items: Seq[MixItemL4],
	val dest: WellPointer,
	val v1: Double
)

class L3A_PcrMixArgs(
	val items: Seq[MixItemL3],
	val dests: Seq[WellConfigL2],
	val v1: Double,
	val well_masterMix_? : Option[Seq[WellConfigL2]],
	val plateHandling: PlateHandlingConfig
)
