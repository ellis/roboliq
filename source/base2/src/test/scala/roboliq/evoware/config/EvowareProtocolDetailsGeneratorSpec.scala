package roboliq.evoware.config

import org.scalatest.FunSpec
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsArray
import spray.json.JsString
import spray.json.JsValue
import java.io.File
import roboliq.utils.JsonUtils
import roboliq.input.ProtocolDetails
import roboliq.input.RjsMap
import roboliq.input.RjsString
import roboliq.ai.plan.Unique
import roboliq.ai.strips
import roboliq.input.RjsNumber
import roboliq.input.RjsBasicMap

class EvowareProtocolDetailsGeneratorSpec extends FunSpec {
	import roboliq.input.ResultCWrapper._

	describe("EvowareProtocolDetailsGenerator") {
		it("") {
			val protocolData0 = new ProtocolDetails(
				objects = RjsBasicMap(
					"plateModel_384_square" -> RjsBasicMap(
						"type" -> RjsString("PlateModel"),
						"label" -> RjsString("384 square-well white plate"),
						"evowareName" -> RjsString("D-BSSE 384 Well Plate White")
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
			val details_? = EvowareProtocolDetailsGenerator.createProtocolDetails(
				agentConfig = evowareAgentConfig,
				table_l = List("mario.default"),
				searchPath_l = List()
			)
			
			val expected = ProtocolDetails(
				objects = RjsBasicMap(
					"CENTRIFUGE" -> RjsBasicMap(
						"evowareCarrier" -> RjsNumber(65,None),
						"evowareGrid" -> RjsNumber(54,None),
						"evowareSite" -> RjsNumber(2,None),
						"type" -> RjsString("Site")
					),
					"P1" -> RjsBasicMap(
						"evowareCarrier" -> RjsNumber(312,None),
						"evowareGrid" -> RjsNumber(9,None),
						"evowareSite" -> RjsNumber(4,None),
						"type" -> RjsString("Site")
					),
					"mario.centrifuge" -> RjsBasicMap(
						"evowareName" -> RjsString("Centrifuge"),
						"type" -> RjsString("Centrifuge")
					),
					"plateModel_384_square" -> RjsBasicMap(
						"evowareName" -> RjsString("D-BSSE 384 Well Plate White"),
						"label" -> RjsString("384 square-well white plate"),
						"type" -> RjsString("PlateModel")
					)
				),
				planningDomainObjects = Map(
					"CENTRIFUGE" -> "Site",
					"mario.sm0" -> "SiteModel",
					"P1" -> "Site",
					"mario.centrifuge" -> "Centrifuge"
				),
				planningInitialState = strips.Literals.fromStrings(
					"site-closed CENTRIFUGE",
					"stackable mario.sm0 plateModel_384_square",
					"model CENTRIFUGE mario.sm0",
					"agent-has-device mario mario.centrifuge",
					"device-can-site mario.centrifuge CENTRIFUGE",
					"device-can-model mario.centrifuge plateModel_384_square"
				)
			)
			// What we actually got back!
			// - [x] fix P1.evowareCarrier
			// - [ ] fix missing mario.sm0
			// - [ ] fix missing mario.sm0 logic
			ProtocolDetails(
				RjsBasicMap(
					"CENTRIFUGE" -> RjsBasicMap(
						"evowareCarrier" -> RjsNumber(65,None),
						"evowareGrid" -> RjsNumber(54,None),
						"evowareSite" -> RjsNumber(2,None),
						"type" -> RjsString("Site")
					),
					"P1" -> RjsBasicMap(
						"evowareCarrier" -> RjsNumber(312,None),
						"evowareGrid" -> RjsNumber(9,None),
						"evowareSite" -> RjsNumber(4,None),
						"type" -> RjsString("Site")
					),
					"mario.centrifuge" -> RjsBasicMap(
						"evowareName" -> RjsString("Centrifuge"),
						"type" -> RjsString("Centrifuge")
					),
					"plateModel_384_square" -> RjsBasicMap(
						"evowareName" -> RjsString("D-BSSE 384 Well Plate White"),
						"label" -> RjsString("384 square-well white plate"),
						"type" -> RjsString("PlateModel")
					)
				),
				Map(),
				List(),
				Map(
					"CENTRIFUGE" -> "Site",
					"P1" -> "Site",
					"mario.centrifuge" -> "Centrifuge"
				),
				strips.Literals.fromStrings(
					"site-closed CENTRIFUG",
					"agent-has-device mario mario.centrifuge",
					"device-can-site mario.centrifuge CENTRIFUGE"
				)
			)

			assert(details_?.run().value == expected)
		}
	}
}