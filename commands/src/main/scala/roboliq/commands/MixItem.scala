package roboliq.commands

import roboliq.common._


sealed trait MixItemL4 {
	def toL3(states: RobotState): Result[MixItemL3]
}
case class MixItemTemplateL4(src: WellPointer, lc0: Seq[Double], c1: Double, vMin: Double = 0, vMax: Double = Double.MaxValue) extends MixItemL4 {
	def toL3(states: RobotState): Result[MixItemTemplateL3] = {
		for { srcs3 <- src.getWells(states) }
		yield MixItemTemplateL3(srcs3, lc0, c1, vMin, vMax)
	}
}
case class MixItemReagentL4(reagent: WellPointerReagent, c0: Double, c1: Double, vMin: Double = 0, vMax: Double = Double.MaxValue) extends MixItemL4 {
	def toL3(states: RobotState): Result[MixItemReagentL3] = {
		for { srcs3 <- reagent.getWells(states) }
		yield MixItemReagentL3(srcs3, c0, c1, vMin, vMax)
	}
}

sealed trait MixItemL3
case class MixItemTemplateL3(srcs: Seq[WellConfigL2], lc0: Seq[Double], c1: Double, vMin: Double = 0, vMax: Double = Double.MaxValue) extends MixItemL3
case class MixItemReagentL3(srcs: Seq[WellConfigL2], c0: Double, c1: Double, vMin: Double = 0, vMax: Double = Double.MaxValue) extends MixItemL3
