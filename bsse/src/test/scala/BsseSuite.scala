import org.scalatest.FunSuite

import roboliq.parts._
import roboliq.robot._
import roboliq.tokens._
import evoware._
import bsse._


class BsseSuite extends FunSuite {
	test("example") {
		val (robot, state00) = BsseRobot.createRobotMockup()
		val plate1 = new Plate(nRows = 8, nCols = 12)
		val plate2 = new Plate(nRows = 8, nCols = 12)
		val builder = new RobotStateBuilder(state00)
		val state0 = {
			val carrier = state00.mapPartToChildren(robot.partTop)(17)
			builder.movePartTo(plate1, carrier, 0)
			builder.movePartTo(plate2, carrier, 1)
	
			for (well <- plate1.wells) {
				builder.addLiquid0(well, robot.liquidWater, 1000)
			}
			builder.toImmutable
		}
		
		val tips = robot.tips.toArray

		val aspirate = T1_Aspirate(Array(
				new TipWellVolume(tips(0), plate1.wells(0), 288),
				new TipWellVolume(tips(1), plate1.wells(1), 288),
				new TipWellVolume(tips(2), plate1.wells(2), 288),
				new TipWellVolume(tips(3), plate1.wells(3), 288)
				)
			)
		val dispense = T1_Dispense(Array(
				new TipWellVolumeDispense(tips(0), plate2.wells(0), 30, DispenseKind.Free),
				new TipWellVolumeDispense(tips(1), plate2.wells(1), 30, DispenseKind.Free),
				new TipWellVolumeDispense(tips(2), plate2.wells(2), 30, DispenseKind.Free),
				new TipWellVolumeDispense(tips(3), plate2.wells(3), 30, DispenseKind.Free)
				)
			)

		var state1 = { aspirate.twvs.foreach(builder.aspirate); builder.toImmutable }
		var state2 = { dispense.twvs.foreach(builder.dispense); builder.toImmutable }
		val txs = List(
				new T1_TokenState(aspirate, state1),
				new T1_TokenState(dispense, state2)
				)

		val translator = new BsseTranslator(robot)
		val s = translator.translateToString(state0, txs)
		println(s)
	}
}
