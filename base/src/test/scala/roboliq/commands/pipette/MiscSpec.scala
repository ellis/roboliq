package roboliq.commands.pipette

import spray.json.JsonParser
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._
import roboliq.commands.pipette._
import roboliq.commands.CommandSpecBase
import roboliq.test.Config01

class MiscSpec extends CommandSpecBase {
	describe("TipWell.equidistant") {
		import Config01._
		val vsA = VesselState(vessel_P1_A01, VesselContent.fromVolume(water, LiquidVolume.ul(100)).getOrElse(null))
		val vsB = VesselState(vessel_P1_B01, VesselContent.fromVolume(water, LiquidVolume.ul(100)).getOrElse(null))
		val vsC = VesselState(vessel_P1_C01, VesselContent.fromVolume(water, LiquidVolume.ul(100)).getOrElse(null))
		val vsD = VesselState(vessel_P1_D01, VesselContent.fromVolume(water, LiquidVolume.ul(100)).getOrElse(null))
		
		val vssA = VesselSituatedState(vsA, VesselPosition(plateState_P1, 0))
		val vssB = VesselSituatedState(vsB, VesselPosition(plateState_P1, 1))
		val vssC = VesselSituatedState(vsC, VesselPosition(plateState_P1, 2))
		val vssD = VesselSituatedState(vsD, VesselPosition(plateState_P1, 3))
		
		val policy = PipettePolicy("POLICY", PipettePosition.WetContact)
		
		it ("1") {
			val twvp_l = List(
				TipWellVolumePolicy(tipState1, vssA, LiquidVolume.ul(50), policy)
			)
			assert(TipWell.equidistant(twvp_l) === true)
		}
		
		it ("2a") {
			val twvp_l = List(
				TipWellVolumePolicy(tipState1, vssA, LiquidVolume.ul(50), policy),
				TipWellVolumePolicy(tipState2, vssB, LiquidVolume.ul(50), policy)
			)
			assert(TipWell.equidistant(twvp_l) === true)
		}
	}
}