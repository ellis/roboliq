package roboliq.token

import roboliq.part._


sealed class Token
case class Aspirate(volumes: Array[Double], plate: Plate, loc: Int, wells: Set[Int], rule: AspirateStrategy) extends Token
case class Dispense(volumes: Array[Double], plate: Plate, loc: Int, wells: Set[Int], rule: DispenseStrategy) extends Token
