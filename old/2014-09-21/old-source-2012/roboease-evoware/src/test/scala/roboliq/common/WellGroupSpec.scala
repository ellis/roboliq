package roboliq.common

import scala.collection.immutable.SortedSet

import org.scalatest.FeatureSpec
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.ShouldMatchers

import roboliq.common._


class WellGroupSpec extends FeatureSpec with GivenWhenThen with ShouldMatchers {
	val kb = new KnowledgeBase
	val plate1, plate2, plate3 = new Plate
	val pp1 = new PlateProxy(kb, plate1)
	val pp2 = new PlateProxy(kb, plate2)
	val pp3 = new PlateProxy(kb, plate3)
	pp1.label = "P1"
	pp1.location = "P1"
	pp1.setDimension(8, 12)
	pp2.label = "P2"
	pp2.location = "P2"
	pp2.setDimension(8, 12)
	pp3.label = "P3"
	pp3.location = "P3"
	pp3.setDimension(8, 12)
	kb.addPlate(plate1, false)
	kb.addPlate(plate2, false)
	kb.addPlate(plate3, false)
	
	val res = kb.concretize()
	if (res.isLeft) {
		kb.printErrors(res.left.get)
	}
	
	feature("WellGroup") {
		val mapper = res.right.get
		val states = mapper.createRobotState()
		val pc1 = plate1.state(states).conf
		val pc2 = plate2.state(states).conf
		val wells1 = pc1.wells.map(well => well.state(states).conf).filter(wc => wc.index >= 4*8 && wc.index < 4*8 + 3)
		val wells2 = pc2.wells.map(well => well.state(states).conf).filter(wc => wc.index >= 6*8+6 && wc.index < 6*8+6+9)
		wells2.foreach(well => println(well.iCol))
		val wells = wells1 ++ wells2
		
		scenario("test plate_?") {
			var g = WellGroup.empty
			info("Add wells from plate 1")
			for (well <- wells1) {
				g = g + well
				g.plate_? should be === Some(pc1)
			}
			info("Add a well from plate 2")
			g = g + wells2(0)
			g.plate_? should be === None
		}
		scenario("test iCol_?") {
			var g = WellGroup.empty
			
			info("group of P2:G7+2")
			g = WellGroup(wells2.take(2))
			g.plate_? should be === Some(pc2)
			g.iCol_? should be === Some(6)
			
			info("add P2:F1")
			g = g + wells2(2)
			println("set: "+g.set)
			g.plate_? should be === Some(pc2)
			g.iCol_? should be === None
		}
		scenario("splitByPlate()") {
			info("group of P1:E1+3;P2:G7+9")
			val g = WellGroup(wells)
			val gs = g.splitByPlate()
			info("splits into P1:E1+3 and P2:G7+9")
			gs.size should be === 2
			gs(0).set.toSeq should be === wells1
			gs(1).set.toSeq should be === wells2
		}
		scenario("splitByCol()") {
			info("group of P1:E1+3;P2:G7+9")
			val g = WellGroup(wells)
			val gs = g.splitByCol()
			val l1a = gs(1).set.toSeq
			val l1b = gs(2).set.toSeq
			info("splits into P1:E1+3, P2:G7+2, P2:H1+7")
			gs.size should be === 3
			gs(0).set.toSeq should be === wells1
			(l1a zip wells2) should be === (l1a zip l1a)
			(l1b.reverse zip wells2.reverse) should be === (l1b zip l1b).reverse
		}
	}
}
/*
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