package roboliq

import roboliq.parts._


case class DispenseSubunit(val tip: Tip, val dest: Well, val nVolume: Double)
case class DispenseUnit(val subunits: Seq[DispenseSubunit])

abstract class Robot {
	def createDispenseTokens(dispenses: Seq[DispenseUnit]): List[concrete.Token]
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
