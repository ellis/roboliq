import org.scalatest.FunSuite

import roboliq.parts._
import roboliq.robot._
import roboliq.tokens._
import evoware._
import bsse._


class BsseSuite extends FunSuite {
	test("example") {
		val carrier = new Carrier
		val plate1 = new Plate(nRows = 8, nCols = 12)
		val plate2 = new Plate(nRows = 8, nCols = 12)

		val evowareSetupState = new EvowareSetupState(
			grids = Map(
				carrier -> 17
			)
		)

		val robot = new BsseRobot(evowareSetupState)

		val builder = new RobotStateBuilder(robot.state)
		builder.sites += (plate1 -> new Site(carrier, 0))
		builder.sites += (plate2 -> new Site(carrier, 1))

		val water = new Liquid(
			sName = "water",
			bWaterFreeDispense = true,
			bRequireDecontamBeforeAspirate = false,
			bCells = false,
			bDna = false,
			bOtherContaminant = false)
		for (well <- plate1.wells) {
			builder.addLiquid0(well, water, 1000)
		}

		robot.state = builder.toImmutable
		val tips = robot.tips.toArray

		val cmds = List[T1_Token](
			T1_Aspirate(Array(
				new TipWellVolume(tips(0), plate1.wells(0), 288),
				new TipWellVolume(tips(1), plate1.wells(1), 288),
				new TipWellVolume(tips(2), plate1.wells(2), 288),
				new TipWellVolume(tips(3), plate1.wells(3), 288)
				)
			),
			T1_Dispense(Array(
				new TipWellVolumeDispense(tips(0), plate2.wells(0), 30, DispenseKind.Free),
				new TipWellVolumeDispense(tips(1), plate2.wells(1), 30, DispenseKind.Free),
				new TipWellVolumeDispense(tips(2), plate2.wells(2), 30, DispenseKind.Free),
				new TipWellVolumeDispense(tips(3), plate2.wells(3), 30, DispenseKind.Free)
				)
			)
		)

		val translator = new BsseTranslator(robot)
		val s = translator.translateToString(cmds)
		println(s)
	}
}
