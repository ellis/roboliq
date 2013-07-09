package roboliq.entities

sealed trait Entity {
	val id: String
	def typeName: String
}

case class Agent(id: String) extends Entity {
	def typeName = "agent"
}

trait DeviceModel extends Entity {
	
}

trait Device extends Entity {
	
}

case class Transporter(id: String) extends Device {
	def typeName = "transporter"
}

case class Pipetter(id: String) extends Device {
	def typeName = "pipetter"
}

case class Sealer(id: String) extends Device {
	def typeName = "sealer"
}

case class Shaker(id: String) extends Device {
	def typeName = "shaker"
}

case class ShakerSpec(id: String) extends Entity {
	def typeName = "shakerSpec"
}

case class Thermocycler(id: String) extends Device {
	def typeName = "thermocycler"
}

case class ThermocyclerSpec(id: String) extends Entity{
	def typeName = "thermocyclerSpec"
}

case class SiteModel(id: String) extends Entity {
	
}

case class Site(id: String) extends Entity {
	def typeName = "site"
}

case class LabwareModel(id: String) extends Entity {
	def typeName = "labwareModel"
}

case class Labware(id: String) extends Entity {
	def typeName = "labware"
}