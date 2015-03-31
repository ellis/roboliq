package roboliq.input

import roboliq.ai.strips
import roboliq.core.ResultC


@RjsJsonType("protocolData")
case class ProtocolData(
	val objects: RjsBasicMap = RjsBasicMap(), 
	val commands: Map[String, CommandInfo] = Map(), 
	val commandOrder: List[String] = Nil, 
	val planningDomainObjects: Map[String, String] = Map(), 
	val planningInitialState: strips.Literals = strips.Literals.empty
) {
	def merge(that: ProtocolData): ResultC[ProtocolData] = {
		for {
			objects <- this.objects merge that.objects
		} yield {
			new ProtocolData(
				objects = objects,
				commands = this.commands ++ that.commands,
				commandOrder = this.commandOrder ++ that.commandOrder,
				planningDomainObjects = this.planningDomainObjects ++ that.planningDomainObjects,
				planningInitialState = this.planningInitialState ++ that.planningInitialState
			)
		}
	}
}

case class CommandValidation(
	message: String,
	param_? : Option[String] = None,
	precond_? : Option[Int] = None
)

case class CommandInfo(
	command: RjsValue,
	successors: List[String] = Nil,
	validations: List[CommandValidation] = Nil,
	effects: strips.Literals = strips.Literals.empty
)

/*
sealed trait CommandValidation
case class CommandValidation_Param(name: String) extends CommandValidation
case class CommandValidation_Precond(description: String) extends CommandValidation

case class MyPlate(
	model_? : Option[String],
	location_? : Option[String]
)

case class ProtocolDataA(
	val objects: RjsBasicMap = RjsBasicMap(),
	val commands: RjsBasicMap = RjsBasicMap(),
	val commandOrderingConstraints: List[List[String]] = Nil,
	val commandOrder: List[String] = Nil,
	val planningDomainObjects: Map[String, String] = Map(),
	val planningInitialState: strips.Literals = strips.Literals.empty
) {
	def merge(that: ProtocolDataA): ResultC[ProtocolDataA] = {
		for {
			objects <- this.objects merge that.objects
			commands <- this.commands merge that.commands
		} yield {
			new ProtocolDataA(
				objects = objects,
				commands = commands,
				commandOrderingConstraints = this.commandOrderingConstraints ++ that.commandOrderingConstraints,
				commandOrder = this.commandOrder ++ that.commandOrder,
				planningDomainObjects = this.planningDomainObjects ++ that.planningDomainObjects,
				planningInitialState = this.planningInitialState ++ that.planningInitialState
			)
		}
	}
}
*/
