package concrete

sealed class Plate(val rows: Int, val cols: Int)
sealed class AspirateStrategy(val sName: String)
sealed class DispenseStrategy(val sName: String, val bEnter: Boolean)

sealed class Token
case class Aspirate(volumes: Array[Double], plate: Plate, loc: Int, wells: Set[Int], rule: AspirateStrategy) extends Token
case class Dispense(volumes: Array[Double], plate: Plate, loc: Int, wells: Set[Int], rule: DispenseStrategy) extends Token
