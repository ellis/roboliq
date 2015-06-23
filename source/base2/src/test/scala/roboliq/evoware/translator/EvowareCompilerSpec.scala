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
      plate1: { type: Plate, model: ourlab.mario_model1, location: ourlab.mario_P1 }
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
			val result_? = for {
				// Load carrier file
				carrierData <- EvowareAgentConfigProcessor.loadCarrierData(evowareAgentConfig)
				tableSetupConfig <- EvowareAgentConfigProcessor.loadTableSetupConfig(evowareAgentConfig, table_l)
				tableData <- EvowareAgentConfigProcessor.loadTableData(carrierData, tableSetupConfig, searchPath_l)
				val compiler = new EvowareCompiler("ourlab.mario.evoware", false)
				token_l <- compiler.buildScript(input1)
			} yield {
				token_l.foreach(println)
				token_l
			}
			
			//val expected = Nothing

			assert(result_?.run().value == Token("nothing"))
		}
	}
}