package temp

import scala.collection.mutable.ArrayBuffer

//import roboliq.common._

	
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

abstract class PCommand extends PObject {
	def getSources(nb: NameBase): List[PropertyValue[_]]
}

class Property[A] {
	var value: PropertyValue[A] = PropertyEmpty[A]()
	def :=(a: A) { value = PropertyObject(a) }
	def :=(ref: TempRef) { value = PropertyRefId[A](ref.id) }
	def :=(ref: TempName) { value = PropertyRefDb[A](ref.name) }
}

class TempRef(val id: String)
class TempName(val name: String)

class NameBase(val mapVars: Map[String, PObject], val mapDb: Map[String, PObject]) {
	def apply(name: String): Option[PObject] = {
		mapVars.get(name).orElse(mapDb.get(name))
	}
}

sealed abstract trait PropertyValue[A] {
	def flatten(nb: NameBase): PropertyValue[A]
	def getValueR(nb: NameBase): Option[A]
}
case class PropertyEmpty[A]() extends PropertyValue[A] {
	def flatten(nb: NameBase): PropertyValue[A] = this
	def getValueR(nb: NameBase): Option[A] = None
}
case class PropertyRefId[A](id: String) extends PropertyValue[A] {
	def flatten(nb: NameBase): PropertyValue[A] = this
	def getValueR(nb: NameBase): Option[A] = nb.mapVars.get(id).map(_.asInstanceOf[A])
}
case class PropertyRefDb[A](name: String) extends PropertyValue[A] {
	def flatten(nb: NameBase): PropertyValue[A] = this
	def getValueR(nb: NameBase): Option[A] = nb.mapDb.get(name).map(_.asInstanceOf[A])
}
case class PropertyRefProperty[A](p: Property[A]) extends PropertyValue[A] {
	def flatten(nb: NameBase): PropertyValue[A] = p.value
	def getValueR(nb: NameBase): Option[A] = flatten(nb).getValueR(nb)
}
case class PropertyObject[A](val value: A) extends PropertyValue[A] {
	def flatten(nb: NameBase): PropertyValue[A] = this
	def getValueR(nb: NameBase): Option[A] = Some(value)
}

class Substance extends PObject {
}

class Liquid extends PObject {
}

class Pcr extends PCommand {
	type Product = PcrProduct
	val products = new Property[List[Product]]
	val volumes = new Property[List[LiquidVolume]]
	val mixSpec = new Property[PcrMixSpec]
	
	def getSources(nb: NameBase): List[PropertyValue[_]] = {
		val l = for {
			lProduct <- products.value.getValueR(nb)
		} yield {
			lProduct.flatMap(prod => List(
					prod.template.value,
					prod.backwardPrimer.value,
					prod.forwardPrimer.value
					))
		}
		l.getOrElse(Nil)
	}
}

class PcrProduct extends PObject {
	val template = new Property[Liquid]
	val forwardPrimer = new Property[Liquid]
	val backwardPrimer = new Property[Liquid]
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

class Test1 {
	def refId(id: String): TempRef = new TempRef(id)
	def refDb(name: String): TempName = new TempName(name)
	
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
				new Product { template := refDb("FRP572"); forwardPrimer := refDb("T7"); backwardPrimer := refDb("FRO1259/60") }
			)
			volumes := List(20 ul)
		},
		new Tube {
			liquid := refId("pcr.mixSpec.polymeraseLiquid")
			//concentration = ...
			location := refId("eppendorfs(A1)")
		}
	)
}

class Process(items: List[PObject]) {
	def getSources(): List[PropertyValue[_]] = {
		val nb = new NameBase(Map(), Map())
		items.collect({case cmd: PCommand => cmd}).flatMap(_.getSources(nb))
	}
}

object T {
	def run {
		val test1 = new Test1
		val p = new Process(test1.l)
		p.getSources().foreach(println)
	}
}