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
		testFixed()
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
	
	def testFixed() {
		import meta.fixed._
		
		val liquidEmpty = new Liquid(null, false)
		val liquidCells = new Liquid("cells", true)
		
		val settings = new Settings(
			tips = Array(
				new Tip(0, 0, 960), new Tip(1, 0, 960), new Tip(2, 0, 960), new Tip(3, 0, 960), 
				new Tip(4, 0, 45), new Tip(5, 0, 45), new Tip(6, 0, 45), new Tip(7, 0, 45) 
			),
			liquids = Array(
				liquidEmpty,
				liquidCells
			)
		)
		
		val o = new OneOverConcrete(settings)
		
		val srcs = Array(Well(0, liquidCells, 100))
		val dests = Array(Well(0), Well(1), Well(2), Well(3), Well(4), Well(5), Well(6), Well(7))
		val volumes = Array[Double](20, 20, 20, 20, 20, 20, 20, 20)
		
		o.pipetteLiquid(srcs, dests, volumes,
				new AspirateStrategy(">> Water free dispense <<"),
				new DispenseStrategy("Water free dispense", false))
	}
}
