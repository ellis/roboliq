package roboliq.evoware.translator

import org.scalatest.FunSpec
import roboliq.utils.JsonUtils
import roboliq.evoware.config.EvowareTableSetupConfig
import roboliq.evoware.config.EvowareAgentConfig
import java.io.File
import roboliq.evoware.config.EvowareAgentConfigProcessor
import roboliq.evoware.parser.CarrierNameGridSiteIndex

class EvowareCompilerSpec extends FunSpec {
	import roboliq.input.ResultCWrapper._

	val input1 = JsonUtils.yamlToJson("""
    objects:
      plate1: { type: Plate, model: ourlab.model1, location: ourlab.mario.P1 }
      ourlab:
        type: Namespace
        mario:
          type: Namespace
          evoware: { type: EvowareRobot }
          arm1: { type: Transporter, evowareRoma: 0 }
          P1: { type: Site, evowareCarrier: "MP 2Pos H+P Shake", evowareGrid: 10, evowareSite: 2 }
          P2: { type: Site, evowareCarrier: "MP 2Pos H+P Shake", evowareGrid: 10, evowareSite: 4 }
        model1: { type: PlateModel, evowareName: Ellis Nunc F96 MicroWell }
    steps: [
      {command: instruction.transporter.movePlate, agent: ourlab.mario.evoware, equipment: ourlab.mario.arm1, program: Narrow, object: plate1, destination: ourlab.mario.P2}
#      {set: {plate1: {location: ourlab.mario.P2}}},
#      {command: instruction.transporter.movePlate, agent: ourlab.mario.evoware, equipment: ourlab.mario.arm1, program: Narrow, object: plate1, destination: ourlab.mario.P1},
#      {set: {plate1: {location: ourlab.mario.P1}}}
    ]
""").asJsObject

	val evowareTableSetupConfig = EvowareTableSetupConfig(
		tableFile = "../testdata/bsse-mario/Template.ewt",
		protocolDetails_? = None,
		evowareProtocolData_? = None
	)
	val evowareAgentConfig = EvowareAgentConfig(
		name = "mario",
		evowareDir = "../testdata/bsse-mario",
		protocolDetails_? = None,
		evowareProtocolData_? = None,
		tableSetups = Map("default"  -> evowareTableSetupConfig)
	)
	val compiler = new EvowareCompiler("ourlab.mario.evoware", false)

	val carrierName = "MP 2Pos H+P Shake"
	val token1_l = List(Token(
		"""Transfer_Rack("10","10",1,0,0,0,0,"","Ellis Nunc F96 MicroWell","Narrow","","","MP 2Pos H+P Shake","","MP 2Pos H+P Shake","3",(Not defined),"5");""",
		Map(CarrierNameGridSiteIndex(carrierName,10,2) -> ("ourlab.mario.P1", "Ellis Nunc F96 MicroWell"), CarrierNameGridSiteIndex(carrierName,10,4) -> ("ourlab.mario.P2", "Ellis Nunc F96 MicroWell"))
	))
	val script1_l = List(
		EvowareScript(
			1,
			Vector("""Transfer_Rack("10","10",1,0,0,0,0,"","Ellis Nunc F96 MicroWell","Narrow","","","MP 2Pos H+P Shake","","MP 2Pos H+P Shake","3",(Not defined),"5");"""),
			Map(CarrierNameGridSiteIndex(carrierName,10,2) -> ("ourlab.mario.P1", "Ellis Nunc F96 MicroWell"), CarrierNameGridSiteIndex(carrierName,10,4) -> ("ourlab.mario.P2", "Ellis Nunc F96 MicroWell"))
		)
	)
	
	// CONTINUE: add typo to input1 in model1.evowareName, and make sure a ResultC error is produced instead of an exception
	// CONTINUE: add typo to input1 in P1.evowareCarrier, and make sure a ResultC error is produced instead of an exception

	describe("EvowareCompiler") {
		it("input1: build tokens") {
			val token_l_? = for {
				// Compile the script
				token_l <- compiler.buildTokens(input1)
			} yield {
				//println("success")
				//token_l.foreach(println)
				token_l
			}
			assert(token_l_?.run().value === token1_l)
		}
		
		it("input1: build scripts") {
			// Test generation of EvowareScript objects from tokens
			val script_l = compiler.buildScripts(token1_l)
			
			assert(script_l === script1_l)
		}
		
		it("input1: generate script contents") {
			// Test generation of script content from EvowareScript objects
			val table_l: List[String] = List("mario.default")
			val searchPath_l: List[File] = Nil
			val content_l_? = for {
				// Load carrier file
				carrierData <- EvowareAgentConfigProcessor.loadCarrierData(evowareAgentConfig)
				tableSetupConfig <- EvowareAgentConfigProcessor.loadTableSetupConfig(evowareAgentConfig, table_l)
				tableData <- EvowareAgentConfigProcessor.loadTableData(carrierData, tableSetupConfig, searchPath_l)
				l <- compiler.generateScriptContents(tableData, "test", script1_l)
			} yield {
				l.foreach(println)
			}
			
			content_l_?.run()
		}
	}
}