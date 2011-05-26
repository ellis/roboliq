package roboliq;

import roboliq.parts._
import roboliq.tokens._
import roboliq.commands._
import roboliq.robot._
import evoware.EvowareRobot
import evoware.EvowareSettings
import evoware.EvowareTranslator
import evoware.Loc


object Main {
	def main(args: Array[String]): Unit = {
		//testConcrete()
		test2()
	}

	def testConcrete() {
		val tips = Array(
			new Tip(0, 0, 960), new Tip(1, 0, 960), new Tip(2, 0, 960), new Tip(3, 0, 960), 
			new Tip(4, 0, 45), new Tip(5, 0, 45), new Tip(6, 0, 45), new Tip(7, 0, 45) 
		)
		val rule1 = new AspirateStrategy(">> Water free dispense <<")
		val rule2 = new DispenseStrategy("Water free dispense", false)
		val carrier = new Carrier
		val plate1 = new Plate(nRows = 8, nCols = 12)
		val plate2 = new Plate(nRows = 8, nCols = 12)
		
		val cmds = List[Token](
			Aspirate(Array(
					new TipWellVolume(tips(0), plate1.wells(0), 288),
					new TipWellVolume(tips(1), plate1.wells(1), 288),
					new TipWellVolume(tips(2), plate1.wells(2), 288),
					new TipWellVolume(tips(3), plate1.wells(3), 288)
					),
					rule1
			),
			Dispense(Array(
					new TipWellVolume(tips(0), plate2.wells(0), 3),
					new TipWellVolume(tips(1), plate2.wells(1), 3),
					new TipWellVolume(tips(2), plate2.wells(2), 3),
					new TipWellVolume(tips(3), plate2.wells(3), 3)
					),
					rule2
			)
		)
		
		val evowareSettings = new EvowareSettings(Map(
				carrier -> 15
		))

		val builder = new RobotStateBuilder(None)
		builder.sites += (plate1 -> new Site(carrier, 0))
		builder.sites += (plate2 -> new Site(carrier, 1))
		val state = builder.toState
		
		println(EvowareTranslator.translate(cmds, evowareSettings, state))
	}
	
	def test2() {
		val tips = Array(
			new Tip(0, 0, 960), new Tip(1, 0, 960), new Tip(2, 0, 960), new Tip(3, 0, 960), 
			new Tip(4, 0, 45), new Tip(5, 0, 45), new Tip(6, 0, 45), new Tip(7, 0, 45) 
		)
		val tipGroups = Array(Array(0, 1, 2, 3), Array(4, 5, 6, 7), Array(0, 1, 2, 3, 4, 5, 6, 7))
		val rule1 = new AspirateStrategy(">> Water free dispense <<")
		val rule2 = new DispenseStrategy("Water free dispense", false)
		val carrier = new Carrier
		val plate1 = new Plate(nRows = 8, nCols = 12)
		val plate2 = new Plate(nRows = 8, nCols = 12)
		
		val robotConfig = new RobotConfig(tips, tipGroups)
		val robot = new EvowareRobot(robotConfig)
		
		val p = new PipetteLiquid(
				settings = robotConfig,
				robot = robot,
				srcs = plate1.wells.take(4),
				dests = plate2.wells.take(4),
				volumes = Array(3, 3, 3, 3),
				aspirateStrategy = rule1,
				dispenseStrategy = rule2)
		
		val evowareSettings = new EvowareSettings(Map(
				carrier -> 15
		))

		val builder = new RobotStateBuilder(None)
		builder.sites += (plate1 -> new Site(carrier, 0))
		builder.sites += (plate2 -> new Site(carrier, 1))
		val state = builder.toState
		
		println(EvowareTranslator.translate(p.tokens, evowareSettings, state))
	}
	
	/*
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
	*/
}
