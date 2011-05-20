package roboliq;

import concrete._
import evoware.EvowareSettings
import evoware.EvowareTranslator


object Main {
	sealed class PlateProperty
	case class Plate_Rows(val n: Int)
	case class Plate_Cols(val n: Int)
	
	def main(args: Array[String]): Unit = {
		testConcrete()
	}

	def testConcrete() {
		val rule1 = new PipettingRule(">> Water free dispense <<")
		val rule2 = new PipettingRule("Water free dispense")
		val template = new Plate(rows = 8, cols = 12)
		val empty = new Plate(rows = 8, cols = 12)
		
		val cmds = List[Token](
			Aspirate(
				volumes = Array(288, 288, 288, 288, 0, 0, 0, 0),
				plate = template,
				loc = 15,
				wells = Set(0, 1, 2, 3),
				rule1
			),
			Dispense(
				volumes = Array(288, 288, 288, 288, 0, 0, 0, 0),
				plate = empty,
				loc = 16,
				wells = Set(0, 1, 2, 3),
				rule2
			)
		)
		
		val settings = new EvowareSettings
		println(EvowareTranslator.translate(cmds, settings))
	}
}
