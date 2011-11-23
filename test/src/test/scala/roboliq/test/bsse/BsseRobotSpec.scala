package roboliq.test.bsse

import org.scalatest.FeatureSpec
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.matchers.MustMatchers

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.protocol.CommonProtocol
import roboliq.devices.pipette._
import roboliq.robots.evoware._

import roboliq.labs.bsse._
import roboliq.labs.bsse.station1._


class BsseRobotSpec extends FeatureSpec with GivenWhenThen with ShouldMatchers with MustMatchers {
	import WellCoords._
	import WellPointerImplicits._
	
	val stationBsse = new StationConfig
	
	class Protocol(nVolume: Double) extends ProtocolTest {
		val plateSrc = new Plate
		val plateDest = new Plate
		val wellsSrc = new Wells
		val wellsDest = new Wells
		pipette(wellsSrc, wellsDest, nVolume)
		__findPlateLabels()
		kb.getPlateSetup(plateSrc).sLabel_? = Some("plateSrc")
		kb.getPlateSetup(plateDest).sLabel_? = Some("plateDest")
	}
	
	def createProtocolBsse(addrSrc: WellAddressPartial, addrDest: WellAddressPartial, nVolume: Double): ProtocolTest = {
		new Protocol(nVolume) with ProtocolSettings {
			wellsSrc.pointer_? = Some(plateSrc(addrSrc))
			wellsDest.pointer_? = Some(plateDest(addrDest))
			setPlate(plateSrc, stationBsse.Sites.cooled1, stationBsse.LabwareModels.platePcr)
			setPlate(plateDest, stationBsse.Sites.cooled2, stationBsse.LabwareModels.platePcr)
		}
	}
	
	/*
	 * Things to test:
	 * - plates: 4x5, 8x12
	 * - source liquids: one liquid, all different
	 * - source wells count: if all different then 1 well per liquid, otherwise [1-8] per liquid
	 * - dest wells: one well, one column, one row, 2.25*rows, full plate
	 * - dest wells count: 1-(2*rows-1), full plate
	 * - volume: 2, 5, 50, 200
	 * - pipetting policies: air, wet
	 * - cleaning policies: None, Thorough, Decontaminate
	 * - also: mixing of second liquid per dest
	 * - also: serial dilution   
	 */
	
	/*
	 * More basic things to test:
	 * - for a single liquid
	 *   - from [1, 2, 4, 6, 8] to [1, 4, 8, 16]
	 *   - from 3 to 8
	 * - from plate to plate, each liquid different
	 * - for 8 liquids: from each source, fill a destination row
	 */
	
	/*
	 * More basic stuff to test:
	 * - tip overrides
	 * - pipette policy overrides
	 * - premix
	 * - postmix
	 */
	
	feature("pipette src to dest") {
		run1(A1+96, A1+96, 30)
		run1(A1, A1, 30)
		/*for {
			nPlateRows <- List(4, 8)
			val nPlateCols = if (nPlateRows == 4) 5 else 12
			val nPlateWells = nPlateCols * nPlateRows
			bSingleLiquid <- List(true, false)
			nWellsPerLiquid <- if (bSingleLiquid) List(1,2,3,4,5,6,7,8) else List(1)
			iDestWellArrangement <- List(1,2,3,4)
			nVolume <- List(2,5,50,200)
		} {
			
		}*/
		/*scenario("P96A:A1+12 to P96B:A1+4,A2+2,A1+4,A2+2, 30 ul") {
			run1(A1+12, A1+4)+plateDest(A2+2)+plateDest(A1+4)+plateDest(A2+2), 30)
		}
		scenario("P1:A1+12 to P2:A1+4,A2+2,A1+4,A2+2, 2 ul") {
			run1(plateSrc(A1+12), plateDest(A1+4)+plateDest(A2+2)+plateDest(A1+4)+plateDest(A2+2), 30)
		}
		scenario("P1:A1+12 to P2:A1+4,A2+2,A1+4,A2+2, 100 ul") {
			run1(plateSrc(A1+12), plateDest(A1+4)+plateDest(A2+2)+plateDest(A1+4)+plateDest(A2+2), 30)
		}*/
	}

	def run(station: StationConfig, protocol: ProtocolTest) {
		protocol.__findLabels(protocol)
		val toolchain = new BsseToolchain(station)
		val res1 = toolchain.compileProtocol(protocol, true)
		res1 match {
			case Left(err) =>
				println(err.toString)
			case Right(succT: TranslatorStageSuccess) =>
				val script = succT.internal.asInstanceOf[EvowareScriptBuilder]
				val translator = new EvowareTranslator(toolchain.evowareConfig)
				val s = script.cmds.mkString("\n")
				s must be === ""
			case Right(succ) =>
				println(succ.toString)
		}
		res1.isRight must be === true
	}
	
	def run1(addrSrc: WellAddressPartial, addrDest: WellAddressPartial, nVolume: Double) {
		val sScenario = List[Tuple2[String, String]](
			"s" -> addrSrc.toString,
			"d" -> addrDest.toString,
			"v" -> nVolume.toString
		).map(pair => pair._1 + pair._2).mkString(", ")
		scenario(sScenario) {
			val protocol = createProtocolBsse(addrSrc, addrDest, nVolume)
			run(stationBsse, protocol)
		}
	}
}
