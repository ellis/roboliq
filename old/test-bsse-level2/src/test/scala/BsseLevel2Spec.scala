import org.scalatest.FeatureSpec
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.ShouldMatchers

import scala.collection.immutable.SortedSet

import roboliq.parts._
import roboliq.robot._
import roboliq.tokens._
import roboliq.devices._
import roboliq.level2.tokens._
import roboliq.level2.commands._

import evoware._
import bsse._


class BsseLevel2Spec extends FeatureSpec with GivenWhenThen with ShouldMatchers {
	val (robot, state00) = BsseRobot.createRobotMockup()
	val plate1 = new Plate(nRows = 8, nCols = 12)
	val plate2 = new Plate(nRows = 8, nCols = 12)
	val state0 = {
		val builder = new RobotStateBuilder(state00)
		val carrier = state00.mapPartToChildren(robot.partTop)(17)
		builder.movePartTo(plate1, carrier, 0)
		builder.movePartTo(plate2, carrier, 1)

		for (well <- plate1.wells) {
			builder.addLiquid0(well, robot.liquidWater, 1000)
		}
		builder.toImmutable
	}
	
	val tips = robot.tips.toArray
	val translator = new BsseTranslator(robot)
	val device = new PipetteDevice
	val tr = new Translator
	tr.register(device)
	
	feature("Pipette well that are neighboring on both source and destination plates") {
		scenario("1x1 -> 1x1") {
			val nVolume = 25.0
			val list = List(
					(plate1.wells(0), plate2.wells(0), nVolume)
					)
			val tok2 = new T2_Pipette(list)
			val txs = tr.translate2(robot, state0, tok2)
			val toks = translator.translate(state0, txs)
			toks.isEmpty should be === false
			toks.foreach(println)
		}
	}
}
