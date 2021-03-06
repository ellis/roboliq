/*
import org.scalatest.FeatureSpec
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.ShouldMatchers

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.devices.pipette._

class PipetteHelperSpec extends FeatureSpec with GivenWhenThen with ShouldMatchers {
	val tips = SortedSet[Tip]((0 to 7).map(new Tip(_)) : _*)
	val (tipsA, tipsB) = tips.splitAt(4)
	val plate = new Plate(8, 12)
	val wellsAll = SortedSet[Well](plate.wells : _*)
	val helper = new PipetteHelper

	def testPairs(tips: SortedSet[Tip], wells: SortedSet[Well], ai: Seq[Int], tws: Seq[TipWell]): Seq[TipWell] = {
		val pairs = helper.chooseTipWellPairsNext(tips, wells, tws)
		val aiWells = pairs.map(_.well.index)
		aiWells should be === ai
		pairs
	}

	feature("Pairing 8 tips to 96 wells for pipetting") {

		var twvs: Seq[TipWell] = Nil
		info("These cycles are *with replacement* of previously paired wells")
		scenario("first cycle should match wells 0-7") {
			twvs = testPairs(tips, wellsAll, (0 to 7), twvs)
		}
		scenario("second cycle should match wells 8-15") {
			twvs = testPairs(tips, wellsAll, (8 to 15), twvs)
		}
		scenario("12th cycle should match wells 84-95") {
			for (iCycle <- (2 to 11)) {
				val iWell0 = iCycle * 8
				twvs = testPairs(tips, wellsAll, (iWell0 to iWell0 + 7), twvs)
			}
		}
		scenario("13th cycle should match wells 0-7") {
			twvs = testPairs(tips, wellsAll, (0 to 7), twvs)
		}
	}

	feature("Pairing 8 tips to 96 wells for pipetting without replacement") {
		var twvs: Seq[TipWell] = Nil
		var wells = wellsAll
		scenario("each cycle should match the next column of wells") {
			for (iCycle <- (0 to 11)) {
				wells.size should be === 96 - iCycle * 8
				val iWell0 = iCycle * 8
				twvs = testPairs(tips, wells, (iWell0 to iWell0 + 7), twvs)
				wells --= twvs.map(_.well)
			}
		}
		scenario("13th cycle should match no wells") {
			twvs = testPairs(tips, wells, Nil, twvs)
		}
	}

	feature("Pairing 4 tips to 96 wells for pipetting without replacement") {
		var twvs: Seq[TipWell] = Nil
		var wells = wellsAll
		scenario("First 12 cycles should match the next column of the top 4 wells") {
			for (iCycle <- (0 to 11)) {
				wells.size should be === 96 - iCycle * 4
				val iWell0 = iCycle * 8
				twvs = testPairs(tipsA, wells, (iWell0 to iWell0 + 3), twvs)
				wells --= twvs.map(_.well)
			}
		}
		scenario("Last 12 cycles should match the next column of the bottom 4 wells") {
			for (iCycle <- (0 to 11)) {
				wells.size should be === 48 - iCycle * 4
				val iWell0 = iCycle * 8 + 4
				twvs = testPairs(tipsA, wells, (iWell0 to iWell0 + 3), twvs)
				wells --= twvs.map(_.well)
			}
		}
		scenario("Any subsequent cycle should match no wells") {
			twvs = testPairs(tipsA, wells, Nil, twvs)
		}
	}
}
*/