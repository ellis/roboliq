package roboliq.input

import scala.collection.mutable.HashMap

/*
object ProtocolDataProcessor {
	def process(protocolData: ProtocolData): ResultE[ProtocolData] = {

		val variables = new HashMap[String, ProcessingVariable]
		
		//CONTINUE, maybe use RjsValue.toBasicValue, don't like working with maps, create case classes instead?
		
		for {
			_ <- ResultE.mapAll(protocolData.variables) { case (name, v) =>
				rjsval match {
					case map: RjsBasicMap =>
						if (map.get("value").isEmpty) {
							variables(name) = ProcessingVariable(
								name = name,
								`type` = map.get("type").getOrElse("string"),
								value_? = None,
								setter_? = None,
								validations = List(CommandValidations(
									message = s"you must set the value",
									param_? = Some(name)
								)),
								alternatives = Nil
							)
						}
				}
			}
		} yield ()
		
	case class ProcessingState(
		variables: Map[String, ProcessingVariable],
		tasks: Map[String, ProcessingTask],
		commands: Map[String, ProcessingCommand],
		plan: ProcessingPlan
	)
		
		???
	}
}
*/