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
	def processVariables(protocolData: ProtocolData): ResultE[ProtocolData] = {
		val settings = new HashMap[String, ProtocolDataSetting]
		for ((name, o) <- protocolData.variables) {
			// TODO: check for known type
			// TODO: check that value conforms to type
			// TODO: if no value given, get list of alternatives
			// TODO: if no value and only one alternative, set that value automatically
			// TODO: if no value and no alternatives, tell use that they must set the value
			
			// Until the above TODOs are done, require that the value was set
			if (!o.value_?.isDefined) {
				val settingName = s"variables.$name.value"
				settings(settingName) = ProtocolDataSetting(None, List("Missing variable value"), Nil)
			}
		}
		ResultE.unit(protocolData.copy(settings = protocolData.settings ++ settings))
	}
	
	def processMaterials(
		protocolData: ProtocolData,
		plateModelName_l: Seq[String]
	): ResultE[ProtocolData] = {
		val settings = new HashMap[String, ProtocolDataSetting]
		for ((name, o) <- protocolData.materials) {
			o match {
				case x: PlateMaterial =>
					// Check whether the plate's model is specified
					if (!x.model_?.isDefined) {
						val settingName = s"materials.$name.model"
						settings(settingName) = ProtocolDataSetting(None, List("Missing plate model"), plateModelName_l.toList.map(RjsString))
					}
				case _ =>
			}
		}
		ResultE.unit(protocolData.copy(settings = protocolData.settings ++ settings))
	}
	
	def processTasks(protocolData: ProtocolData, taskToMethods_m: Map[String, List[String]]): ResultE[ProtocolData] = {
		val settings = new HashMap[String, ProtocolDataSetting]
		processTasks2(Vector(), taskToMethods_m, settings, protocolData.steps)
		ResultE.unit(protocolData.copy(settings = protocolData.settings ++ settings))
	}
	
	private def processTasks2(
		prefix_l: Vector[String],
		taskToMethods_m: Map[String, List[String]],
		settings: HashMap[String, ProtocolDataSetting],
		steps: Map[String, ProtocolDataStep]
	) {
		for ((name, step) <- steps) {
			// If this step contains a command:
			step.params.get("command") match {
				case None =>
				case Some(RjsString(commandName)) =>
					// If the command is a task:
					taskToMethods_m.get(commandName) match {
						case None =>
						case Some(method_l) =>
							// If the task's method hasn't been specified yet:
							step.params.get("method") match {
								case Some(_) =>
								case None =>
									val prefix = (prefix_l :+ name).mkString(".")
									val settingName = s"steps.$prefix.method"
									settings(settingName) = ProtocolDataSetting(None, Nil, method_l.map(RjsString))
							}
					}
				case Some(x) =>
					// TODO: error
			}
			if (!step.children.isEmpty)
				processTasks2(prefix_l :+ name, taskToMethods_m, settings, step.children)
		}
	}
	
	def processMethods(protocolData: ProtocolData, taskToMethods_m: Map[String, List[String]]): ResultE[ProtocolData] = {
		val settings = new HashMap[String, ProtocolDataSetting]
		processMethods2(Vector(), taskToMethods_m, settings, protocolData.steps)
		ResultE.unit(protocolData.copy(settings = protocolData.settings ++ settings))
	}

	private def processMethods2(
		prefix_l: Vector[String],
		taskToMethods_m: Map[String, List[String]],
		settings: HashMap[String, ProtocolDataSetting],
		steps: Map[String, ProtocolDataStep]
	) {
		for ((name, step) <- steps) {
			// If this step contains a command:
			step.params.get("command") match {
				case None =>
				case Some(RjsString(commandName)) =>
					// If the command is a task:
					taskToMethods_m.get(commandName) match {
						case None =>
						case Some(method_l) =>
							// If the task's method hasn't been specified yet:
							step.params.get("method") match {
								case Some(_) =>
								case None =>
									val prefix = (prefix_l :+ name).mkString(".")
									val settingName = s"steps.$prefix.method"
									settings(settingName) = ProtocolDataSetting(None, Nil, method_l.map(RjsString))
							}
					}
				case Some(x) =>
					// TODO: error
			}
			if (!step.children.isEmpty)
				processTasks2(prefix_l :+ name, taskToMethods_m, settings, step.children)
		}
	}
}
