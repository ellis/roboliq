package roboliq.translator.jshop

import spray.json._
import roboliq.core._
import roboliq.input.Protocol
import roboliq.evoware.translator.EvowareConfigData
import roboliq.evoware.translator.EvowareConfig
import roboliq.evoware.translator.EvowareTranslator

object JshopMain extends App {
	val protocol = new Protocol
	
	for {
		carrierData <- roboliq.evoware.parser.EvowareCarrierData.loadFile("./testdata/bsse-robot1/config/carrier.cfg")
		tableData <- roboliq.evoware.parser.EvowareTableData.loadFile(carrierData, "./testdata/bsse-robot1/config/table-01.esc")
	} {
		protocol.loadConfig()
		protocol.loadEvoware(carrierData, tableData)
		protocol.loadJson("""
{
"plates": [
	{ "name": "plate1", "model": "Thermocycler Plate", "location": "offsite"}
],
"protocol": [
	{ "command": "log", "text": "do the right thing" },
	{ "command": "prompt", "text": "Please do the right thing, then press ENTER." }
]
}""".asJson.asJsObject
		)
		
		val protocolName = "pd"
		protocol.saveProblem(protocolName)
		
		val taskOutput = """(!log r1 text0002)"""
			
		val token_l = JshopTranslator.translate(protocol, taskOutput)
		token_l.foreach(println)

		val configData = EvowareConfigData(Map("G009S1" -> "pipette2hi"))
		val config = new EvowareConfig(carrierData, tableData, configData)
		val translator = new EvowareTranslator(config)
		translator.translate(token_l) match {
			case RsError(e, w) => println(e); println(w)
			case RsSuccess(script, w) =>
				translator.saveWithHeader(script, s"tasks/autogen/$protocolName.esc")
				script.cmds.foreach(println)
		}
	}
}