package roboliq;

import roboliq.parts._
import roboliq.tokens._
import roboliq.commands._
import evoware.EvowareSettings
import evoware.EvowareTranslator


object Main {
	def main(args: Array[String]): Unit = {
		testConcrete()
		testFixed()
	}

	def testConcrete() {
		val rule1 = new AspirateStrategy(">> Water free dispense <<")
		val rule2 = new DispenseStrategy("Water free dispense", false)
		val carrier1 = new Carrier
		val carrier2 = new Carrier
		val plate_template = new Plate(rows = 8, cols = 12)
		val plate_empty = new Plate(rows = 8, cols = 12)
		
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
			tipGroups = Array(Array(0, 1, 2, 3), Array(4, 5, 6, 7), Array(0, 1, 2, 3, 4, 5, 6, 7)),
			liquids = Array(
				liquidEmpty,
				liquidCells
			)
		)
		
		val o = new OneOverConcrete(settings)
		
		val srcs = Array(Well(0, liquidCells, 100))
		val plate = Plate.create(0, 8, 1)
		//val dests = Array(Well(0), Well(1), Well(2), Well(3), Well(4), Well(5), Well(6), Well(7))
		val volumes = Array[Double](20, 20, 20, 20, 20, 20, 20, 20)
		
		o.pipetteLiquid(srcs, plate.wells, volumes,
				new AspirateStrategy(">> Water free dispense <<"),
				new DispenseStrategy("Water free dispense", false))
	}
}
