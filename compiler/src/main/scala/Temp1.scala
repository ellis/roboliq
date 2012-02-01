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

class LiquidAmount extends PObject {
	def properties: List[Property[_]] = Nil
}
class LiquidVolume(pl: Int) extends PObject {
	def properties: List[Property[_]] = Nil
}

//class FixedPlate(model: PlateModel, val location: String)
//class FixedCarrier(val location: String)

abstract class PObject {
	//private val m_properties = new ArrayBuffer[Property[_]]
	//val id = new Property[String]
	def properties: List[Property[_]]
	
	def getRefDbs(): List[PropertyRefDb[_]] = {
		val ll: List[List[PropertyRefDb[_]]] = for (p <- properties) yield {
			getRefDbs(p)
		}
		ll.flatten
	}
	
	def getRefDbs(p: Property[_]): List[PropertyRefDb[_]] = {
		p.values.flatMap(getRefDbs)
	}
	
	def getRefDbs(v: PropertyValue[_]): List[PropertyRefDb[_]] = {
		v match {
			case ref@PropertyRefDb(_) => List(ref)
			case obj: PropertyPObject[_] => obj.value.getRefDbs()
			case _ => Nil
		}
	}
}

abstract class PCommand extends PObject {
	//def getSources(nb: NameBase): List[PropertyValue[_]]
}

/*class IProperty[A <: PObject : ClassManifest, B <: PObject : ClassManifest] {
	var value: A = 
	def isEmpty: Boolean
	def getRefDb: Option[String]
}*/

/*
class Property[A <: PObject : ClassManifest] {
	private var m_bEmpty = true
	private var m_db: String = null
	private var m_var: String = null
	private var m_ptr: Property[A] = null
	private var m_obj: Option[A] = None
	
	private def clear() {
		m_db = null
		m_var = null
		m_ptr = null
		m_obj = None
	}
	private def updateEmpty() {
		m_bEmpty = (m_db == null && m_var == null && m_ptr == null && m_obj.isEmpty)
	}
	
	def :=(a: A) {
		clear()
		m_obj = Some(a)
		updateEmpty
	}
	
	def :=(ref: TempRef) { value = PropertyRefId[A](ref.id) }
	def :=(ref: TempName) { value = PropertyRefDb[A](ref.name) }
	def list: List[A] = List(value)
}
*/

//class Property[A <: PObject : ClassManifest, B <: PObject : ClassManifest] {
/*class Property[A <: PObject, B <: PObject](implicit ma: scala.reflect.Manifest[A], mb: scala.reflect.Manifest[B]) {
	var values: List[PropertyValue[A]] = Nil
	def :=(a: A) { value = PropertyPObject(a) }
	def :=(ref: TempRef) { value = PropertyRefId[A](ref.id) }
	def :=(ref: TempName) { value = PropertyRefDb[A](ref.name) }
}*/
class Property[A <: PObject](implicit m: Manifest[A]) {
	var values: List[PropertyValue[A]] = Nil
	def :=(a: A) { values = List(PropertyPObject(a)(m)) }
	def :=(ref: TempRef) { values = List(PropertyRefId[A](ref.id)) }
	def :=(ref: TempName) { values = List(PropertyRefDb[A](ref.name)) }
	def :=(la: List[A]) { values = la.map(a => PropertyPObject(a)) }
}

/*
class Property1[A <: PObject](implicit m: scala.reflect.Manifest[A]) extends Property[A, A]()(m, m) {
	def values: List[PropertyValue[A]] = List(value)
}

class PropertyList[A <: PObject](implicit m: scala.reflect.Manifest[A]) extends Property[List[PropertyValue[A]], A] {
	override def list: List[PropertyValue[A]] = value match {
		case obj: PropertyObject[_] => obj.asInstanceOf[List[PropertyValue[A]]]
		case _ => Nil
	}
	//var value: PropertyValue[List[PropertyValue[A]]] = PropertyEmpty[List[PropertyValue[A]]]()
	def ::=(a: A) { value = PropertyObject(List(PropertyObject(a))) }
	def ::=(la: List[A]) { value = PropertyObject(la.map(a => PropertyObject(a))) }
}
*/

class TempRef(val id: String)
class TempName(val name: String)

class NameBase(val mapVars: Map[String, PObject], val mapDb: Map[String, PObject]) {
	def apply(name: String): Option[PObject] = {
		mapVars.get(name).orElse(mapDb.get(name))
	}
}

sealed abstract class PropertyValue[A <: PObject](implicit m: Manifest[A]) {
	//def flatten(nb: NameBase): PropertyValue[A]
	//def getValueR(nb: NameBase): Option[A]
}
/*case class PropertyEmpty[A: ClassManifest]() extends PropertyValue[A] {
	def flatten(nb: NameBase): PropertyValue[A] = this
	def getValueR(nb: NameBase): Option[A] = None
}*/
case class PropertyRefId[A <: PObject](id: String)(implicit m: Manifest[A]) extends PropertyValue[A]()(m) {
	//def flatten(nb: NameBase): PropertyValue[A] = this
	//def getValueR(nb: NameBase): Option[A] = nb.mapVars.get(id).map(_.asInstanceOf[A])
}
case class PropertyRefDb[A <: PObject](name: String)(implicit m: Manifest[A]) extends PropertyValue[A]()(m) {
	//def flatten(nb: NameBase): PropertyValue[A] = this
	//def getValueR(nb: NameBase): Option[A] = nb.mapDb.get(name).map(_.asInstanceOf[A])
}
case class PropertyRefProperty[A <: PObject](p: Property[A])(implicit m: Manifest[A]) extends PropertyValue[A]()(m) {
	//def flatten(nb: NameBase): PropertyValue[A] = p.value
	//def getValueR(nb: NameBase): Option[A] = flatten(nb).getValueR(nb)
}
/*case class PropertyObject[A: ClassManifest](val value: A) extends PropertyValue[A] {
	def flatten(nb: NameBase): PropertyValue[A] = this
	def getValueR(nb: NameBase): Option[A] = Some(value)
}*/
case class PropertyPObject[A <: PObject](val value: A)(implicit m: Manifest[A]) extends PropertyValue[A]()(m) {
	//def flatten(nb: NameBase): PropertyValue[A] = this
	//def getValueR(nb: NameBase): Option[A] = Some(value)
	//def getRefDbs(): List[PropertyRefDb[_]] = value.getRefDbs()
}

/*class Substance extends PObject {
}*/

class Liquid extends PObject {
	def properties: List[Property[_]] = Nil
}

class Pcr extends PCommand {
	type Product = PcrProduct
	val products = new Property[Product]
	val volumes = new Property[LiquidVolume]
	val mixSpec = new Property[PcrMixSpec]
	
	def properties: List[Property[_]] = List(products, volumes, mixSpec)

	/*
	def getSources(nb: NameBase): List[PropertyValue[_]] = {
		products.values.flatMap(_.getValueR(nb) match {
			case None => Nil
			case Some(prod) => List(
				prod.template.value,
				prod.backwardPrimer.value,
				prod.forwardPrimer.value
			)
		})
	}
	*/
}

class PcrProduct extends PObject {
	val template = new Property[Liquid]
	val forwardPrimer = new Property[Liquid]
	val backwardPrimer = new Property[Liquid]
	def properties: List[Property[_]] = List(template, forwardPrimer, backwardPrimer)
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
	def properties: List[Property[_]] = Nil
}

class Tube extends PObject {
	val liquid = new Property[Liquid]
	val conc = new Property[LiquidAmount]
	val volume = new Property[LiquidVolume]
	//val location = new Property[String]
	def properties: List[Property[_]] = List(liquid, conc, volume)//, location)
}

class Test1 {
	def refId(id: String): TempRef = new TempRef(id)
	def refDb(name: String): TempName = new TempName(name)
	
	class IntToVolumeWrapper(n: Int) {
		def pl: LiquidVolume = new LiquidVolume(n)
		def ul: LiquidVolume = new LiquidVolume(n * 1000)
		def ml: LiquidVolume = new LiquidVolume(n * 1000000)
	}
	
	implicit def intToVolumeWrapper(n: Int): IntToVolumeWrapper = new IntToVolumeWrapper(n)

	val l = List(
		new Pcr {
			products := List(
				new Product { template := refDb("FRP572"); forwardPrimer := refDb("FRO115"); backwardPrimer := refDb("FRO1260") },
				new Product { template := refDb("FRP128"); forwardPrimer := refDb("FRO1259"); backwardPrimer := refDb("FRO1262") },
				new Product { template := refDb("FRP572"); forwardPrimer := refDb("FRO1261"); backwardPrimer := refDb("FRO114") }
			)
			volumes := intToVolumeWrapper(20).ul
		}
	)
}

class Process(items: List[PObject]) {
	def getDbRefs(): List[PropertyRefDb[_]] = items.flatMap(_.getRefDbs())
	/*def getSources(): List[PropertyValue[_]] = {
		val nb = new NameBase(Map(), Map())
		items.collect({case cmd: PCommand => cmd}).flatMap(_.getSources(nb))
	}*/
}

object T {
	def run {
		val test1 = new Test1
		val p = new Process(test1.l)
		test1.l.flatMap(_.getRefDbs()).foreach(println)
		//p.getSources().foreach(println)
	}
}