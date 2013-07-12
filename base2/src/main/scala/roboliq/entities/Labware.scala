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

trait LabwareModel extends Entity

trait Labware extends Entity

case class SiteModel(id: String) extends LabwareModel {
	def typeName = "siteModel"
}

case class Site(id: String) extends Labware {
	def typeName = "site"
}

case class Liquid(id: String) extends Entity {
	def typeName = "liquid"
}

case class PlateModel(
	id: String,
	rows: Int,
	cols: Int,
	wellVolume: LiquidVolume
) extends LabwareModel {
	def typeName = "plateModel"
}

case class Plate(id: String) extends Labware {
	def typeName = "plate"
}

case class TubeModel(id: String) extends Entity {
	def typeName = "tubeModel"
}

case class Tube(id: String) extends Entity {
	def typeName = "tube"
}