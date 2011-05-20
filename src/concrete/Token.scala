package concrete

sealed class Plate(
	val rows: Int, val cols: Int, val loc: Int
)

sealed class PipettingRule(
	val name: String
)

sealed class Token
case class Aspirate(volumes: Array[Double], plate: Plate, pattern: Set[Int], rule: PipettingRule) extends Token
case class Dispense(volumes: Array[Double], plate: Plate, pattern: Set[Int], rule: PipettingRule) extends Token
