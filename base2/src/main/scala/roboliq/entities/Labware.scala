package roboliq.entities

sealed trait Entity {
	val id: String
	def typeNames: List[String]
}

case class Agent(id: String) extends Entity {
	def typeNames = List("agent")
}

trait DeviceModel extends Entity {
	
}

trait Device extends Entity {
	
}

case class Transporter(id: String) extends Device {
	def typeNames = List("transporter")
}

case class Pipetter(id: String) extends Device {
	def typeNames = List("pipetter")
}

case class Sealer(id: String) extends Device {
	def typeNames = List("sealer")
}

case class Peeler(id: String) extends Device {
	def typeNames = List("peeler")
}

case class Shaker(id: String) extends Device {
	def typeNames = List("shaker")
}

case class ShakerSpec(id: String) extends Entity {
	def typeNames = List("shakerSpec")
}

case class Thermocycler(id: String) extends Device {
	def typeNames = List("thermocycler")
}

case class ThermocyclerSpec(id: String) extends Entity{
	def typeNames = List("thermocyclerSpec")
}

trait LabwareModel extends Entity

trait Labware extends Entity

case class SiteModel(id: String) extends LabwareModel {
	def typeNames = List("model", "siteModel")
}

case class Site(id: String) extends Labware {
	def typeNames = List("labware", "site")
}

case class Liquid(id: String) extends Entity {
	def typeNames = List("liquid")
}

case class PlateModel(
	id: String,
	rows: Int,
	cols: Int,
	wellVolume: LiquidVolume
) extends LabwareModel {
	def typeNames = List("model", "plateModel")
}

case class Plate(id: String) extends Labware {
	def typeNames = List("labware", "plate")
}

case class TubeModel(id: String) extends Entity {
	def typeNames = List("model", "tubeModel")
}

case class Tube(id: String) extends Entity {
	def typeNames = List("labware", "tube")
}

/////

/** Basically a tuple of a pipette policy name and the position of the tips while pipetting. */
case class PipettePolicy(id: String, pos: PipettePosition.Value) extends Entity {
	def typeNames = List("pipettePolicy")
	override def equals(that: Any): Boolean = {
		that match {
			case b: PipettePolicy => id == b.id
			case _ => false
		}
	}
}

object PipettePolicy {
	def fromName(name: String): PipettePolicy = {
		val pos = PipettePosition.getPositionFromPolicyNameHack(name)
		PipettePolicy(name, pos)
	}
}
