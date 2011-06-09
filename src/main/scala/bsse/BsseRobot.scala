/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package bsse

import roboliq.parts._


sealed class BsseTipKind(
	val sName: String,
	val nAspirateVolumeMin: Double,
	val nHoldVolumeMax: Double,
	val nDecontaminateVolumeExtra: Double
)

//sealed class BsseTip(val roboTip: Tip)

class BsseRobot {
	val nFreeDispenseVolumeThreshold = 10
	val tipKind1000 = new BsseTipKind("large", 2, 950, 50)
	val tipKind50 = new BsseTipKind("small", 0.01, 45, 5)
	val tips = (0 until 8).map(new Tip(_))
	val tipKinds = (0 until 8).map(if (_ < 4) tipKind1000 else tipKind50)
	
	private def getTipKind(tip: Tip): BsseTipKind = tipKinds(tip.index)
	
	def getTipAspirateVolumeMin(tip: Tip, liquid: Liquid): Double = {
		val tipKind = getTipKind(tip)
		tipKind.nAspirateVolumeMin
	}
	
	def getTipHoldVolumeMax(tip: Tip, liquid: Liquid): Double = {
		val tipKind = getTipKind(tip)
		val nReduce =
			if (liquid.contaminates)
				tipKind.nDecontaminateVolumeExtra
			else
				0
		tipKind.nHoldVolumeMax - nReduce
	}
	
	def getDispenseKind(tip: Tip, liquid: Liquid, nVolume: Double, wellState: WellState): DispenseKind.Value = {
		if (nVolume >= nFreeDispenseVolumeThreshold || liquid.bCells)
			DispenseKind.Free
		else if (wellState.nVolume == 0)
			DispenseKind.DryContact
		else
			DispanseKind.WetContact
	}
}
