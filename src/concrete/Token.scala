package concrete

sealed class Plate(val rows: Int, val cols: Int)

sealed class PipettingRule(val name: String)

sealed class Token
case class Aspirate(volumes: Array[Double], plate: Plate, loc: Int, wells: Set[Int], rule: PipettingRule) extends Token
case class Dispense(volumes: Array[Double], plate: Plate, loc: Int, wells: Set[Int], rule: PipettingRule) extends Token
