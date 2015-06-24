package roboliq.evoware.translator

import org.scalatest.FunSpec
import roboliq.utils.JsonUtils
import roboliq.evoware.config.EvowareTableSetupConfig
import roboliq.evoware.config.EvowareAgentConfig
import java.io.File
import roboliq.evoware.config.EvowareAgentConfigProcessor

class EvowareCompilerSpec extends FunSpec {
	import roboliq.input.ResultCWrapper._

	val input1 = JsonUtils.yamlToJson("""
    objects:
      plate1: { type: Plate, model: ourlab.mario_model1, location: ourlab.mario.P1 }
      ourlab:
        type: Namespace
        mario:
          type: Namespace
          evoware: { type: EvowareRobot }
          arm1: { type: Transporter, evowareRoma: 0 }
          P1: { type: Site, evowareCarrier: "some carrier", evowareGrid: 10, evowareSite: 2 }
          P2: { type: Site, evowareCarrier: "some carrier", evowareGrid: 10, evowareSite: 4 }
        model1: { type: PlateModel, evowareName: D-BSSE 96 Well Plate }
    steps: [
      {command: instruction.transporter.movePlate, agent: ourlab.mario.evoware, equipment: ourlab.mario.arm1, program: Narrow, object: plate1, destination: ourlab.mario.P2}
#      {set: {plate1: {location: ourlab.mario.P2}}},
#      {command: instruction.transporter.movePlate, agent: ourlab.mario.evoware, equipment: ourlab.mario.arm1, program: Narrow, object: plate1, destination: ourlab.mario.P1},
#      {set: {plate1: {location: ourlab.mario.P1}}}
    ]
""").asJsObject


	describe("EvowareCompiler") {
		it("test compiling") {
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
			val table_l: List[String] = List("mario.default")
			val searchPath_l: List[File] = Nil
			val compiler = new EvowareCompiler("ourlab.mario.evoware", false)
			val result_? = for {
				// Load carrier file
				//carrierData <- EvowareAgentConfigProcessor.loadCarrierData(evowareAgentConfig)
				//tableSetupConfig <- EvowareAgentConfigProcessor.loadTableSetupConfig(evowareAgentConfig, table_l)
				//tableData <- EvowareAgentConfigProcessor.loadTableData(carrierData, tableSetupConfig, searchPath_l)
				token_l <- compiler.buildScript(input1)
			} yield {
				//println("success")
				//token_l.foreach(println)
				token_l
			}
			
			//val test_? = roboliq.input.JsConverter.fromJs[String](input1.fields("objects").asJsObject, List("ourlab", "mario_P1", "evowareCarrier"))
			//assert(test_?.run().value == "")
			//val test2_? = compiler.lookupAs[String](input1.fields("objects").asJsObject, "ourlab.mario_P1", "evowareCarrier")
			//assert(test2_?.run().value == "")
			
			//val expected = Nothing

			//println("result: ")
			//println(result_?.run())
			val expected = List(Token(
						"""Transfer_Rack("10","10",1,0,0,0,0,"","ourlab.mario_model1","Narrow","","","some carrier","","some carrier","3",(Not defined),"5");""",
						Map((10,2) -> "ourlab.mario_model1", (10,4) -> "ourlab.mario_model1")
					))
			assert(result_?.run().value == expected)
		}
	}
}