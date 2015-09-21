package roboliq.planner

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.ShouldMatchers
import roboliq.core._
import scala.collection.immutable.BitSet
import roboliq.utils.FileUtils


class LiquidPlannerSpec extends FunSpec with ShouldMatchers with BeforeAndAfter {
	
	// The "components" we'll be mixing together
	val cA = new SubstanceLiquid("A", LiquidPhysicalProperties.Water, TipCleanPolicy.TL, None)
	val cB = new SubstanceLiquid("B", LiquidPhysicalProperties.Water, TipCleanPolicy.TT, None)
	val cC = new SubstanceDna("C", None, None)
	val cD = new SubstanceDna("D", None, None)
	val cE = new SubstanceDna("E", None, None)
	
	// Our source wells
	val s1 = new VesselContent(/*"S1",*/ Map(cA -> LiquidVolume.ul(1000)), Map())
	val s2 = new VesselContent(/*"S2",*/ Map(cA -> LiquidVolume.ul(950), cB -> LiquidVolume.ul(50)), Map())
	val s3 = new VesselContent(/*"S3",*/ Map(cA -> LiquidVolume.ul(1000)), Map(cC -> 1))
	val s4 = new VesselContent(/*"S4",*/ Map(cA -> LiquidVolume.ul(1000)), Map(cD -> 1))
	val s5 = new VesselContent(/*"S5",*/ Map(cA -> LiquidVolume.ul(1000)), Map(cE -> 1))
	val src_l = List(s1, s2, s3, s4, s5)
	
	// Dest wells
	val d1 = new VesselContent(/*"D1",*/
		Map(cA -> LiquidVolume.ul(99), cB -> LiquidVolume.ul(1)),
		Map(cC -> 0.1, cD -> 0.1)
	)
	val d2 = new VesselContent(/*"D2",*/
		Map(cA -> LiquidVolume.ul(99), cB -> LiquidVolume.ul(1)),
		Map(cC -> 0.1, cE -> 0.1)
	)
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