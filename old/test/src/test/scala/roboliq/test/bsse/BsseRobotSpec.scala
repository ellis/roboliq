package roboliq.test.bsse

import org.scalatest.FeatureSpec
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.matchers.MustMatchers

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.protocol._
import roboliq.devices.pipette._
import roboliq.robots.evoware._

import roboliq.labs.bsse._
import roboliq.labs.bsse.station1._


class BsseRobotSpec extends FeatureSpec with GivenWhenThen with ShouldMatchers with MustMatchers {
	import WellCoords._
	import WellPointerImplicits._
	
	val stationBsse = new StationConfig
	
	class LiquidDef(val name: String, val properties: LiquidProperties.Value, val cleanPolicy: GroupCleanPolicy, val contaminants: Set[Contaminant.Value] = Set())
	sealed abstract class LiquidSpec
	case class LiquidNone extends LiquidSpec
	case class LiquidTemplate(liquidDef: LiquidDef) extends LiquidSpec
	case class LiquidSpecific(liquidDef: LiquidDef) extends LiquidSpec
	
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
	
	/**
	 * @param liquidSrc_?
	 */
	def createProtocolBsse(
		addrSrc: WellAddressPartial,
		liquidSrc: LiquidSpec,
		addrDest: WellAddressPartial,
		liquidDest: LiquidSpec,
		nVolume: Double
	): ProtocolTest = {
		new Protocol(nVolume) with ProtocolSettings {
			def createReagent(liquidDef: LiquidDef, sSuffix: String = ""): Reagent = {
				val reagent = new Reagent
				val reagentSetup = kb.getReagentSetup(reagent)
				reagentSetup.sName_? = Some(liquidDef.name + sSuffix)
				reagentSetup.contaminants = liquidDef.contaminants
				reagentSetup.sFamily_? = Some(liquidDef.properties.toString)
				reagentSetup.group_? = Some(new LiquidGroup(liquidDef.cleanPolicy))
				reagent
			}
			def addReagent(liquidDef: LiquidDef, plate: Plate, addr: WellAddressPartial) {
				val reagent = createReagent(liquidDef)
				for (lWell <- addr.toWells(kb, plate)) {
					for (well <- lWell) {
						val wellSetup = kb.getWellSetup(well)
						wellSetup.reagent_? = Some(reagent)
					}
				}
			}
			def addReagentTemplate(liquidDef: LiquidDef, plate: Plate, addr: WellAddressPartial) {
				for (lWell <- addr.toWells(kb, plate)) {
					for (well <- lWell) {
						val wellSetup = kb.getWellSetup(well)
						val reagent = createReagent(liquidDef, "#"+wellSetup.index_?.get)
						wellSetup.reagent_? = Some(reagent)
					}
				}
			}
			def handleLiquidSpec(liquidSpec: LiquidSpec, plate: Plate, addr: WellAddressPartial) {
				liquidSpec match {
					case LiquidNone() =>
					case LiquidTemplate(liquidDef) => addReagentTemplate(liquidDef, plate, addr)
					case LiquidSpecific(liquidDef) => addReagent(liquidDef, plate, addr)
				}
			}
			wellsSrc.pointer_? = Some(plateSrc(addrSrc))
			wellsDest.pointer_? = Some(plateDest(addrDest))
			setPlate(plateSrc, stationBsse.Sites.cooled1, stationBsse.LabwareModels.platePcr)
			setPlate(plateDest, stationBsse.Sites.cooled2, stationBsse.LabwareModels.platePcr)
			handleLiquidSpec(liquidSrc, plateSrc, addrSrc)
			handleLiquidSpec(liquidDest, plateDest, addrDest)
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
	 * Basic pipetting procedures to test:
	 * - water to empty wells
	 *   - from 1 to [1, 4, 8, 1 row, full plate]
	 *   - from 8 to [1, 4, 8, 1 row, full plate] volume is [small enough to fill all dests, large enough to require refills]
	 * - for a single liquid
	 *   - from [1, 2, 4, 6, 8] source wells in a column to [1, 4, 8, 16] sequential destination wells
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
	 * - automatic tipModel selection based on volume
	 * - check for smart usage of multiple source wells for a single liquid based upon well volumes
	 */
	
	val water = new LiquidDef("water", LiquidProperties.Water, GroupCleanPolicy.ThoroughNone)
	val rowTop = WellAddressList(List.tabulate(12)(i => WellIndex(i * 8)))
	
	feature("pipette water") {
		// Transfer single well
		run1(A1, LiquidSpecific(water), A1, LiquidNone(), 30)
		run1(A1, LiquidSpecific(water), A1+4, LiquidNone(), 30)
		run1(A1, LiquidSpecific(water), A1+8, LiquidNone(), 30)
		run1(A1, LiquidSpecific(water), rowTop, LiquidNone(), 30)
		// Full plate transfer, each well holds unique liquid
		run1(A1+96, LiquidSpecific(water), A1+96, LiquidNone(), 30)
	}
	
	feature("pipette src to dest") {
		// Full plate transfer, each well holds unique liquid
		run1(A1+96, LiquidNone(), A1+96, LiquidNone(), 30)
		// Transfer single well
		run1(A1, LiquidNone(), A1, LiquidNone(), 30)
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
		println("kb:")
		println(protocol.kb.getPlateSetup(stationBsse.pipetter.plateDecon).dim_?)
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
	
	def run1(
		addrSrc: WellAddressPartial,
		liquidSrc: LiquidSpec,
		addrDest: WellAddressPartial,
		liquidDest: LiquidSpec,
		nVolume: Double
	) {
		val sScenario = List[Tuple2[String, String]](
			"s:" -> addrSrc.toString,
			"d:" -> addrDest.toString,
			"v:" -> nVolume.toString
		).map(pair => pair._1 + pair._2).mkString(", ")
		scenario(sScenario) {
			val protocol = createProtocolBsse(addrSrc, liquidSrc, addrDest, liquidDest, nVolume)
			run(stationBsse, protocol)
		}
	}
}
