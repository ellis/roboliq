package roboliq.protocol

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import roboliq.common
import roboliq.common._
import common.WellPointerReagent


object LiquidProperties extends Enumeration {
	val Water, Glycerol = Value
}

class Volume(n: Double) {
	def ul = n
	def ml = n * 1000
}

case class Route(sClass: String, sKey: String) {
	def toPair: Tuple2[String, String] = (sClass, sKey)
}

/** Volume in picoliters */
class LiquidVolume(val pl: Int) {
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

sealed abstract class LiquidAmount
case class LiquidAmountByVolume(vol: LiquidVolume) extends LiquidAmount {
	override def toString = vol.toString 
}
case class LiquidAmountByConc(conc: BigDecimal) extends LiquidAmount {
	override def toString = conc.toString
}

/*sealed abstract class WellPointer
case class WellPointerLiquidKey(sLiquidKey: String) extends WellPointer
case class WellPointerLiquid(liquid: Liquid) extends WellPointer
case class WellPointerWell()*/

class Pool(val sPurpose: String) {
	var liquid0_? : Option[Liquid] = None
	var volume0_? : Option[LiquidVolume] = None
}

class Sample(val liquid: Liquid, val volume: LiquidVolume)

abstract class Item {
	var key: String = null
	def properties: List[Property[_]]
	
	//def refId(id: String): TempNameA = new TempNameA(id)
	def refDb(key: String): TempKeyA = new TempKeyA(key)
	def refKey[A](key: String): TempKey[A] = new TempKey[A](key)

	def gatherValueKeys: List[ValueKey[_]] = properties.flatMap(_.gatherValueKeys).distinct
	
	def toContentString: Option[String] = Some(toString)

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

/*
case class PString(val value: String) extends Item {
	def properties: List[Property[_]] = Nil
	override def toString(): String = "\"" + value.toString() + "\""
	override def equals(o: Any): Boolean = o match {
		case that: PString => value == that.value
		case _ => false
	}
}

class PInteger(val value: Int) extends Item {
	def properties: List[Property[_]] = Nil
	override def toString(): String = value.toString()
	override def equals(o: Any): Boolean = o match {
		case that: PInteger => value == that.value
		case _ => false
	}
}
*/

//sealed abstract class PLocation extends Item

/** Volume in picoliters */
class PLiquidVolume(vol: LiquidVolume) extends Item {
	def properties: List[Property[_]] = Nil
	override def toString = vol.toString
}

class PLiquidAmount(amt: LiquidAmount) extends Item {
	def properties: List[Property[_]] = Nil
	override def toString = amt.toString
}

abstract class PCommand extends Item {
	def getNewPools(): List[Pool] = Nil
	def createCommands(vom: ValueToObjectMap): List[common.Command]
}

class Property[A](implicit m: Manifest[A]) {
	var values: List[Value[A]] = Nil
	def :=(a: A) { values = List(ValueBasic(a)) }
	def :=(ref: TempKeyA) { values = List(ValueKey[A](ref.key)) }
	def :=(la: List[A]) { values = la.map(a => ValueBasic(a)) }
	def set(v: TempValue[A]) = v match {
		case TempNull() => values = Nil
		case Temp1(a) => :=(a)
		case TempKey(s) => :=(new TempKeyA(s))
		case TempList(la) => :=(la)
	}
	
	def gatherValueKeys: List[ValueKey[_]] = values.flatMap(_.gatherValueKeys).distinct

	def getValueKey: Option[ValueKey[_]] = values match {
		case List(value) => value.getValueKey
		case _ => None
	}
	def getValues(db_? : Option[ValueDatabase]): List[A] = values.flatMap(_.getValues(db_?))
	def getValue(db_? : Option[ValueDatabase]): Option[A] = getValues(db_?) match {
		case List(v) => Some(v)
		case _ => None
	}
	def getValue(db: ValueDatabase): Option[A] = getValue(Some(db))
	def getValue: Option[A] = getValue(None)
	def getValues(db: ValueDatabase): List[A] = getValues(Some(db))
	
	def valueEquals(a: A, db_? : Option[ValueDatabase]): Boolean = (getValue(db_?) == Some(a))

	def toContentString: Option[String] = {
		values match {
			case Nil => None
			case a :: Nil => a.toContentString
			case _ => Some(values.map(_.toContentString).flatten.mkString("[ ", ", ", " ]"))
		}
	}
}

class PropertyItem[A <: Item](implicit m: Manifest[A]) extends Property[A]()(m) {
	override def :=(a: A) { values = List(ValueItem(a)(m)) }
	override def :=(la: List[A]) { values = la.map(a => ValueItem(a)) }
}

abstract class Value[A](implicit m: Manifest[A]) {
	def getValueKey: Option[ValueKey[_]]
	def gatherValueKeys: List[ValueKey[_]]
	def getValues(db_? : Option[ValueDatabase]): List[A]
	def toContentString: Option[String]
}
case class ValueBasic[A](value: A)(implicit m: Manifest[A]) extends Value[A]()(m) {
	def getValueKey: Option[ValueKey[_]] = None
	def gatherValueKeys: List[ValueKey[_]] = Nil
	def getValues(db_? : Option[ValueDatabase]): List[A] = value :: Nil
	def toContentString(): Option[String] = Some(value.toString)
}
case class ValueItem[A <: Item](item: A)(implicit m: Manifest[A]) extends Value[A]()(m) {
	def getValueKey: Option[ValueKey[_]] = None
	def gatherValueKeys: List[ValueKey[_]] = item.gatherValueKeys
	def getValues(db_? : Option[ValueDatabase]): List[A] = item :: Nil
	def toContentString(): Option[String] = Some(item.toString)
}
case class ValueKey[A](key: String)(implicit m: Manifest[A]) extends Value[A]()(m) {
	def getValueKey: Option[ValueKey[_]] = Some(this)
	def gatherValueKeys: List[ValueKey[_]] = List(this)
	def getValues(db_? : Option[ValueDatabase]): List[A] = db_? match {
		case Some(db) => db.lookup[A](this).toList
		case _ => Nil
	}
	def toContentString(): Option[String] = Some("K\""+key+"\"")

	def route = Route(m.erasure.getCanonicalName(), key)
	override def toString: String = route.toString
}
case class ValueProperty[A](ptr: Property[A])(implicit m: Manifest[A]) extends Value[A]()(m) {
	def getValueKey: Option[ValueKey[_]] = None
	def gatherValueKeys: List[ValueKey[_]] = ptr.gatherValueKeys
	def getValues(db_? : Option[ValueDatabase]): List[A] = ptr.getValues(db_?)
	def toContentString(): Option[String] = Some("*"+ptr.toContentString)
}

class ValueDatabase(map: Map[Value[_], Any]) {
	def lookup[A](key: Value[A]): Option[A] = map.get(key) match {
		case None => None
		case Some(v) => Some(v.asInstanceOf[A])
	}
}
/*
object ValueDatabase {
	val empty = new ValueDatabase {
		def lookup[A](value: Value[A]): Option[A] = None
	}
}*/

class TempKeyA(val key: String)
//class TempNameA(val name: String)

sealed abstract class TempValue[A]
case class TempNull[A]() extends TempValue[A]
case class Temp1[A](val a: A) extends TempValue[A]
case class TempKey[A](val key: String) extends TempValue[A]
//case class TempName[A](val name: String) extends TempValue[A]
case class TempList[A](val la: List[A]) extends TempValue[A]

/*
class NameBase(val mapVars: Map[String, Item], val mapDb: Map[String, Item]) {
	def apply(name: String): Option[Item] = {
		mapVars.get(name).orElse(mapDb.get(name))
	}
}
*/

/*
sealed abstract class PropertyValue[A <: Item](implicit m: Manifest[A]) {
	def getKey: Option[String] = None
	def getValues: List[A] = Nil
	def keyEquals(sKey: String): Boolean = false
	def toContentString(): Option[String] = None
}
case class PropertyRefId[A <: Item](id: String)(implicit m: Manifest[A]) extends PropertyValue[A]()(m) {
}
case class PropertyRefDb[A <: Item](key: String)(implicit m: Manifest[A]) extends PropertyValue[A]()(m) {
	override def getKey: Option[String] = Some(key)
	override def keyEquals(sKey: String): Boolean = (key == sKey)
	override def toContentString(): Option[String] = Some("K\""+key+"\"")
	def getKeyPair: Tuple2[String, String] = (m.erasure.getCanonicalName() -> key)
}
case class PropertyRefProperty[A <: Item](p: Property[A])(implicit m: Manifest[A]) extends PropertyValue[A]()(m) {
	override def getKey: Option[String] = p.getKey
	override def getValues: List[A] = p.getValues
}
case class PropertyPObject[A <: Item](val value: A)(implicit m: Manifest[A]) extends PropertyValue[A]()(m) {
	override def getKey: Option[String] = Some(value.key)
	override def getValues: List[A] = List(value)
	override def keyEquals(sKey: String): Boolean = (sKey != null && sKey == value.key)
	override def toContentString(): Option[String] = Some(value.toString)
}
*/

class Liquid extends Item {
	val physical = new Property[String]
	val cleanPolicy = new Property[String]
	var contaminants = new Property[String]
	var components: List[Sample] = Nil

	def properties: List[Property[_]] = List(physical, cleanPolicy)
}

class PlateModel extends Item {
	val rows = new Property[Int]
	val cols = new Property[Int]
	
	def properties: List[Property[_]] = List(rows, cols)
}

class Plate extends Item {
	val model = new Property[String]
	val label = new Property[String]
	val description = new Property[String]
	val purpose = new Property[String]
	def properties: List[Property[_]] = List(model, label)
	
	override def toString =
		(key :: List(model, label, description, purpose).flatMap(_.toContentString)).mkString("Plate(", ", ", ")")
}

class Well extends Item {
	val parent = new PropertyItem[Plate]
	val index = new Property[Int]
	val liquid = new PropertyItem[Liquid]
	val volume = new Property[LiquidVolume]
	def properties: List[Property[_]] = List(parent, index, liquid, volume)
}

object Well {
	def apply(
		parent: TempValue[Plate] = TempNull[Plate],
		index: TempValue[Int] = TempNull[Int],
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

class Tube extends Item {
	val liquid = new Property[Liquid]
	val conc = new Property[PLiquidAmount]
	val volume = new Property[PLiquidVolume]
	def properties: List[Property[_]] = List(liquid, conc, volume)//, location)
}

class ItemListData(
	val lItem: List[Item],
	val valueDb: ValueDatabase,
	//val mapPropertyToItem: Map[Property[_], Option[Liquid]],
	val mapValueKeyToItem: Map[ValueKey[_], Option[Item]],
	//val mapRouteToItem: Map[Route, Option[Item]],
	val mapKeyToPlateModel: Map[String, PlateModel],
	val mapLiquidKeyToWells: Map[String, List[Well]],
	//val lPlateSource: List[Plate],
	val lLiquid: List[Liquid],
	val lPoolNew: List[Pool],
	val mapPoolToWellsNew: Map[Pool, List[Well]],
	val lPlate: List[Plate]
) {
	
}

object ItemListData {
	def apply(lItem: List[Item], db: ItemDatabase): ItemListData = {
		val builder = new ItemListDataBuilder(lItem, db)
		builder.run
	}
}

trait ItemDatabase {
	def lookupItem(pair: Tuple2[String, String]): Option[Item]
	def lookupItem(route: Route): Option[Item] = lookupItem(route.toPair)
	def lookup[A](pair: Tuple2[String, String]): Option[A]
	def findWellsByLiquid(sLiquidKey: String): List[Well]
	def findWellsByPlateKey(sPlateKey: String): List[Well]
	def findPlateByPurpose(sPurpose: String): List[Plate]
}

private class ItemListDataBuilder(items: List[Item], db: ItemDatabase) {
	def gatherValueKeys(): List[ValueKey[_]] = items.flatMap(_.gatherValueKeys)
	//def getClassKey(): List[Tuple2[String, String]] = getRefDbs().map(_.getKeyPair).distinct
	def getCommands(): List[PCommand] = items.collect({case cmd: PCommand => cmd})
	def getNewPools(): List[Pool] = getCommands().flatMap(_.getNewPools)

	def findWells(lsLiquidKey: List[String]): Map[String, List[Well]] = {
		lsLiquidKey.map(sLiquidKey => sLiquidKey -> db.findWellsByLiquid(sLiquidKey)).toMap
	}
	
	def findPlates(mapLiquidKeyToWells: Map[String, List[Well]]): List[String] = {
		val lLiquidKeyToWells = mapLiquidKeyToWells.toList
		val lLiquidKeyToPlateKeys: List[Tuple2[String, List[String]]]
			= lLiquidKeyToWells.map(pair => pair._1 -> pair._2.flatMap(_.parent.getValueKey.map(_.key)).distinct)
		// Keys of the plates which are absolutely required (i.e. liquid is not available on a different plate too)
		val lRequired: Set[String] = lLiquidKeyToPlateKeys.collect({case (_, List(sPlateKey)) => sPlateKey}).toSet
		// List of the items whose plates are not in lRequired
		val lRemaining = lLiquidKeyToPlateKeys.map({case (sLiquidKey, lPlate) => {
			val lPlate2 = lPlate.filter(sPlateKey => !lRequired.contains(sPlateKey))
			lPlate2 match {
				case Nil => None
				case _ => Some(sLiquidKey -> lPlate2)
			}
		}}).flatten
		val lChosen = lRemaining.map(_._2.head)
		(lRequired ++ lChosen).toList
	}
	
	// Wells for the new pools
	def findNewPoolWells(lPool: List[Pool]): Map[Pool, List[Well]] = {
		val lsPurpose0 = lPool.map(_.sPurpose)
		val lsPurpose = lsPurpose0.distinct
		val mapPurposeToCount = lsPurpose0.groupBy(identity).mapValues(_.length)
		val mapPurposeToPlates = lsPurpose.map(sPurpose => sPurpose -> db.findPlateByPurpose(sPurpose)).toMap
		val lPlate = mapPurposeToPlates.values.flatten
		val mapPlateToFreeIndex = lPlate.map(plate => {
			val lWell = db.findWellsByPlateKey(plate.key)
			val index = lWell.foldLeft(0) {(acc, well) => well.index.getValue match {
				case None => acc
				case Some(i) => math.max(acc, i + 1)
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
				pool -> List(Well(parent = TempKey(plate.key), index = Temp1(index)))
			}
		}).flatten.toMap
	}
	
	def run: ItemListData = {
		val lValueKey: List[ValueKey[_]] = gatherValueKeys()
		lValueKey.foreach(println)
		//val lValueKeyToItem: List[Tuple2[ValueKey[_], Option[Item]]] = lValueKey.map(v => Tuple2[ValueKey[_], Option[Item]](v, db.lookupItem(v.route)))
		val mapValueKeyToItem: Map[ValueKey[_], Option[Item]] = lValueKey.map(v => v -> db.lookupItem(v.route)).toMap
		println("mapValueKeyToItem:")
		mapValueKeyToItem.foreach(println)
		//val mapDb = lKeyPair.map(pair => pair -> db.lookupItem(pair)).toMap
		//mapDb.foreach(println)
		
		items.flatMap(item => item.properties)
		
		val lLiquid = mapValueKeyToItem.values.toList.collect({case Some(liq: Liquid) => liq})
		println("lLiquid:")
		lLiquid.foreach(println)
		
		println()
		println("mapLiquidKeyToWells:")
		val lsLiquidKey = lLiquid.map(_.key)
		val mapLiquidKeyToWells = findWells(lsLiquidKey)
		mapLiquidKeyToWells.foreach(println)

		println()
		println("findPlates:")
		val lsPlateKeySrc = findPlates(mapLiquidKeyToWells)
		lsPlateKeySrc.foreach(println)
		
		println()
		println("getNewPools:")
		val lPool = getNewPools()
		lPool.foreach(println)
		
		println()
		println("findNewPoolWells:")
		val mapPoolToWells = findNewPoolWells(lPool)
		mapPoolToWells.foreach(println)
		
		println()
		println("all plates:")
		val lsPlateKeyDest = mapPoolToWells.toList.flatMap(_._2).flatMap(_.parent.getValueKey.map(_.key)).distinct
		val lsPlateKey = (lsPlateKeySrc ++ lsPlateKeyDest).distinct
		val lPlate = lsPlateKey.flatMap(sPlateKey => db.lookup[Plate](("Plate", sPlateKey)))
		lPlate.foreach(println)
		
		val lsPlateModel = lPlate.flatMap(_.model.getValue).distinct
		val mapKeyToPlateModel = lsPlateModel.flatMap(key => db.lookup[PlateModel]("PlateModel", key).map(item => key -> item)).toMap
		println()
		println("mapKeyToPlateModel:")
		mapKeyToPlateModel.foreach(println)
		
		/*
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
		*/
		
		new ItemListData(
			lItem = items,
			valueDb = new ValueDatabase(mapValueKeyToItem.toMap),
			mapValueKeyToItem = mapValueKeyToItem,
			//mapKeyToItem = mapDb,
			mapKeyToPlateModel = mapKeyToPlateModel,
			mapLiquidKeyToWells = mapLiquidKeyToWells,
			lLiquid = lLiquid,
			lPoolNew = lPool,
			mapPoolToWellsNew = mapPoolToWells,
			lPlate = lPlate
		)
	}
}

class ValueToObjectMap(
	val valueDb: ValueDatabase,
	val kb: KnowledgeBase,
	val mapKeyToPlateObj: Map[String, common.Plate],
	val mapValueToWellPointer: Map[Value[_], WellPointer],
	val mapPoolToWellPointer: Map[Pool, WellPointer]
)

object ValueToObjectMap {
	def apply(ild: ItemListData): ValueToObjectMap = {
		val kb = new KnowledgeBase
		
		// Create Reagent objects
		val mapReagents = ild.lLiquid.map(liquid => {
			val reagent = new Reagent
			val setup = kb.getReagentSetup(reagent)
			kb.addReagent(reagent)
			setup.sName_? = Some(liquid.key)
			setup.sFamily_? = liquid.physical.getValue(ild.valueDb).orElse(Some("Water"))
			setup.contaminants = liquid.contaminants.getValues(ild.valueDb).map(s => Contaminant.withName(s)).toSet
			liquid.cleanPolicy.getValue(ild.valueDb) match {
				case Some(s) =>
					val cleanPolicy = s match {
						case "TNT" => GroupCleanPolicy.TNT
						case "TNL" => GroupCleanPolicy.TNL
						case "DDD" => GroupCleanPolicy.DDD
					}
					setup.group_? = Some(new LiquidGroup(cleanPolicy))
				case _ =>
			}
			liquid.key -> reagent
		}).toMap
		println()
		println("mapReagents:")
		mapReagents.foreach(println)
		
		// Create plate models
		val lsPlateModel = ild.lPlate.flatMap(_.model.getValue(ild.valueDb)).distinct
		println("lsPlateModel:")
		lsPlateModel.foreach(println)
		val mapPlateModels: Map[String, common.PlateModel] = lsPlateModel.flatMap(sPlateModel => {
			for {
				plateModel <- ild.mapKeyToPlateModel.get(sPlateModel)
				nRows <- plateModel.rows.getValue(ild.valueDb)
				nCols <- plateModel.cols.getValue(ild.valueDb)
			} yield {
				val nWellVolume = -1.0 // FIXME: get real volume
				sPlateModel -> new common.PlateModel(sPlateModel, nRows, nCols, nWellVolume)
			}
		}).toMap
		println("mapPlateModels:")
		mapPlateModels.foreach(println)
		
		// Create plate objects
		val mapPlates = ild.lPlate.flatMap(plate => {
			for {
				sPlateModel <- plate.model.getValue
				plateModel <- mapPlateModels.get(sPlateModel)
			} yield {
				val obj = new roboliq.common.Plate
				val setup = kb.getPlateSetup(obj)
				setup.sLabel_? = Some(plate.key)
				setup.model_? = Some(plateModel)
				setup.setDimension(plateModel.nRows, plateModel.nCols)
				plate.key -> obj
			}
		}).toMap
		//val lPlate = mapKeyToPlateObj.values.toList
		println("mapPlates:")
		mapPlates.foreach(println)
		
		def getWellObject(well: Well): Option[common.Well] = {
			for {
				sPlateKey <- well.parent.getValueKey.map(_.key)
				plateObj <- mapPlates.get(sPlateKey)
				dim <- plateObj.setup.dim_?
				index <- well.index.getValue
			} yield {
				dim.wells(index)
			}
		}
		
		// Setup well objects with the given liquid
		for ((sLiquidKey, lWell) <- ild.mapLiquidKeyToWells) {
			mapReagents.get(sLiquidKey) match {
				case None =>
				case Some(reagentObj) =>
					for (well <- lWell) {
						println("well: ", well, getWellObject(well))
						for (wellObj <- getWellObject(well)) {
							wellObj.setup.sLabel_? = Some("W'"+sLiquidKey)
							wellObj.setup.reagent_? = Some(reagentObj)
							kb.addWell(wellObj, true)
						}
					}
			}
		}
		
		// Map of liquids -> WellPointerReagents
		val lValueKeyToWellPointer = ild.mapValueKeyToItem.toList.flatMap(pair => {
			val (value, item) = pair
			item match {
				case Some(liquid: Liquid) =>
					println("X:", liquid.key, ild.mapLiquidKeyToWells.get(liquid.key), mapReagents.get(liquid.key))
					for {
						lWell <- ild.mapLiquidKeyToWells.get(liquid.key)
						reagent <- mapReagents.get(liquid.key)
					} yield {
						value -> WellPointerReagent(reagent)
					}
				case _ => println("None"); None
			}
		})
		
		// Map of new pools -> WellPointerReagents
		val lPoolToWellPointer = ild.mapPoolToWellsNew.toList.map(pair => {
			val (pool, lWell) = pair
			val lWellObj = lWell.flatMap(well => {
				for (wellObj <- getWellObject(well))
				yield wellObj
			})
			pool -> WellPointerWells(lWellObj)			
		})
		
		println("kb:")
		println(kb.toString())
		
		println("lValueKeyToWellPointer:")
		lValueKeyToWellPointer.foreach(println)
		
		println("lPoolToWellPointer:")
		lPoolToWellPointer.foreach(println)
		
		new ValueToObjectMap(
			valueDb = ild.valueDb,
			kb = kb,
			mapKeyToPlateObj = mapPlates,
			mapValueToWellPointer = lValueKeyToWellPointer.toMap,
			mapPoolToWellPointer = lPoolToWellPointer.toMap
		)
	}
}
