package roboliq.robot

import roboliq.parts._
import roboliq.tokens._


trait Robot {
	val state0 = new RobotState(None, Map())
	val config: RobotConfig
	def state: RobotState
	//def createDispenseTokens(dispenses: Seq[DispenseUnit]): List[concrete.Token]
	def chooseWellsForTips(tips: Seq[Tip], wells: Seq[Well]): Seq[Well]
	def score(tokens: Seq[Token]): Int
	/*def createDispenseTokens(units: Seq[DispenseUnit]): List[concrete.Token] = {
		//case class Aspirate(volumes: Array[Double], plate: Plate, loc: Int, wells: Set[Int], rule: AspirateStrategy) extends Token
		//case class Dispense(volumes: Array[Double], plate: Plate, loc: Int, wells: Set[Int], rule: DispenseStrategy) extends Token
		val tokens = new ArrayBuffer[concrete.Token]
		for (unit <- units) {
			val holder = unit.subunits.head.dest.holder
			tokens += new concrete.Dispense(
				volumes = unit.subunits.map(_.nVolume),
				plate = new concrete.Plate(holder.nRows, holder.nCols),
				loc = 0,
				
			)
			for (subunit <- unit.subunits) {
			}
		}
		tokens.toList
	}*/
}
