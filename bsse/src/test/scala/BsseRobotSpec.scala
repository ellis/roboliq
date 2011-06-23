import org.scalatest.FeatureSpec
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.matchers.MustMatchers

import scala.collection.immutable.SortedSet

import roboliq.parts._
import roboliq.robot._
import roboliq.tokens._
import evoware._
import bsse._


class BsseRobotSpec extends FeatureSpec with GivenWhenThen with ShouldMatchers with MustMatchers {
	feature("batchesForDispense") {
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
			bCells = false,
			bDna = false,
			bOtherContaminant = false)
		for (well <- plate1.wells) {
			builder.addLiquid0(well, water, 1000)
		}

		robot.state = builder.toImmutable
		val tips = robot.tips.toArray

		scenario("4 large tips, 48ul") {
			val twvds = (0 to 3).map(i => new TipWellVolumeDispense(tips(i), plate2.wells(i), 48, DispenseKind.Free))
			val twvdss = robot.batchesForDispense(twvds)
			twvdss.size must be === 1
			twvdss.head.size should be === 4
		}

		scenario("4 large tips, 240ul") {
			val twvds = (0 to 3).map(i => new TipWellVolumeDispense(tips(i), plate2.wells(i), 240, DispenseKind.Free))
			val twvdss = robot.batchesForDispense(twvds)
			twvdss.size must be === 1
			twvdss.head.size should be === 4
		}
	}
	
	feature("ANother") {
		val carrier = new Carrier
		val plate1 = new Plate(nRows = 8, nCols = 12)
		val plate2 = new Plate(nRows = 8, nCols = 12)
		val liquidDirty1 = new Liquid("dirty1", true, true, false, false)
		val liquidDirty2 = new Liquid("dirty2", true, true, false, false)
		val liquidWater1 = new Liquid("water1", true, false, false, false)
		val liquidWater2 = new Liquid("water2", true, false, false, false)

		val evowareSetupState = new EvowareSetupState(
			grids = Map(
				carrier -> 17
			)
		)

		val robot = new BsseRobot(evowareSetupState)

		val builder = new RobotStateBuilder(robot.state)
		builder.sites += (plate1 -> new Site(carrier, 0))
		builder.sites += (plate2 -> new Site(carrier, 1))

		robot.state = builder.toImmutable
		val tips = robot.tips.toArray
		
		scenario("4 large tips, 240ul") {
			val twvds = (0 to 3).map(i => new TipWellVolumeDispense(tips(i), plate2.wells(i), 240, DispenseKind.Free))
			val twvdss = robot.batchesForDispense(twvds)
			twvdss.size must be === 1
			twvdss.head.size should be === 4
			println(twvdss.head)
		}
	}
}
