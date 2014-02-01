package roboliq.commands.pipette.scheduler

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.ShouldMatchers
import roboliq.core._

class TipModelSearcher1Spec extends FunSpec with ShouldMatchers with BeforeAndAfter {
	val searcher = new TipModelSearcher1[String, String, String]
	val itemAll_l = (0 until 8).toList.map(i => s"Item$i")
	val item1 = itemAll_l(0)
	val item2 = itemAll_l(1)
	val liquid1 = "Liquid1"
	val liquid2 = "Liquid2"
	val tipModel1 = "TipModel1"
	val tipModel2 = "TipModel2"
	val tipModel3 = "TipModel3"

	val tm1 = List(tipModel1)
	val tm2 = List(tipModel2)
	val tm3 = List(tipModel3)
	val tm12 = List(tipModel1, tipModel2)
	val tm13 = List(tipModel1, tipModel3)
	val tm21 = List(tipModel2, tipModel1)
	val tm23 = List(tipModel2, tipModel3)
	
	val x11 = (liquid1, tm1)
	val x12 = (liquid1, tm2)
	val x13 = (liquid1, tm3)
	val x21 = (liquid2, tm1)
	val x22 = (liquid2, tm2)
	val x112 = (liquid1, tm12)
	val x113 = (liquid1, tm13)
	val x121 = (liquid1, tm21)
	val x123 = (liquid1, tm23)
	
	/**
	 * x is the list of liquid and tip models per item
	 * tm_l is the list of expected tipModel assignments to the items  
	 */
	private def check(x: List[(String, List[String])], tm_l: List[String]) {
		val l = itemAll_l zip x
	  	val res = searcher.searchGraph(
			item_l = l.map(_._1),
			itemToLiquid_m = l.map(o => (o._1, o._2._1)).toMap,
			itemToModels_m = l.map(o => (o._1, o._2._2)).toMap
		)
		val map = itemAll_l.zip(tm_l).toMap
		res should equal (RqSuccess(map))
	}
	
	describe("[11]") {
		it("should return 1") {
			check(List(x11), List(tipModel1))
		}
	}
	describe("[112]") {
		it("should return 1") {
			check(List(x112), List(tipModel1))
		}
	}
	describe("[11, 11]") {
		it("should return 11") {
			check(List(x11, x11), List(tipModel1, tipModel1))
		}
	}
	describe("[11, 12]") {
		it("should return 12") {
			check(List(x11, x12), List(tipModel1, tipModel2))
		}
	}
	describe("[11, 21]") {
		it("should return 11") {
			check(List(x11, x21), List(tipModel1, tipModel1))
		}
	}
	describe("[11, 22]") {
		it("should return 12") {
			check(List(x11, x22), List(tipModel1, tipModel2))
		}
	}
	describe("[11, 112]") {
		it("should return 11") {
			check(List(x11, x112), List(tipModel1, tipModel1))
		}
	}
	describe("[11, 121]") {
		it("should return 11") {
			check(List(x11, x121), List(tipModel1, tipModel1))
		}
	}
	describe("[12, 112]") {
		it("should return 22") {
			check(List(x112, x12), List(tipModel2, tipModel2))
		}
	}
	describe("[11, 11, 11]") {
		it("should return 111") {
			check(List(x11, x11, x11), List(tipModel1, tipModel1, tipModel1))
		}
	}
	describe("[11, 11, 12]") {
		it("should return 112") {
			check(List(x11, x11, x12), List(tipModel1, tipModel1, tipModel2))
		}
	}
	describe("[11, 11, 121]") {
		it("should return 111") {
			check(List(x11, x11, x121), List(tipModel1, tipModel1, tipModel1))
		}
	}
	describe("[11, 121, 121]") {
		it("should return 111") {
			check(List(x11, x121, x121), List(tipModel1, tipModel1, tipModel1))
		}
	}
	describe("[112, 112, 112]") {
		it("should return 111") {
			check(List(x112, x112, x112), List(tipModel1, tipModel1, tipModel1))
		}
	}
	describe("[112, 112, 121]") {
		it("should return 111") {
			check(List(x112, x112, x121), List(tipModel1, tipModel1, tipModel1))
		}
	}
	describe("[112, 121, 121]") {
		it("should return 222") {
			check(List(x112, x121, x121), List(tipModel2, tipModel2, tipModel2))
		}
	}
	describe("[112, 112, 112, 12, 13, 113, 113, 113]") {
		it("should return 22223333") {
			check(
				List(x112, x112, x112, x12, x13, x113, x113, x113),
				List(tipModel2, tipModel2, tipModel2, tipModel2, tipModel3, tipModel3, tipModel3, tipModel3)
			)
		}
	}
}