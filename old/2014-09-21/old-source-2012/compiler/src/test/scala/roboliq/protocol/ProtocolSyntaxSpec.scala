package roboliq.protocol

import scala.collection.immutable.SortedSet

/*
import org.scalatest.FeatureSpec
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.ShouldMatchers

import roboliq.common
import roboliq.common._
import roboliq.protocol._


class ProtocolSyntaxSpec extends FeatureSpec with GivenWhenThen with ShouldMatchers {
	//import roboliq.common.Plate
	
	feature("PlateWells") {
		val protocol = new CommonProtocol {
			val p1 = new Plate(PlateFamily.Standard)
			val p2 = new Plate(PlateFamily.Standard)
			
			p1.label = "P1"
			p2.label = "P2"
			
			p1(G7).toString should be === "P1:G7"
			p1(G7+9).toString should be === "P1:G7+9"
			(p1(G7+9) + p2(A1+64)).toString should be === "P1:G7+9;P2:A1+64"
		}
	}
}*/