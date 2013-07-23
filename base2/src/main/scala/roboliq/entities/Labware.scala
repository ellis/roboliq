package roboliq.entities

sealed trait Entity {
	/** Key in database */
	val key: String
	/** Key in database */
	val label: Option[String]
	/** Description for the user */
	val description: Option[String]
	
	def typeNames: List[String]
}

case class Agent(key: String, label: Option[String] = None, description: Option[String] = None) extends Entity {
	def typeNames = List("agent")
}

trait DeviceModel extends Entity {
	
}

trait Device extends Entity {
	
}

case class Transporter(key: String, label: Option[String] = None, description: Option[String] = None) extends Device {
	def typeNames = List("transporter")
}

case class Pipetter(key: String, label: Option[String] = None, description: Option[String] = None) extends Device {
	def typeNames = List("pipetter")
}

case class Sealer(key: String, label: Option[String] = None, description: Option[String] = None) extends Device {
	def typeNames = List("sealer")
}

case class Peeler(key: String, label: Option[String] = None, description: Option[String] = None) extends Device {
	def typeNames = List("peeler")
}

case class Shaker(key: String, label: Option[String] = None, description: Option[String] = None) extends Device {
	def typeNames = List("shaker")
}

case class ShakerSpec(key: String, label: Option[String] = None, description: Option[String] = None) extends Entity {
	def typeNames = List("shakerSpec")
}

case class Thermocycler(key: String, label: Option[String] = None, description: Option[String] = None) extends Device {
	def typeNames = List("thermocycler")
}

case class ThermocyclerSpec(key: String, label: Option[String] = None, description: Option[String] = None) extends Entity {
	def typeNames = List("thermocyclerSpec")
}

trait LabwareModel extends Entity

trait Labware extends Entity

case class SiteModel(key: String, label: Option[String] = None, description: Option[String] = None) extends LabwareModel {
	def typeNames = List("model", "siteModel")
}

case class Site(key: String, label: Option[String] = None, description: Option[String] = None) extends Labware {
	def typeNames = List("labware", "site")
}

case class Liquid(key: String, label: Option[String] = None, description: Option[String] = None) extends Entity {
	def typeNames = List("liquid")
}

case class PlateModel(
	key: String,
	label: Option[String],
	description: Option[String],
	rows: Int,
	cols: Int,
	wellVolume: LiquidVolume
) extends LabwareModel {
	def typeNames = List("model", "plateModel")
}

case class Plate(key: String, label: Option[String] = None, description: Option[String] = None) extends Labware {
	def typeNames = List("labware", "plate")
}

case class TubeModel(key: String, label: Option[String] = None, description: Option[String] = None) extends Entity {
	def typeNames = List("model", "tubeModel")
}

case class Tube(key: String, label: Option[String] = None, description: Option[String] = None) extends Entity {
	def typeNames = List("labware", "tube")
}
