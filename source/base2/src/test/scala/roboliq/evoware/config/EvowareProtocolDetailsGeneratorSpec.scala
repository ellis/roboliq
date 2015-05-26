package roboliq.evoware.config

import org.scalatest.FunSpec
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsArray
import spray.json.JsString
import spray.json.JsValue
import java.io.File
import roboliq.utils.JsonUtils
import roboliq.input.ProtocolData
import roboliq.input.RjsMap
import roboliq.input.RjsString
import roboliq.ai.plan.Unique
import roboliq.ai.strips
import roboliq.input.RjsNumber
import roboliq.input.RjsBasicMap

class EvowareAgentConfigProcessorSpec extends FunSpec {
	import roboliq.input.ResultCWrapper._

	describe("EvowareAgentConfigProcessor") {
		it("") {
			val protocolData0 = new ProtocolData(
				objects = RjsBasicMap(
					"plateModel_384_square_transparent_greiner" -> RjsBasicMap(
						"type" -> RjsString("PlateModel"),
						"label" -> RjsString("384 square-flat-well transparent Greiner"),
						"evowareName" -> RjsString("384 Sqr Flat Trans Greiner")
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
				sites = Map("P3" -> EvowareSiteConfig(grid_? = Some(10), site_? = Some(4))),
				devices = Map(),
				transporterBlacklist = Nil,
				userSites = List("P3")
			)
			val evowareTableSetupConfig = EvowareTableSetupConfig(
				tableFile = "../testdata/bsse-mario/Template.ewt",
				protocolDetails_? = None,
				evowareProtocolData_? = Some(evowareProtocolData1)
			)
			val evowareAgentConfig = EvowareAgentConfig(
				name = "mario",
				evowareDir = "../testdata/bsse-mario",
				protocolDetails_? = Some(protocolData0),
				evowareProtocolData_? = Some(evowareProtocolData0),
				tableSetups = Map("default"  -> evowareTableSetupConfig)
			)
			val details_? = EvowareAgentConfigProcessor.createProtocolDetails(
				agentConfig = evowareAgentConfig,
				table_l = List("mario.default"),
				searchPath_l = List()
			)
			
			val expected = ProtocolData(
				objects = RjsBasicMap(
					"CENTRIFUGE" -> RjsBasicMap(
						"evowareCarrier" -> RjsNumber(65,None),
						"evowareGrid" -> RjsNumber(54,None),
						"evowareSite" -> RjsNumber(1,None),
						"type" -> RjsString("Site")
					),
					"P3" -> RjsBasicMap(
						"evowareCarrier" -> RjsNumber(316,None),
						"evowareGrid" -> RjsNumber(10,None),
						"evowareSite" -> RjsNumber(4,None),
						"type" -> RjsString("Site")
					),
					"mario.centrifuge" -> RjsBasicMap(
						"evowareName" -> RjsString("Centrifuge"),
						"type" -> RjsString("Centrifuge")
					),
					"plateModel_384_square_transparent_greiner" -> RjsBasicMap(
						"evowareName" -> RjsString("384 Sqr Flat Trans Greiner"),
						"label" -> RjsString("384 square-flat-well transparent Greiner"),
						"type" -> RjsString("PlateModel")
					)
				),
				planningDomainObjects = Map(
					"CENTRIFUGE" -> "Site",
					"mario.sm0" -> "SiteModel",
					"P3" -> "Site",
					"mario.centrifuge" -> "Centrifuge"
				),
				planningInitialState = strips.Literals.fromStrings(
					"site-closed CENTRIFUGE",
					"stackable mario.sm0 plateModel_384_square_transparent_greiner",
					"model CENTRIFUGE mario.sm0",
					"model P3 mario.sm0",
					"agent-has-device mario mario.centrifuge",
					"device-can-site mario.centrifuge CENTRIFUGE",
					"device-can-model mario.centrifuge plateModel_384_square_transparent_greiner"
				)
			)

			assert(details_?.run().value == expected)
		}
	}
}