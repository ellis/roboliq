package roboliq.devices.pipette

import roboliq.common._
import roboliq.commands.pipette._


trait PipetteCommands extends RoboliqCommands {
	def mix(target: WellOrPlateOrLiquid, volume: Double, count: Int) {
		val mixSpec = new MixSpec(volume, count)
		val args = new L4A_MixArgs(Seq(target), mixSpec)
		val cmd = L4C_Mix(args)
		cmds += cmd
	}
	
	def pipette(source: WellOrPlateOrLiquid, dest: WellOrPlate, volume: Double) {
		val item = new L4A_PipetteItem(source, dest, volume)
		val cmd = L4C_Pipette(new L4A_PipetteArgs(Seq(item)))
		cmds += cmd
	}
	
	implicit def wellToWPL(o: Well): WellOrPlateOrLiquid = WPL_Well(o)
	implicit def plateToWPL(o: Plate): WellOrPlateOrLiquid = WPL_Plate(o)
	implicit def liquidToWPL(o: Liquid): WellOrPlateOrLiquid = WPL_Liquid(o)
	
	implicit def wellToWP(o: Well): WellOrPlate = WP_Well(o)
	implicit def plateToWP(o: Plate): WellOrPlate = WP_Plate(o)
}
