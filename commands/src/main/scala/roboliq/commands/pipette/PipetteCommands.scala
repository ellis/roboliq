package roboliq.commands.pipette

import roboliq.common._
import roboliq.commands.pipette._


trait PipetteCommands extends RoboliqCommands {
	def mix(target: WellPointer, volume: Double, count: Int) {
		val mixSpec = new MixSpec(volume, count)
		val args = new L4A_MixArgs(Seq(target), mixSpec)
		val cmd = L4C_Mix(args)
		cmds += cmd
	}
	
	def pipette(source: WellPointer, dest: WellPointer, volume: Double) {
		val item = new L4A_PipetteItem(source, dest, volume)
		val cmd = L4C_Pipette(new L4A_PipetteArgs(Seq(item)))
		cmds += cmd
	}
}
