package temp

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

//import roboliq.common._

	
object LiquidProperties extends Enumeration {
	val Water, Glycerol = Value
}

class Volume(n: Double) {
	def ul = n
	def ml = n * 1000
}

class Pool(val sPurpose: String) {
	var liquid0_? : Option[Liquid] = None
	var volume0_? : Option[LiquidVolume] = None
	//var purpose: String = null
}

class Sample(val liquid: Liquid, val volume: LiquidVolume)

class LiquidAmount extends PObject {
	def properties: List[Property[_]] = Nil
}
/** Volume in picoliters */
class LiquidVolume(pl: Int) extends PObject {
	def properties: List[Property[_]] = Nil
}

//class FixedPlate(model: PlateModel, val location: String)
//class FixedCarrier(val location: String)

abstract class PObject {
	//private val m_properties = new ArrayBuffer[Property[_]]
	var key: String = null
	def properties: List[Property[_]]
	
	def refId(id: String): TempNameA = new TempNameA(id)
	def refDb(key: String): TempKeyA = new TempKeyA(key)
	def refKey[A](key: String): TempKey[A] = new TempKey[A](key)

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

	
	override def toString(): String = {
		val clazz = getClass()
		val clazzProperty = classOf[Property[_]]
		val map: Map[String, String] = (for (m <- clazz.getDeclaredMethods() if clazzProperty.isAssignableFrom(m.getReturnType())) yield {
			val sProperty = m.getName()
			val p = m.invoke(this).asInstanceOf[Property[_]]
			p.toContentString match {
				case None => None
				case Some(sValue) => Some(sProperty -> sValue)
			}
		}).flatten.toMap
		//properties.map(p => clazz.getDeclaredMethod())
		val pairs = ((if (key == null) Nil else ("key", key) :: Nil) ++ map.toList)
		this.getClass().getSimpleName() + " {" +
			pairs.map(pair => pair._1+": "+pair._2).mkString(", ") + " }"
	}
}

case class PString(val value: String) extends PObject {
	def properties: List[Property[_]] = Nil
	override def toString(): String = "\"" + value.toString() + "\""
	override def equals(o: Any): Boolean = o match {
		case that: PString => value == that.value
		case _ => false
	}
}

class PInteger(val value: Int) extends PObject {
	def properties: List[Property[_]] = Nil
	override def toString(): String = value.toString()
	override def equals(o: Any): Boolean = o match {
		case that: PInteger => value == that.value
		case _ => false
	}
}

abstract class PCommand extends PObject {
	//def getSources(nb: NameBase): List[PropertyValue[_]]
	def getNewPools(): List[Pool] = Nil
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
	def :=(ref: TempNameA) { values = List(PropertyRefId[A](ref.name)) }
	def :=(ref: TempKeyA) { values = List(PropertyRefDb[A](ref.key)) }
	def :=(la: List[A]) { values = la.map(a => PropertyPObject(a)) }
	def set(v: TempValue[A]) = v match {
		case TempNull() => values = Nil
		case Temp1(a) => :=(a)
		case TempKey(s) => :=(new TempKeyA(s))
		case TempName(s) => :=(new TempNameA(s))
		case TempList(la) => :=(la)
	}
	
	def toContentString(): Option[String] = {
		values match {
			case Nil => None
			case a :: Nil => a.toContentString
			case _ => Some(values.map(_.toContentString).flatten.mkString("[ ", ", ", " ]"))
		}
	}

	def getKeys: List[String] = values.map(_.getKey).flatten
	def getKey: Option[String] = getKeys match {
		case List(key) => Some(key)
		case _ => None
	}

	def getValues: List[A] = values.flatMap(_.getValues)
	def getValue: Option[A] = getValues match {
		case List(v) => Some(v)
		case _ => None
	} 
	
	def valueEquals(a: A): Boolean = (getValue == Some(a))
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

class TempKeyA(val key: String)
class TempNameA(val name: String)

sealed abstract class TempValue[A]
case class TempNull[A]() extends TempValue[A]
case class Temp1[A](val a: A) extends TempValue[A]
case class TempKey[A](val key: String) extends TempValue[A]
case class TempName[A](val name: String) extends TempValue[A]
case class TempList[A](val la: List[A]) extends TempValue[A]

class NameBase(val mapVars: Map[String, PObject], val mapDb: Map[String, PObject]) {
	def apply(name: String): Option[PObject] = {
		mapVars.get(name).orElse(mapDb.get(name))
	}
}

sealed abstract class PropertyValue[A <: PObject](implicit m: Manifest[A]) {
	def getKey: Option[String] = None
	def getValues: List[A] = Nil
	def keyEquals(sKey: String): Boolean = false
	//def valueEquals(b: Any): Boolean = false
	//def flatten(nb: NameBase): PropertyValue[A]
	//def getValueR(nb: NameBase): Option[A]
	def toContentString(): Option[String] = None
}
/*case class PropertyEmpty[A: ClassManifest]() extends PropertyValue[A] {
	def flatten(nb: NameBase): PropertyValue[A] = this
	def getValueR(nb: NameBase): Option[A] = None
}*/
case class PropertyRefId[A <: PObject](id: String)(implicit m: Manifest[A]) extends PropertyValue[A]()(m) {
	//def flatten(nb: NameBase): PropertyValue[A] = this
	//def getValueR(nb: NameBase): Option[A] = nb.mapVars.get(id).map(_.asInstanceOf[A])
}
case class PropertyRefDb[A <: PObject](key: String)(implicit m: Manifest[A]) extends PropertyValue[A]()(m) {
	override def getKey: Option[String] = Some(key)
	override def keyEquals(sKey: String): Boolean = (key == sKey)
	override def toContentString(): Option[String] = Some("K\""+key+"\"")
	def getKeyPair: Tuple2[String, String] = (m.erasure.getCanonicalName() -> key)
	//def flatten(nb: NameBase): PropertyValue[A] = this
	//def getValueR(nb: NameBase): Option[A] = nb.mapDb.get(name).map(_.asInstanceOf[A])
}
case class PropertyRefProperty[A <: PObject](p: Property[A])(implicit m: Manifest[A]) extends PropertyValue[A]()(m) {
	override def getKey: Option[String] = p.getKey
	override def getValues: List[A] = p.getValues
	//override def valueEquals(b: Any): Boolean = p.valueEquals(b)
	//def flatten(nb: NameBase): PropertyValue[A] = p.value
	//def getValueR(nb: NameBase): Option[A] = flatten(nb).getValueR(nb)
}
/*case class PropertyObject[A: ClassManifest](val value: A) extends PropertyValue[A] {
	def flatten(nb: NameBase): PropertyValue[A] = this
	def getValueR(nb: NameBase): Option[A] = Some(value)
}*/
case class PropertyPObject[A <: PObject](val value: A)(implicit m: Manifest[A]) extends PropertyValue[A]()(m) {
	override def getKey: Option[String] = Some(value.key)
	override def getValues: List[A] = List(value)
	override def keyEquals(sKey: String): Boolean = (sKey != null && sKey == value.key)
	override def toContentString(): Option[String] = Some(value.toString)
	//override def valueEquals(b: Any): Boolean = value.equals(b)
	//def flatten(nb: NameBase): PropertyValue[A] = this
	//def getValueR(nb: NameBase): Option[A] = Some(value)
	//def getRefDbs(): List[PropertyRefDb[_]] = value.getRefDbs()
}

/*class Substance extends PObject {
}*/

class Liquid extends PObject {
	var physical: String = null
	var cleanPolicy: String = null
	var contaminants: List[String] = null
	def properties: List[Property[_]] = Nil
	var components: List[Sample] = Nil
}

/*class Mixture extends Liquid {
}*/

class Plate extends PObject {
	val model = new Property[PString]
	val label = new Property[PString]
	val description = new Property[PString]
	val purpose = new Property[PString]
	def properties: List[Property[_]] = List(model, label)
}

class Well extends PObject {
	val parent = new Property[Plate]
	val index = new Property[PInteger]
	val liquid = new Property[Liquid]
	val volume = new Property[LiquidVolume]
	def properties: List[Property[_]] = List(parent, index, liquid, volume)
}

object Well {
	def apply(
		parent: TempValue[Plate] = TempNull[Plate],
		index: TempValue[PInteger] = TempNull[PInteger],
		liquid: TempValue[Liquid] = TempNull[Liquid],
		volume: TempValue[LiquidVolume] = TempNull[LiquidVolume]
	): Well = {
		val o = new Well
		o.parent.set(parent)
		o.index.set(index)
		o.liquid.set(liquid)
		o.volume.set(volume)
		o
	}
}

class Pcr extends PCommand {
	type Product = PcrProduct
	val products = new Property[Product]
	val volumes = new Property[LiquidVolume]
	val mixSpec = new Property[PcrMixSpec]
	
	def properties: List[Property[_]] = List(products, volumes, mixSpec)

	override def getNewPools(): List[Pool] = {
		List.fill(products.values.length)(new Pool("PCR"))
	}
	
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
	val waterLiquid = new Property[Liquid]
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
	def properties: List[Property[_]] = List(
		waterLiquid,
		bufferLiquid, bufferConc,
		dntpLiquid, dntpConc
	)
}

class Tube extends PObject {
	val liquid = new Property[Liquid]
	val conc = new Property[LiquidAmount]
	val volume = new Property[LiquidVolume]
	//val location = new Property[String]
	def properties: List[Property[_]] = List(liquid, conc, volume)//, location)
}

class Test1 {
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
			mixSpec := new PcrMixSpec {
				waterLiquid := refDb("water")
			}
		}
	)
}

class Process(items: List[PObject]) {
	def getRefDbs(): List[PropertyRefDb[_]] = items.flatMap(_.getRefDbs())
	def getClassKey(): List[Tuple2[String, String]] = getRefDbs().map(_.getKeyPair).distinct
	def getCommands(): List[PCommand] = items.collect({case cmd: PCommand => cmd})
	def getNewPools(): List[Pool] = getCommands().flatMap(_.getNewPools)
	/*def getSources(): List[PropertyValue[_]] = {
		val nb = new NameBase(Map(), Map())
		items.collect({case cmd: PCommand => cmd}).flatMap(_.getSources(nb))
	}*/
}

object Parsers {
	import scala.util.parsing.combinator._
	class Parser extends JavaTokenParsers {
		def propertyAndValue = ident ~ "=" ~ stringLiteral ^^ { case s ~ _ ~ v => (s, v) }
		def liquidContents = repsep(propertyAndValue, ",")
	}
}

object T {
	val lLiquid = List[Liquid](
		new Liquid { key = "water"; },
		new Liquid { key = "FRO114"; },
		new Liquid { key = "FRO115"; },
		new Liquid { key = "FRO1259"; },
		new Liquid { key = "FRO1260"; },
		new Liquid { key = "FRO1261"; },
		new Liquid { key = "FRO1262"; },
		new Liquid { key = "FRP128"; },
		new Liquid { key = "FRP572"; }
	)
	val lPlate = List[Plate](
		new Plate { key = "T50_water"; model := new PString("Tube 50ml"); description := new PString("water") },
		new Plate { key = "P1"; model := new PString("D-BSSE 96 Well PCR Plate"); description := new PString("templates and primers") },
		new Plate { key = "T1"; model := new PString("eppendorf"); description := new PString("polymerase") },
		new Plate { key = "P2"; model := new PString("D-BSSE 96 Well PCR Plate"); description := new PString("PCR products"); purpose := new PString("PCR") }
	)
	val lWell = List[Well](
		Well(parent = TempKey("T50_water"), liquid = TempKey("water")),
		Well(parent = TempKey("P1"), index = Temp1(new PInteger(0)), liquid = TempKey("FRO114")),
		Well(parent = TempKey("P1"), index = Temp1(new PInteger(1)), liquid = TempKey("FRO115")),
		Well(parent = TempKey("P1"), index = Temp1(new PInteger(2)), liquid = TempKey("FRO1259")),
		Well(parent = TempKey("P1"), index = Temp1(new PInteger(3)), liquid = TempKey("FRO1260")),
		Well(parent = TempKey("P1"), index = Temp1(new PInteger(4)), liquid = TempKey("FRO1261")),
		Well(parent = TempKey("P1"), index = Temp1(new PInteger(5)), liquid = TempKey("FRO1262")),
		Well(parent = TempKey("P1"), index = Temp1(new PInteger(6)), liquid = TempKey("FRP128")),
		Well(parent = TempKey("P1"), index = Temp1(new PInteger(7)), liquid = TempKey("FRP572"))
	)
	val mapTables = Map[String, Map[String, PObject]](
		"Liquid" -> lLiquid.map(liquid => liquid.key -> liquid).toMap,
		"Plate" -> lPlate.map(o => o.key -> o).toMap,
		"Well" -> lWell.map(o => o.key -> o).toMap
	)
	val mapClassToTable = Map[String, String](
		classOf[Liquid].getCanonicalName() -> "Liquid"
	)
	
	def lookup(pair: Tuple2[String, String]): Option[PObject] = {
		for {
			sTable <- mapClassToTable.get(pair._1)
			table <- mapTables.get(sTable)
			obj <- table.get(pair._2)
		} yield obj
	}
	
	def findWells(lsLiquidKey: List[String]): Map[String, List[Well]] = {
		lsLiquidKey.map(sLiquidKey => {
			sLiquidKey -> lWell.filter(_.liquid.values.exists(_.keyEquals(sLiquidKey)))
		}).toMap
	}
	
	def findPlates(mapLiquidKeyToWells: Map[String, List[Well]]): List[String] = {
		val lLiquidKeyToWells = mapLiquidKeyToWells.toList
		val lLiquidKeyToPlates: List[Tuple2[String, List[String]]]
			= lLiquidKeyToWells.map(pair => pair._1 -> pair._2.map(_.parent.getKey).flatten)
		// Keys of the plates which are absolutely required (i.e. liquid is not available on a different plate too)
		val lRequired: Set[String] = lLiquidKeyToPlates.collect({case (_, List(sPlateKey)) => sPlateKey}).toSet
		// List of the items whose plates are not in lRequired
		val lRemaining = lLiquidKeyToPlates.map({case (sLiquidKey, lPlate) => {
			val lPlate2 = lPlate.filter(sPlateKey => !lRequired.contains(sPlateKey))
			lPlate2 match {
				case Nil => None
				case _ => Some(sLiquidKey -> lPlate2)
			}
		}}).flatten
		val lChosen = lRemaining.map(_._2.head)
		(lRequired ++ lChosen).toList
	}
	
	def queryPlateByPurpose(sPurpose: String): List[Plate] = {
		val s = new PString(sPurpose)
		//lPlate.foreach(plate => println(plate, plate.purpose.getValue, s, plate.purpose.getValue == Some(s), plate.purpose.valueEquals(s)))
		lPlate.filter(_.purpose.valueEquals(s))
	}
	
	def queryWellByPlateKey(sPlateKey: String): List[Well] = {
		lWell.filter(_.parent.getKey.filter(_ equals sPlateKey).isDefined)
	}
	
	// Wells for the new pools
	def findNewPoolWells(lPool: List[Pool]): Map[Pool, List[Well]] = {
		val lsPurpose0 = lPool.map(_.sPurpose)
		val lsPurpose = lsPurpose0.distinct
		val mapPurposeToCount = lsPurpose0.groupBy(identity).mapValues(_.length)
		val mapPurposeToPlates = lsPurpose.map(sPurpose => sPurpose -> queryPlateByPurpose(sPurpose)).toMap
		val lPlate = mapPurposeToPlates.values.flatten
		val mapPlateToFreeIndex = lPlate.map(plate => {
			val lWell = queryWellByPlateKey(plate.key)
			val index = lWell.foldLeft(0) {(acc, well) => well.index.getValue match {
				case None => acc
				case Some(i) => math.max(acc, i.value + 1)
			}}
			plate -> index
		})
		val map = new HashMap[Plate, Int]() ++ mapPlateToFreeIndex
		println(lsPurpose0, lsPurpose, mapPurposeToCount, mapPurposeToPlates)
		lPool.map(pool => {
			for {
				lPlate <- mapPurposeToPlates.get(pool.sPurpose)
				plate <- lPlate.headOption
				index <- map.get(plate)
			} yield {
				map(plate) = index + 1
				pool -> List(Well(parent = TempKey(plate.key), index = Temp1(new PInteger(index))))
			}
		}).flatten.toMap
	}
	
	def run {
		val test1 = new Test1
		val p = new Process(test1.l)
		val lKeyPair = p.getClassKey()
		lKeyPair.foreach(println)
		val mapDb = lKeyPair.map(pair => pair -> lookup(pair)).toMap
		mapDb.foreach(println)
		
		println()
		println("findWells:")
		val lsLiquidKey = mapDb.toList.map(_._1._2)
		val mapLiquidKeyToWells = findWells(lsLiquidKey)
		mapLiquidKeyToWells.foreach(println)

		println()
		println("findPlates:")
		val lsPlateKeySrc = findPlates(mapLiquidKeyToWells)
		lsPlateKeySrc.foreach(println)
		
		println()
		println("getNewPools:")
		val lPool = p.getNewPools()
		lPool.foreach(println)
		
		println()
		println("findNewPoolWells:")
		val mapPoolToWells = findNewPoolWells(lPool)
		mapPoolToWells.foreach(println)
		
		println()
		println("all plates:")
		val lsPlateKeyDest = mapPoolToWells.toList.flatMap(_._2).map(_.parent.getKey).flatten
		val lsPlateKey = lsPlateKeySrc ++ lsPlateKeyDest
		lsPlateKey.foreach(println)
		
		println()
		println("choosePlateLocations:")
		val lPlate = lsPlateKey.map(sPlateKey => lookup(("Plate", sPlateKey)).map(_.asInstanceOf[Plate])).flatten
		
	}
}
