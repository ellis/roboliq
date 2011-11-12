package roboliq.roboease

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._


class Robolib(shared: ParserSharedData) {
	import WellPointerImplicits._
	import CmdLogImplicits._
	
	implicit def reagentSpecToPointer(o: Reagent): WellPointer = WellPointerReagent(o.reagent)
	implicit def commandToTuple(o: Command): Tuple2[Seq[Command], Seq[String]] = Seq(o) -> Seq()

	def prepareMix(mixdef: MixDef, nFactor: Double): Result[CmdLog] = {
		val nDests = mixdef.reagent.wells.size
		val dests = mixdef.reagent.wells.toSeq
		
		val items =
			for ((reagent, nVolume) <- mixdef.items; dest <- dests)
			yield new L4A_PipetteItem(reagent, dest, Seq(nVolume * nFactor / nDests))
		
		for (cmd <- PipetteCommandsL4.pipette(items)) yield {
			// TODO: add log?
			CmdLog(cmd)
		}
	}
}