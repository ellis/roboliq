package roboliq.input


/*sealed trait ProtocolMaterial {
	val `type`: Option[String]
	val label: Option[String]
	val description: Option[String]
}*/

case class ProtocolVariable(
	`type`: Option[String] = None,
	description: Option[String] = None,
	value: Option[RjsBasicValue] = None,
	alternatives: List[RjsBasicValue] = Nil
)

case class ProtocolSourceSubstance(
	name: Option[String] = None,
	amount: Option[RjsNumber] = None
)

case class ProtocolSource(
	well: String,
	substances: List[ProtocolSourceSubstance] = Nil
)

case class Protocol(
	variables: Map[String, ProtocolVariable],
	materials: Map[String, RjsBasicMap],
	sources: Map[String, ProtocolSource],
	steps: Map[String, RjsBasicMap]
)
