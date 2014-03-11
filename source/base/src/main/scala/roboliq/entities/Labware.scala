package roboliq.entities

import roboliq.core._

trait Entity {
	/** Key in database */
	val key: String
	/** A more human-friendly name */
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

case class TransporterSpec(key: String, label: Option[String] = None, description: Option[String] = None) extends Entity {
	def typeNames = List("transporterSpec")
}

case class Pipetter(key: String, label: Option[String] = None, description: Option[String] = None) extends Device {
	def typeNames = List("pipetter")
}

case class Peeler(key: String, label: Option[String] = None, description: Option[String] = None) extends Device {
	def typeNames = List("peeler")
}

case class PeelerSpec(key: String, label: Option[String] = None, description: Option[String] = None) extends Entity {
	def typeNames = List("peelerSpec")
}

case class ReaderProgram(
	key: String,
	label: Option[String] = None,
	description: Option[String] = None,
	duration: Option[Int] = None,
	rpm: Option[Int] = None,
	amplitude: Option[Int] = None
) extends Entity {
	def typeNames = List("shakerSpec")
}

case class Sealer(key: String, label: Option[String] = None, description: Option[String] = None) extends Device {
	def typeNames = List("sealer")
}

case class SealerSpec(key: String, label: Option[String] = None, description: Option[String] = None) extends Entity {
	def typeNames = List("sealerSpec")
}

case class Shaker(key: String, label: Option[String] = None, description: Option[String] = None) extends Device {
	def typeNames = List("shaker")
}

case class ShakerSpec(
	key: String,
	label: Option[String] = None,
	description: Option[String] = None,
	duration: Option[Int] = None,
	rpm: Option[Int] = None,
	amplitude: Option[Int] = None
) extends Entity {
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
	
	def isValidRowCol(row: Int, col: Int): Boolean =
		(row < 0 || row >= rows || col < 0 || col >= cols)
	
	def rowColToIndex(row: Int, col: Int): RsResult[Int] = {
		if (isValidRowCol(row, col))
			RsSuccess(col * rows + row)
		else
			RsError("invalid row/col")
	}
}

case class Plate(key: String, label: Option[String] = None, description: Option[String] = None) extends Labware {
	def typeNames = List("labware", "plate")
}

case class TubeModel(
	key: String,
	label: Option[String] = None,
	description: Option[String] = None,
	wellVolume: LiquidVolume
) extends LabwareModel {
	def typeNames = List("model", "tubeModel")
}

case class Tube(key: String, label: Option[String] = None, description: Option[String] = None) extends Entity {
	def typeNames = List("labware", "tube")
}

case class Well(key: String, label: Option[String] = None, description: Option[String] = None) extends Entity {
	def typeNames = List("well")
}

case class Tip(
	key: String,
	label: Option[String] = None,
	description: Option[String] = None,
	val index: Int,
	val row: Int,
	val col: Int,
	val permanent_? : Option[TipModel]
) extends Entity with Ordered[Tip]{
	def typeNames = List("tip")
	override def compare(that: Tip): Int = (index - that.index)
}

case class TipModel(
	key: String,
	label: Option[String] = None,
	description: Option[String] = None,
	val volume: LiquidVolume, 
	val volumeMin: LiquidVolume,
	val cleanIntensityToExtraVolume: Map[CleanIntensity.Value, LiquidVolume] = Map()
) extends Entity {
	def typeNames = List("tipModel")
	val volumeWashExtra: LiquidVolume = cleanIntensityToExtraVolume.getOrElse(CleanIntensity.Thorough, LiquidVolume.empty)
	val volumeDeconExtra: LiquidVolume = cleanIntensityToExtraVolume.getOrElse(CleanIntensity.Decontaminate, LiquidVolume.empty)
}
