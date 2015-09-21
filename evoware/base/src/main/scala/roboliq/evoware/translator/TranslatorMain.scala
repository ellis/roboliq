/*package roboliq.evoware.translator

import roboliq.tokens.control.CommentToken
import ch.ethz.reactivesim.RsError
import ch.ethz.reactivesim.RsSuccess

object TranslatorMain extends App {
	for {
		carrier <- roboliq.evoware.parser.EvowareCarrierData.loadFile("./testdata/bsse-robot1/config/carrier.cfg")
		table <- roboliq.evoware.parser.EvowareTableData.loadFile(carrier, "./testdata/bsse-robot1/config/table-01.esc")
	} {
		val configData = EvowareConfigData(Map("G009S1" -> "pipette2hi"))
		val config = new EvowareConfig(carrier, table, configData)
		val translator = new EvowareTranslator(config)
		translator.translate(List(CommentToken("Hi"))) match {
			case RsError(e, w) => println(e); println(w)
			case RsSuccess(script, w) =>
				script.cmds.foreach(println)
		}
	}
}*/