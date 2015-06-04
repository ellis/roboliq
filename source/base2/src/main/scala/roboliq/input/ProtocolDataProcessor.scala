package roboliq.input

import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer

sealed trait ProcessorMessage

case class VariableMessage(
	name: String,
	message: String
) extends ProcessorMessage

object ProtocolDataProcessor {
	/*
	def process(protocolData: ProtocolData): ResultE[ProtocolData] = {

		//val variables = new HashMap[String, ProcessingVariable]
		
		//CONTINUE, maybe use RjsValue.toBasicValue, don't like working with maps, create case classes instead?
		
		for {
			nameToProcessingVariable <- ResultE.mapAll(protocolData.variables)({ case (name, o) =>
				o.value_? match {
					case None =>
						ResultE.unit(Some(name -> ProcessingVariable(
							name = name,
							`type` = o.type_?.getOrElse("string"),
							value_? = None,
							setter_? = None,
							validations = List(CommandValidation(
								message = s"you must set the value",
								param_? = Some(name)
							)),
							alternatives = Nil
						)))
					case _ =>
						ResultE.unit(None)
				}
			}).map(_.flatten.toMap)
		} yield ()
		
	case class ProcessingState(
		variables: Map[String, ProcessingVariable],
		tasks: Map[String, ProcessingTask],
		commands: Map[String, ProcessingCommand],
		plan: ProcessingPlan
	)
		
		???
	}
	*/

	/*
	 * For each variable:
	 * - check for known type
	 * - check that value conforms to type
	 * - if no value given, get list of alternatives
	 * - if no value and no alternatives, tell use that they must set the value
	 * - if no value and only one alternative, set that value automatically
	 */
	def processVariables(protocol: Protocol): ResultE[ProtocolData] = {
		val messages = new ArrayBuffer[ProcessorMessage]
		for {
			nameToProcessingVariable <- ResultE.mapAll(protocol.variables)({ case (name, o) =>
				val typ = o.`type`.getOrElse("String")
				for {
					// TODO: check for known type
					// TODO: check that value conforms to type
					// TODO: if no value given, get list of alternatives
					// TODO: if no value and no alternatives, tell use that they must set the value
					// TODO: if no value and only one alternative, set that value automatically
				}
				o.value match {
					case None =>
						ResultE.unit(Some(name -> ProcessingVariable(
							name = name,
							`type` = o.`type`.getOrElse("String"),
							value_? = None,
							setter_? = None,
							validations = List(CommandValidation(
								message = s"you must set the value",
								param_? = Some(name)
							)),
							alternatives = Nil
						)))
					case _ =>
						ResultE.unit(None)
				}
			}).map(_.flatten.toMap)
		} yield ()
		
		???
	}
	
	def processMaterials()...
}
