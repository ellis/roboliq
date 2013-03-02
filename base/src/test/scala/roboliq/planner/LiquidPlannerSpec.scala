package roboliq.planner

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.ShouldMatchers
import roboliq.core._
import scala.collection.immutable.BitSet
import roboliq.utils.FileUtils


class LiquidPlannerSpec extends FunSpec with ShouldMatchers with BeforeAndAfter {
	
	// The "components" we'll be mixing together
	val cA = Substance.liquid("A", 55, TipCleanPolicy.TL)
	val cB = Substance.liquid("B", 55, TipCleanPolicy.TT)
	val cC = Substance.dna("C")
	val cD = Substance.dna("D")
	val cE = Substance.dna("E")
	
	// Our source wells
	val s1 = VesselContent.byVolume(cA, LiquidVolume.ul(1000)).getOrElse(null)
	val s2 = VesselContent.byVolume(cA, LiquidVolume.ul(950)).flatMap(_.addLiquid(cB, LiquidVolume.ul(50))).getOrElse(null)
	val s3 = VesselContent.byVolume(cA, LiquidVolume.ul(1000)).map(_.addSubstance(cC, 1)).getOrElse(null)
	val s4 = VesselContent.byVolume(cA, LiquidVolume.ul(1000)).map(_.addSubstance(cD, 1)).getOrElse(null)
	val s5 = VesselContent.byVolume(cA, LiquidVolume.ul(1000)).map(_.addSubstance(cE, 1)).getOrElse(null)
	val src_l = List(s1, s2, s3, s4, s5)
	
	// Dest wells
	val d1 = VesselContent.byVolume(cA, LiquidVolume.ul(99)).flatMap(_.addLiquid(cB, LiquidVolume.ul(1))).
		map(_.addSubstance(cC, 0.1).addSubstance(cD, 0.1)).getOrElse(null)
	val d2 = VesselContent.byVolume(cA, LiquidVolume.ul(99)).flatMap(_.addLiquid(cB, LiquidVolume.ul(1))).
		map(_.addSubstance(cC, 0.1).addSubstance(cE, 0.1)).getOrElse(null)
	val dst_l = List(d1, d2)
	
	val planner = new LiquidPlanner

	describe("Planner") {
		val trace0 = planner.calcMixture(src_l, dst_l)
		val bitset_l = planner.calcBitset(trace0.mixture_l)
		it("should calcuate the correct mixtures for each destination well") {
			trace0.mixture_l should equal (List(List[Double](60, 20, 10, 10, 0), List[Double](60, 20, 10, 0, 10)))
		}
		it("should calcuate the correct source bitmap for each destination well") {
			bitset_l should equal (List(BitSet(0, 1, 2, 3), BitSet(0, 1, 2, 4)))
		}
		it("should step") {
			val trace = planner.run(src_l, dst_l)
			val doc = trace.createRst("Example A", "=-~")
			
			println(doc)
			FileUtils.writeToFile("trace-base/LiquidPlannerSpec_exampleA.rst", doc)
		}
	}
}