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
import spray.json.JsString
import roboliq.evoware.parser.EvowareCarrierData
import roboliq.core.ResultC

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
    steps:
      "1": {command: transporter.instruction.movePlate, agent: ourlab.mario.evoware, equipment: ourlab.mario.arm1, program: Narrow, object: plate1, destination: ourlab.mario.P3}
      "2": {command: transporter.instruction.movePlate, agent: ourlab.mario.evoware, equipment: ourlab.mario.arm1, program: Narrow, object: plate1, destination: ourlab.mario.P2}
    effects:
      "1": {"plate1.location": "ourlab.mario.P3"}
      "2": {"plate1.location": "ourlab.mario.P2"}
""").asJsObject

	val compiler = new EvowareCompiler("ourlab.mario.evoware", false)

	val carrierName = "MP 2Pos H+P Shake"
	val token1_l = List(
		Token(
			"""Transfer_Rack("10","10",1,0,0,0,0,"","Ellis Nunc F96 MicroWell","Narrow","","","MP 2Pos H+P Shake","","MP 2Pos H+P Shake","2",(Not defined),"4");""",
			JsonUtils.makeSimpleObject("plate1.location", JsString("ourlab.mario.P3")),
			Map(CarrierNameGridSiteIndex(carrierName,10,2) -> ("ourlab.mario.P2", "Ellis Nunc F96 MicroWell"), CarrierNameGridSiteIndex(carrierName,10,4) -> ("ourlab.mario.P3", "Ellis Nunc F96 MicroWell"))
		),
		Token(
			"",
			JsonUtils.makeSimpleObject("plate1.location", JsString("ourlab.mario.P3")),
			Map()
		),
		Token(
			"""Transfer_Rack("10","10",1,0,0,0,0,"","Ellis Nunc F96 MicroWell","Narrow","","","MP 2Pos H+P Shake","","MP 2Pos H+P Shake","4",(Not defined),"2");""",
			JsonUtils.makeSimpleObject("plate1.location", JsString("ourlab.mario.P2")),
			Map(CarrierNameGridSiteIndex(carrierName,10,2) -> ("ourlab.mario.P2", "Ellis Nunc F96 MicroWell"), CarrierNameGridSiteIndex(carrierName,10,4) -> ("ourlab.mario.P3", "Ellis Nunc F96 MicroWell"))
		),
		Token(
			"",
			JsonUtils.makeSimpleObject("plate1.location", JsString("ourlab.mario.P2")),
			Map()
		)
	)
	val script1_l = List(
		EvowareScript(
			1,
			Vector(
				"""Transfer_Rack("10","10",1,0,0,0,0,"","Ellis Nunc F96 MicroWell","Narrow","","","MP 2Pos H+P Shake","","MP 2Pos H+P Shake","2",(Not defined),"4");""",
				"""Transfer_Rack("10","10",1,0,0,0,0,"","Ellis Nunc F96 MicroWell","Narrow","","","MP 2Pos H+P Shake","","MP 2Pos H+P Shake","4",(Not defined),"2");"""
			),
			Map(CarrierNameGridSiteIndex(carrierName,10,2) -> ("ourlab.mario.P2", "Ellis Nunc F96 MicroWell"), CarrierNameGridSiteIndex(carrierName,10,4) -> ("ourlab.mario.P3", "Ellis Nunc F96 MicroWell"))
		)
	)

//List(EvowareScript(1,Vector(Transfer_Rack("10","10",1,0,0,0,0,"","Ellis Nunc F96 MicroWell","Narrow","","","MP 2Pos H+P Shake","","MP 2Pos H+P Shake","4",(Not defined),"2");, Transfer_Rack("10","10",1,0,0,0,0,"","Ellis Nunc F96 MicroWell","Narrow","","","MP 2Pos H+P Shake","","MP 2Pos H+P Shake","2",(Not defined),"4");),Map(CarrierNameGridSiteIndex(MP 2Pos H+P Shake,10,2) -> (ourlab.mario.P2,Ellis Nunc F96 MicroWell), CarrierNameGridSiteIndex(MP 2Pos H+P Shake,10,4) -> (ourlab.mario.P3,Ellis Nunc F96 MicroWell))))
//List(EvowareScript(1,Vector(Transfer_Rack("10","10",1,0,0,0,0,"","Ellis Nunc F96 MicroWell","Narrow","","","MP 2Pos H+P Shake","","MP 2Pos H+P Shake","2",(Not defined),"4");, Transfer_Rack("10","10",1,0,0,0,0,"","Ellis Nunc F96 MicroWell","Narrow","","","MP 2Pos H+P Shake","","MP 2Pos H+P Shake","4",(Not defined),"2");),Map(CarrierNameGridSiteIndex(MP 2Pos H+P Shake,10,2) -> (ourlab.mario.P2,Ellis Nunc F96 MicroWell), CarrierNameGridSiteIndex(MP 2Pos H+P Shake,10,4) -> (ourlab.mario.P3,Ellis Nunc F96 MicroWell))))	
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
				// Load carrier file
			val result_? = for {
				carrierData <- EvowareCarrierData.loadFile(new File("../testdata/bsse-mario", "Carrier.cfg").getPath)
				filename <- FileUtils.findFile("../testdata/bsse-mario/Template.ewt", searchPath_l)
				tableData <- roboliq.evoware.parser.EvowareTableData.loadFile(carrierData, filename.getPath)
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