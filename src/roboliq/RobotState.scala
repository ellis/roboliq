package roboliq

abstract class RobotConfig(
		val tips: Array[Tip],
		val tipGroups: Array[Array[Int]]
)

/*
class Robot {
	def createDispenseTokens(units: Seq[DispenseUnit]): List[concrete.Token] = {
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
	}
}
*/
