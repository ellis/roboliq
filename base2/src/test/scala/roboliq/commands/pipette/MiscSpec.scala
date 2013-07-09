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
		
		it ("should return true for single well") {
			val twvp_l = List(
				TipWellVolumePolicy(tipState1, vssA, LiquidVolume.ul(50), policy)
			)
			assert(TipWell.equidistant(twvp_l) === true)
		}
		
		it ("should return true for two adjacent wells") {
			val twvp_l = List(
				TipWellVolumePolicy(tipState1, vssA, LiquidVolume.ul(50), policy),
				TipWellVolumePolicy(tipState2, vssB, LiquidVolume.ul(50), policy)
			)
			assert(TipWell.equidistant(twvp_l) === true)
		}
		
		it ("2b") {
			val twvp_l = List(
				TipWellVolumePolicy(tipState1, vssC, LiquidVolume.ul(50), policy),
				TipWellVolumePolicy(tipState2, vssD, LiquidVolume.ul(50), policy)
			)
			assert(TipWell.equidistant(twvp_l) === true)
		}
	}
	
	describe("WellSpecParser") {
		it ("wellRow()") {
			val plate = Config01.plate_P1
			assert(WellSpecParser.wellRow(plate, 0) === 0)
			assert(WellSpecParser.wellRow(plate, 8) === 0)
			assert(WellSpecParser.wellRow(plate, 88) === 0)

			assert(WellSpecParser.wellRow(plate, 1) === 1)
			assert(WellSpecParser.wellRow(plate, 2) === 2)
		}
		
		it ("wellIndexName()") {
			assert(WellSpecParser.wellIndexName(8, 12, 0) === "A01")
			assert(WellSpecParser.wellIndexName(8, 12, 95) === "H12")
			assert(WellSpecParser.wellIndexName(8, 12, 1) === "B01")
			assert(WellSpecParser.wellIndexName(8, 12, 2) === "C01")
			assert(WellSpecParser.wellIndexName(8, 12, 10) === "C02")
		}
	}
}