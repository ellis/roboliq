package roboliq.protocol

import scala.collection.mutable.ArrayBuffer

import roboliq.common._

	
object LiquidProperties extends Enumeration {
	val Water, Glycerol = Value
}

class Volume(n: Double) {
	def ul = n
	def ml = n * 1000
}

class LiquidAmount
class LiquidVolume(pl: Int)

//class FixedPlate(model: PlateModel, val location: String)
//class FixedCarrier(val location: String)

class PObject {
	private val m_properties = new ArrayBuffer[Property[_]]
	val id = new Property[String]
}

class Property[A] {
	var value: PropertyValue[A] = PropertyEmpty[A]()
	def :=(a: A) { value = PropertyObject(a) }
	def :=(ref: TempRef) { value = PropertyRefId[A](ref.id) }
	def :=(ref: TempName) { value = PropertyRefName[A](ref.name) }
}

class TempRef(val id: String)
class TempName(val name: String)

sealed abstract trait PropertyValue[A]
case class PropertyEmpty[A]() extends PropertyValue[A]
case class PropertyRefId[A](id: String) extends PropertyValue[A]
case class PropertyRefName[A](name: String) extends PropertyValue[A]
case class PropertyRefProperty[A](p: Property[A]) extends PropertyValue[A]
case class PropertyObject[A](val value: A) extends PropertyValue[A]

class Liquid extends PObject {
}

class Pcr extends PObject {
	type Product = PcrProduct
	val products = new Property[List[Product]]
	val volumes = new Property[List[LiquidVolume]]
	val mixSpec = new Property[PcrMixSpec]
}

class PcrProduct extends PObject {
	val forwardPrimer = new Property[Liquid]
	val backwardPrimer = new Property[Liquid]
	val template = new Property[Liquid]
}

class PcrMixSpec extends PObject {
	val bufferLiquid = new Property[Liquid]
	val bufferConc = new Property[LiquidAmount]
	val dntpLiquid = new Property[Liquid]
	val dntpConc = new Property[LiquidAmount]
	val templateLiquid = new Property[Liquid]
	val templateConc = new Property[LiquidAmount]
	val forwardPrimerLiquid = new Property[Liquid]
	val forwardPrimerConc = new Property[LiquidAmount]
	val backwardPrimerLiquid = new Property[Liquid]
	val backwardPrimerConc = new Property[LiquidAmount]
	val polymeraseLiquid = new Property[Liquid]
	val polymeraseConc = new Property[LiquidAmount]
}

class Tube extends PObject {
	val liquid = new Property[Liquid]
	val conc = new Property[LiquidAmount]
	val volume = new Property[LiquidVolume]
	val location = new Property[String]
}

class XXX {
	def refId(id: String): TempRef = new TempRef(id)
	def refName(name: String): TempName = new TempName(name)
	
	class IntToVolumeWrapper(n: Int) {
		def pl = new LiquidVolume(n)
		def ul = new LiquidVolume(n * 1000)
		def ml = new LiquidVolume(n * 1000000)
	}
	
	implicit def intToVolumeWrapper(n: Int): IntToVolumeWrapper = new IntToVolumeWrapper(n)

	val l = List(
		new Pcr {
			id := "pcr"
			products := List(
				new Product { template := refName("FRP572"); forwardPrimer := refName("T7"); backwardPrimer := refName("FRO1259/60") }
			)
			volumes := List(20 ul)
		},
		new Tube {
			liquid := refId("pcr.mixSpec.polymeraseLiquid")
			//concentration = ...
			location := refId("eppendorfs:A1")
		}
	)
}
