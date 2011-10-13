/*package roboliq.devices.pipette

import scala.collection.mutable.Queue

import roboliq.common._
import roboliq.commands._
import roboliq.commands.pipette._
import roboliq.compiler._


class PipettePlanner(
	val robot: PipetteDevice,
	val ctx: CompilerContextL3
) {
	def translate(tipGroups: Seq[Seq[TipConfigL2]]): Result[Seq[Command]] = {
		
		lnCostMin = new Array(nItems)
		Success(Seq())
	}
	
	def x2(liItem: Array[Int], tipGroups: Seq[Seq[TipConfigL2]]) {
		val nStates = liItem.size
		val frontier = new Queue[Int] 
	}
}
*/