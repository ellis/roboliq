package roboliq.input


case class ProtocolVariable(
	name: Option[String] = None,
	`type`: Option[String] = None,
	description: Option[String] = None,
	value: Option[RjsBasicValue] = None,
	alternatives: List[RjsBasicValue] = Nil
)

case class ProtocolLabware(
	model: Option[String] = None,
	location: Option[String] = None
)

case class ProtocolSubstance()

case class ProtocolSourceSubstance(
	name: Option[String] = None,
	amount: Option[String] = None
)

case class ProtocolSource(
	well: String,
	substances: List[ProtocolSourceSubstance] = Nil
)

case class Protocol(
	variables: Map[String, ProtocolVariable],
	labwares: Map[String, ProtocolLabware],
	substances: Map[String, ProtocolSubstance],
	sources: Map[String, ProtocolSource],
	commands: List[RjsValue]
)