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
	val (robot, state00) = BsseRobot.createRobotMockup()
	val plate1 = new Plate(nRows = 8, nCols = 12)
	val plate2 = new Plate(nRows = 8, nCols = 12)
	val state0 = {
		val builder = new RobotStateBuilder(state00)
		val carrier = builder.mapPartToChildren(robot.partTop)(17)
		builder.movePartTo(plate1, carrier, 0)
		builder.movePartTo(plate2, carrier, 1)

		for (well <- plate1.wells) {
			builder.addLiquid0(well, robot.liquidWater, 1000)
		}
		builder.toImmutable
	}
	
	val tips = robot.tips.toArray
	
	feature("batchesForDispense") {
		scenario("4 large tips, 48ul") {
			val twvds = (0 to 3).map(i => new TipWellVolumeDispense(tips(i), plate2.wells(i), 48, DispenseKind.Free))
			val twvdss = robot.batchesForDispense(state0, twvds)
			twvdss.size must be === 1
			twvdss.head.size should be === 4
		}

		scenario("4 large tips, 240ul") {
			val twvds = (0 to 3).map(i => new TipWellVolumeDispense(tips(i), plate2.wells(i), 240, DispenseKind.Free))
			val twvdss = robot.batchesForDispense(state0, twvds)
			twvdss.size must be === 1
			twvdss.head.size should be === 4
		}
	}
	
	feature("ANother") {
		scenario("4 large tips, 240ul") {
			val twvds = (0 to 3).map(i => new TipWellVolumeDispense(tips(i), plate2.wells(i), 240, DispenseKind.Free))
			val twvdss = robot.batchesForDispense(state0, twvds)
			twvdss.size must be === 1
			twvdss.head.size should be === 4
			println(twvdss.head)
		}
	}
}
