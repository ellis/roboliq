package roboliq.commands.pcr

import roboliq.common._
import roboliq.commands._

case class L4C_PcrMix(args: L4A_PcrMixArgs) extends CommandL4 {
	type L3Type = L3C_PcrMix
	
	val setup = new L4A_PcrMixSetup

	def addKnowledge(kb: KnowledgeBase) {
		// TODO: note that plate will occupy the target location
		// TODO: request plate compatibility with this device
		args.items.foreach(_ match {
			case item: MixItemReagentL4 => kb.addWellPointer(item.reagent)
			case item: MixItemTemplateL4 => kb.addWellPointer(item.src)
		})
		kb.addWellPointer(args.dest, false)
		kb.addWellPointer(args.water)
		if (setup.well_masterMix != null)
			kb.addWellPointer(setup.well_masterMix)
	}
	
	def toL3(states: RobotState): Result[L3Type] = {
		for {
			items3 <- Result.sequence(args.items.map(_.toL3(states)))
			dests3 <- args.dest.getWells(states)
			plateHandling <- setup.plateHandling.toL3(states)
			water <- args.water.getWells(states)
			masterMixWells <- if (setup.well_masterMix != null) setup.well_masterMix.getWells(states) else Success(Seq())
		} yield {
			L3C_PcrMix(new L3A_PcrMixArgs(
				items = items3,
				dests = dests3,
				v1 = args.v1,
				water,
				masterMixWells = masterMixWells,
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
	val water: WellPointer,
	val v1: Double
)

class L3A_PcrMixArgs(
	val items: Seq[MixItemL3],
	val dests: Seq[WellConfigL2],
	val v1: Double,
	val water: Seq[WellConfigL2],
	val masterMixWells: Seq[WellConfigL2],
	val plateHandling: PlateHandlingConfig
)
