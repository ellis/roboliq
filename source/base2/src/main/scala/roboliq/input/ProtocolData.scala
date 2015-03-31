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
