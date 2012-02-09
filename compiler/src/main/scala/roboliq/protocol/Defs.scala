package roboliq.protocol

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._

	
object LiquidProperties extends Enumeration {
	val Water, Glycerol = Value
}

class Volume(n: Double) {
	def ul = n
	def ml = n * 1000
}

/** Volume in picoliters */
class LiquidVolume(pl: Int) {
	override def toString = {
		if (pl > 1000000)
			(pl / 1000000).toString + " ml"
		else if (pl > 1000)
			(pl / 1000).toString + " ul"
		else
			pl.toString + " pl"
	}
}
object LiquidVolume {
	def pl(n: Int): LiquidVolume = new LiquidVolume(n)
	def ul(n: Int): LiquidVolume = new LiquidVolume(n * 1000)
	def ml(n: Int): LiquidVolume = new LiquidVolume(n * 1000000)
}

abstract class LiquidAmount
case class LiquidAmountByVolume(vol: LiquidVolume) extends LiquidAmount {
	override def toString = vol.toString 
}
case class LiquidAmountByConc(conc: BigDecimal) extends LiquidAmount {
	override def toString = conc.toString
}

class Pool(val sPurpose: String) {
	var liquid0_? : Option[Liquid] = None
	var volume0_? : Option[LiquidVolume] = None
	//var purpose: String = null
}

class Sample(val liquid: Liquid, val volume: LiquidVolume)

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

/** Volume in picoliters */
class PLiquidVolume(vol: LiquidVolume) extends PObject {
	def properties: List[Property[_]] = Nil
	override def toString = vol.toString
}

class PLiquidAmount(amt: LiquidAmount) extends PObject {
	def properties: List[Property[_]] = Nil
	override def toString = amt.toString
	/*override def equals(o: Any): Boolean = o match {
		case that: PL => value == that.value
		case _ => false
	}*/
}

abstract class PCommand extends PObject {
	//def getSources(nb: NameBase): List[PropertyValue[_]]
	def getNewPools(): List[Pool] = Nil
}

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
	def toContentString(): Option[String] = None
}
case class PropertyRefId[A <: PObject](id: String)(implicit m: Manifest[A]) extends PropertyValue[A]()(m) {
}
case class PropertyRefDb[A <: PObject](key: String)(implicit m: Manifest[A]) extends PropertyValue[A]()(m) {
	override def getKey: Option[String] = Some(key)
	override def keyEquals(sKey: String): Boolean = (key == sKey)
	override def toContentString(): Option[String] = Some("K\""+key+"\"")
	def getKeyPair: Tuple2[String, String] = (m.erasure.getCanonicalName() -> key)
}
case class PropertyRefProperty[A <: PObject](p: Property[A])(implicit m: Manifest[A]) extends PropertyValue[A]()(m) {
	override def getKey: Option[String] = p.getKey
	override def getValues: List[A] = p.getValues
}
case class PropertyPObject[A <: PObject](val value: A)(implicit m: Manifest[A]) extends PropertyValue[A]()(m) {
	override def getKey: Option[String] = Some(value.key)
	override def getValues: List[A] = List(value)
	override def keyEquals(sKey: String): Boolean = (sKey != null && sKey == value.key)
	override def toContentString(): Option[String] = Some(value.toString)
}

class Liquid extends PObject {
	var physical: String = null
	var cleanPolicy: String = null
	var contaminants: List[String] = null
	def properties: List[Property[_]] = Nil
	var components: List[Sample] = Nil
}

class Plate extends PObject {
	val model = new Property[PString]
	val label = new Property[PString]
	val description = new Property[PString]
	val purpose = new Property[PString]
	def properties: List[Property[_]] = List(model, label)
	
	override def toString =
		(key :: List(model, label, description, purpose).flatMap(_.getValue)).mkString("Plate(", ", ", ")")
}

class Well extends PObject {
	val parent = new Property[Plate]
	val index = new Property[PInteger]
	val liquid = new Property[Liquid]
	val volume = new Property[PLiquidVolume]
	def properties: List[Property[_]] = List(parent, index, liquid, volume)
}

object Well {
	def apply(
		parent: TempValue[Plate] = TempNull[Plate],
		index: TempValue[PInteger] = TempNull[PInteger],
		liquid: TempValue[Liquid] = TempNull[Liquid],
		volume: TempValue[PLiquidVolume] = TempNull[PLiquidVolume]
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
	val volumes = new Property[PLiquidVolume]
	val mixSpec = new Property[PcrMixSpec]
	
	def properties: List[Property[_]] = List(products, volumes, mixSpec)

	override def getNewPools(): List[Pool] = {
		List.fill(products.values.length)(new Pool("PCR"))
	}
}

class PcrProduct extends PObject {
	val template = new Property[Liquid]
	val forwardPrimer = new Property[Liquid]
	val backwardPrimer = new Property[Liquid]
	def properties: List[Property[_]] = List(template, forwardPrimer, backwardPrimer)
}

class PcrMixSpec extends PObject {
	class Item {
		val liquid = new Property[Liquid]
		val amt0 = new Property[PLiquidAmount]
		val amt1 = new Property[PLiquidAmount]
	}
	val waterLiquid = new Property[Liquid]
	val buffer = new Item
	val dntp = new Item
	val templateLiquid = new Property[Liquid]
	val templateConc = new Property[PLiquidAmount]
	val forwardPrimerLiquid = new Property[Liquid]
	val forwardPrimerConc = new Property[PLiquidAmount]
	val backwardPrimerLiquid = new Property[Liquid]
	val backwardPrimerConc = new Property[PLiquidAmount]
	val polymerase = new Item
	def properties: List[Property[_]] = waterLiquid :: List(buffer, dntp, polymerase).flatMap(item => List[Property[_]](item.liquid, item.amt0, item.amt1))
}

class Tube extends PObject {
	val liquid = new Property[Liquid]
	val conc = new Property[PLiquidAmount]
	val volume = new Property[PLiquidVolume]
	//val location = new Property[String]
	def properties: List[Property[_]] = List(liquid, conc, volume)//, location)
}

class Test1 {
	class IntToVolumeWrapper(n: Int) {
		def pl: PLiquidVolume = new PLiquidVolume(new LiquidVolume(n))
		def ul: PLiquidVolume = new PLiquidVolume(new LiquidVolume(n * 1000))
		def ml: PLiquidVolume = new PLiquidVolume(new LiquidVolume(n * 1000000))
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

				buffer.liquid := refDb("buffer5x")
				buffer.amt0 := new PLiquidAmount(LiquidAmountByConc(10))
				buffer.amt1 := new PLiquidAmount(LiquidAmountByConc(1))
				
				dntp.liquid := refDb("dntp")
				dntp.amt0 := new PLiquidAmount(LiquidAmountByConc(2))
				dntp.amt1 := new PLiquidAmount(LiquidAmountByConc(0.2))
				
				polymerase.liquid := refDb("polymerase")
				polymerase.amt0 := new PLiquidAmount(LiquidAmountByConc(5))
				polymerase.amt1 := new PLiquidAmount(LiquidAmountByConc(0.01))
			}
		}
	)
}

class Process(items: List[PObject]) {
	def getRefDbs(): List[PropertyRefDb[_]] = items.flatMap(_.getRefDbs())
	def getClassKey(): List[Tuple2[String, String]] = getRefDbs().map(_.getKeyPair).distinct
	def getCommands(): List[PCommand] = items.collect({case cmd: PCommand => cmd})
	def getNewPools(): List[Pool] = getCommands().flatMap(_.getNewPools)
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
		new Liquid { key = "buffer10x"; },
		new Liquid { key = "buffer5x"; },
		new Liquid { key = "dntp"; },
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
		new Plate { key = "P2"; model := new PString("D-BSSE 96 Well PCR Plate"); description := new PString("buffer and dntp") },
		new Plate { key = "P3"; model := new PString("D-BSSE 96 Well PCR Plate"); description := new PString("polymerase") },
		new Plate { key = "P4"; model := new PString("D-BSSE 96 Well PCR Plate"); description := new PString("PCR products"); purpose := new PString("PCR") }
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
		Well(parent = TempKey("P1"), index = Temp1(new PInteger(7)), liquid = TempKey("FRP572")),
		Well(parent = TempKey("P2"), index = Temp1(new PInteger(0)), liquid = TempKey("buffer5x")),
		Well(parent = TempKey("P2"), index = Temp1(new PInteger(1)), liquid = TempKey("dntp")),
		Well(parent = TempKey("P3"), index = Temp1(new PInteger(0)), liquid = TempKey("polymerase"))
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
		//if (!mapClassToTable.contains(pair._1))
		//	println("lookup: class not found: "+pair._1)
		val sTable = mapClassToTable.getOrElse(pair._1, pair._1)
		for {
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
		val lsPlateKey = (lsPlateKeySrc ++ lsPlateKeyDest).distinct
		val lPlate = lsPlateKey.map(sPlateKey => lookup(("Plate", sPlateKey)).map(_.asInstanceOf[Plate])).flatten
		lPlate.foreach(println)
		
		// This task is platform specific.  In our case: tecan evoware. 
		println()
		println("choosePlateLocations:")
		val mapLocFree = new HashMap[String, List[String]]
		mapLocFree += "D-BSSE 96 Well PCR Plate" -> List("cooled1", "cooled2", "cooled3", "cooled4", "cooled5")
		val mapRack = new HashMap[String, List[Int]]
		mapRack += "Tube 50ml" -> (0 until 8).toList
		lPlate.flatMap(plate => {
			plate.model.getValue match {
				case None => None
				case Some(psModel) =>
					val sModel = psModel.value
					mapLocFree.get(sModel) match {
						case None =>
							mapRack.get(sModel) match {
								case None => None
								case Some(li) =>
									mapRack(sModel) = li.tail
									Some(plate -> li.head.toString)
							}
						case Some(Nil) => None
						case Some(ls) =>
							mapLocFree(sModel) = ls.tail
							Some(plate -> ls.head)
					}
			}
		}).foreach(println)
	}
}
