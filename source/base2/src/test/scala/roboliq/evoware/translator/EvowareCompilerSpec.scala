package roboliq.evoware.translator

import org.scalatest.FunSpec
import roboliq.utils.JsonUtils
import roboliq.evoware.config.EvowareTableSetupConfig
import roboliq.evoware.config.EvowareAgentConfig
import java.io.File
import roboliq.evoware.config.EvowareAgentConfigProcessor
import roboliq.evoware.parser.CarrierNameGridSiteIndex
import roboliq.utils.FileUtils
import roboliq.utils.FileUtils

class EvowareCompilerSpec extends FunSpec {
	import roboliq.input.ResultCWrapper._

	val input1 = JsonUtils.yamlToJson("""
    objects:
      plate1: { type: Plate, model: ourlab.model1, location: ourlab.mario.P2 }
      ourlab:
        type: Namespace
        mario:
          type: Namespace
          evoware: { type: EvowareRobot }
          arm1: { type: Transporter, evowareRoma: 0 }
          P2: { type: Site, evowareCarrier: "MP 2Pos H+P Shake", evowareGrid: 10, evowareSite: 2 }
          P3: { type: Site, evowareCarrier: "MP 2Pos H+P Shake", evowareGrid: 10, evowareSite: 4 }
        model1: { type: PlateModel, evowareName: Ellis Nunc F96 MicroWell }
    steps: [
      {command: instruction.transporter.movePlate, agent: ourlab.mario.evoware, equipment: ourlab.mario.arm1, program: Narrow, object: plate1, destination: ourlab.mario.P3}
#      {set: {plate1: {location: ourlab.mario.P3}}},
#      {command: instruction.transporter.movePlate, agent: ourlab.mario.evoware, equipment: ourlab.mario.arm1, program: Narrow, object: plate1, destination: ourlab.mario.P2},
#      {set: {plate1: {location: ourlab.mario.P2}}}
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
		Map(CarrierNameGridSiteIndex(carrierName,10,2) -> ("ourlab.mario.P2", "Ellis Nunc F96 MicroWell"), CarrierNameGridSiteIndex(carrierName,10,4) -> ("ourlab.mario.P3", "Ellis Nunc F96 MicroWell"))
	))
	val script1_l = List(
		EvowareScript(
			1,
			Vector("""Transfer_Rack("10","10",1,0,0,0,0,"","Ellis Nunc F96 MicroWell","Narrow","","","MP 2Pos H+P Shake","","MP 2Pos H+P Shake","3",(Not defined),"5");"""),
			Map(CarrierNameGridSiteIndex(carrierName,10,2) -> ("ourlab.mario.P2", "Ellis Nunc F96 MicroWell"), CarrierNameGridSiteIndex(carrierName,10,4) -> ("ourlab.mario.P3", "Ellis Nunc F96 MicroWell"))
		)
	)
	
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
			val result_? = for {
				// Load carrier file
				carrierData <- EvowareAgentConfigProcessor.loadCarrierData(evowareAgentConfig)
				tableSetupConfig <- EvowareAgentConfigProcessor.loadTableSetupConfig(evowareAgentConfig, table_l)
				tableData <- EvowareAgentConfigProcessor.loadTableData(carrierData, tableSetupConfig, searchPath_l)
				content_l <- compiler.generateScriptContents(tableData, "test", script1_l)
			} yield {
				// Save scripts
				val dir = new java.io.File("testoutput/spec_EvowareCompilerSpec")
				dir.mkdirs()
				for ((filename, bytes) <- content_l) {
					val file = new java.io.File(dir, filename)
					FileUtils.writeBytesToFile(file, bytes)
					assert(FileUtils.contentEquals(file, new java.io.File(dir, "accepted.esc")))
				}
				()
			}
			
			result_?.run().value
		}
	}
}