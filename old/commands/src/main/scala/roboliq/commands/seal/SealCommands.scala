package roboliq.commands.seal

import roboliq.common._
import roboliq.commands._


trait SealCommands extends RoboliqCommands {
	def seal(plate: PlateObj): L4A_SealSetup = {
		val args = new L4A_SealArgs(plate)
		val cmd = L4C_Seal(args)
		cmds += cmd
		cmd.setup
	}

	def peel(plate: PlateObj): L4A_PeelSetup = {
		val args = new L4A_PeelArgs(plate)
		val cmd = L4C_Peel(args)
		cmds += cmd
		cmd.setup
	}
}

object SealCommandsL4 {
	def seal(wells: WellPointer): Result[List[L4C_Seal]] = {
		for {
			plates <- wells.getPlatesL4
		} yield {
			plates.toList.distinct.map(plate => {
				L4C_Seal(new L4A_SealArgs(plate))
			})
		}
	}
}