package roboliq.evoware.config

import org.scalatest.FunSpec
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsArray
import spray.json.JsString
import spray.json.JsValue
import java.io.File
import roboliq.utils.JsonUtils
import roboliq.input.ProtocolDataA
import roboliq.input.RjsMap
import roboliq.input.RjsString
import roboliq.ai.plan.Unique
import roboliq.ai.strips
import roboliq.input.RjsNumber

class EvowareProtocolDataGeneratorSpec extends FunSpec {
	import roboliq.input.ResultCWrapper._

	describe("EvowareProtocolDataGenerator") {
		it("") {
			val protocolData0 = new ProtocolDataA(
				objects = RjsMap(
					"plateModel_96_pcr" -> RjsMap(
						"type" -> RjsString("PlateModel"),
						"label" -> RjsString("96 well PCR plate"),
						"evowareName" -> RjsString("D-BSSE 96 Well PCR Plate")
					)
				),
				planningInitialState = strips.Literals(Unique[strips.Literal](strips.Literal.parse("site-closed CENTRIFUGE")))
			)
			val evowareProtocolData0 = EvowareProtocolData(
				sites = Map("CENTRIFUGE" -> EvowareSiteConfig(carrier_? = Some("Centrifuge"))),
				devices = Map("mario.centrifuge" -> EvowareDeviceConfig("Centrifuge", "Centrifuge", List("CENTRIFUGE", "CENTRIFUGE1"))),
				transporterBlacklist = Nil,
				userSites = Nil
			)
			val evowareProtocolData1 = EvowareProtocolData(
				sites = Map("P1" -> EvowareSiteConfig(grid_? = Some(9), site_? = Some(3))),
				devices = Map(),
				transporterBlacklist = Nil,
				userSites = List("P1")
			)
			val evowareTableSetupConfig = EvowareTableSetupConfig(
				tableFile = "../testdata/bsse-mario/Template.ewt",
				protocolData_? = None,
				evowareProtocolData_? = Some(evowareProtocolData1)
			)
			val evowareAgentConfig = EvowareAgentConfig(
				evowareDir = "../testdata/bsse-mario",
				protocolData_? = Some(protocolData0),
				evowareProtocolData_? = Some(evowareProtocolData0),
				tableSetups = Map("default"  -> evowareTableSetupConfig)
			)
			val data_? = EvowareProtocolDataGenerator.createProtocolData(
				agentIdent = "mario",
				agentConfig = evowareAgentConfig,
				table_l = List("mario.default"),
				searchPath_l = List()
			)
			
			val expected = ProtocolDataA(
				objects = RjsMap(
					"CENTRIFUGE" -> RjsMap(
						"evowareGrid" -> RjsNumber(54,None),
						"evowareSite" -> RjsNumber(2,None),
						"type" -> RjsString("Site")),
					"P1" -> RjsMap(
						"evowareGrid" -> RjsNumber(9,None),
						"evowareSite" -> RjsNumber(4,None),
						"type" -> RjsString("Site")),
					"mario.centrifuge" -> RjsMap(
						"evowareName" -> RjsString("Centrifuge"),
						"type" -> RjsString("Centrifuge")),
					"plateModel_96_pcr" -> RjsMap(
						"evowareName" -> RjsString("D-BSSE 96 Well PCR Plate"),
						"label" -> RjsString("96 well PCR plate"),
						"type" -> RjsString("PlateModel"))
				),
				planningDomainObjects = Map(
					"CENTRIFUGE" -> "Site",
					"P1" -> "Site",
					"mario.centrifuge" -> "Centrifuge"
				),
				planningInitialState = strips.Literals.fromStrings(
					"site-closed CENTRIFUGE"
				)
			)

			assert(data_?.run().value == expected)
		}
	}
}