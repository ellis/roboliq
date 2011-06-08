package roboliq;

import roboliq.parts._
import roboliq.tokens._
import roboliq.commands._
import roboliq.robot._
import evoware.EvowareRobot
import evoware.EvowareSettings
import evoware.EvowareTipKind
import evoware.EvowareTranslator


object Main {
	def main(args: Array[String]): Unit = {
		//testConcrete()
		//test2()
		//EvowareTranslator.test()
		Tests.a()
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
		
		val tipKindL = new EvowareTipKind(0, Map(
				"Aspirate" -> "Water free dispense",
				"Dispense Enter" -> "Water wet contact",
				"Dispense Hover" -> "Water free dispense"))
		val tipKindS = new EvowareTipKind(1, Map(
				"Aspirate" -> "D-BSSE Te-PS Wet Contact",
				"Dispense Enter" -> "D-BSSE Te-PS Wet Contact",
				"Dispense Hover" -> "D-BSSE Te-PS Dry Contact"))

		val evowareSettings = new EvowareSettings(
			grids = Map(
				carrier -> 17
			),
			mapTipIndexToKind = Map(
				0 -> tipKindL, 1 -> tipKindL, 2 -> tipKindL, 3 -> tipKindL,
				4 -> tipKindS, 5 -> tipKindS, 6 -> tipKindS, 7 -> tipKindS
			)
		)

		val builder = new RobotStateBuilder(RobotState.empty)
		builder.sites += (plate1 -> new Site(carrier, 0))
		builder.sites += (plate2 -> new Site(carrier, 1))
		val state = builder.toImmutable
		
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
		val liquid1 = new Liquid("template", true)
		
		val robotConfig = new RobotConfig(tips, tipGroups)
		val robot = new EvowareRobot(robotConfig)

		val builder = new RobotStateBuilder(RobotState.empty)
		builder.sites += (plate1 -> new Site(carrier, 0))
		builder.sites += (plate2 -> new Site(carrier, 1))
		builder.fillWells(plate1.wells, liquid1, 50)
		robot.state = builder.toImmutable
		
		val p = new PipetteLiquid(
				robot = robot,
				srcs = plate1.wells.take(1),
				dests = plate2.wells.take(4),
				volumes = Array(3, 3, 3, 3),
				aspirateStrategy = rule1,
				dispenseStrategy = rule2)
		
		val tipKindL = new EvowareTipKind(0, Map(
				"Aspirate" -> "Water free dispense",
				"Dispense Enter" -> "Water wet contact",
				"Dispense Hover" -> "Water free dispense"))
		val tipKindS = new EvowareTipKind(1, Map(
				"Aspirate" -> "D-BSSE Te-PS Wet Contact",
				"Dispense Enter" -> "D-BSSE Te-PS Wet Contact",
				"Dispense Hover" -> "D-BSSE Te-PS Dry Contact"))

		val evowareSettings = new EvowareSettings(
			grids = Map(
				carrier -> 17
			),
			mapTipIndexToKind = Map(
				0 -> tipKindL, 1 -> tipKindL, 2 -> tipKindL, 3 -> tipKindL,
				4 -> tipKindS, 5 -> tipKindS, 6 -> tipKindS, 7 -> tipKindS
			)
		)
		
		println(EvowareTranslator.translate(p.tokens, evowareSettings, robot.state))
	}
}
