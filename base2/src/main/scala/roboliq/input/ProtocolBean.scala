/*package roboliq.input

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import scala.collection.mutable.ListBuffer
import scala.beans.BeanProperty

/*
		"""{
		"wellContents": [
			{ "name": "plate1(A01 d D01)", "contents": "water@200ul" }
		],
		"protocol": [
			{ "command": "distribute", "source": "plate1(A01)", "destination": "plate2(A01)", "volume": "50ul" },
			{ "command": "distribute", "source": "plate1(A01)", "destination": "plate2(A01 d B01)", "volume": "50ul" },
			{ "command": "distribute", "source": "plate1(C01 d D01)", "destination": "plate2(C01 d D01)", "volume": "50ul" }
		]
		}""",
*/

class ProtocolBean {
	@BeanProperty var substances: java.util.ArrayList[SubstanceBean] = null
	@BeanProperty var plates: java.util.ArrayList[PlateBean] = null
	/*@BeanProperty var logic: java.util.ArrayList[String] = null
	@BeanProperty var specs: java.util.HashMap[String, String] = null
	@BeanProperty var deviceToSpec: java.util.ArrayList[java.util.ArrayList[String]] = null
	@BeanProperty var deviceToModelToSpec: java.util.ArrayList[java.util.ArrayList[String]] = null
	@BeanProperty var labware: java.util.ArrayList[String] = null
	@BeanProperty var tipModels: java.util.HashMap[String, TipModelBean] = null
	@BeanProperty var tips: java.util.ArrayList[TipBean] = null
	@BeanProperty var externalThermocyclers: java.util.ArrayList[String] = null
	*/
}

class SubstanceBean {
	@BeanProperty var name: String = null
	@BeanProperty var kind: String = null
	@BeanProperty var tipCleanPolicy: String = null
}

class PlateBean {
	@BeanProperty var name: String = null
	@BeanProperty var model: String = null
	@BeanProperty var location: String = null
}
*/