package roboliq.common

import scala.collection
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet


trait AbstractConfigL1
trait AbstractConfigL3
trait AbstractStateL1
trait AbstractStateL3

abstract class Obj {
	type ConfigL1 <: AbstractConfigL1
	type ConfigL3 <: AbstractConfigL3
	type StateL1 <: AbstractStateL1
	type StateL3 <: AbstractStateL3
	
	def createConfigL1(configL3: ConfigL3, map1: collection.Map[Obj, AbstractConfigL1]): Either[Seq[String], ConfigL1]
	def createConfigL3(): ConfigL3
	def createState0L1(state3: StateL3): Either[Seq[String], StateL1]
	def createState0L3(): StateL3

	def createConfigL1(map3: collection.Map[Obj, AbstractConfigL3], map1: collection.Map[Obj, AbstractConfigL1]): Either[Seq[String], ConfigL1] = {
		getConfigL3(map3) match {
			case Some(c3) => createConfigL1(c3, map1)
			case _ => Left(List("object's level 3 config not found"))
		}
	}
	
	def createState0L1(map: scala.collection.Map[Obj, AbstractStateL3]): Either[Seq[String], StateL1] = {
		getState0L3(map) match {
			case Some(st3) => createState0L1(st3)
			case _ => Left(List("object's level 3 config not found"))
		}
	}
	
	def getConfigL1(map31: ObjMapper): Option[ConfigL1] = getConfigL1(map31.configL1)
	def getConfigL3(map31: ObjMapper): Option[ConfigL3] = getConfigL3(map31.configL3)
	def getState0L1(map31: ObjMapper): Option[StateL1] = getState0L1(map31.state0L1)
	def getState0L3(map31: ObjMapper): Option[StateL3] = getState0L3(map31.state0L3)
	
	def getConfigL1(map: scala.collection.Map[Obj, AbstractConfigL1]): Option[ConfigL1] = getFromMap(map)
	def getConfigL3(map: scala.collection.Map[Obj, AbstractConfigL3]): Option[ConfigL3] = getFromMap(map)
	def getState0L1(map: scala.collection.Map[Obj, AbstractStateL1]): Option[StateL1] = getFromMap(map)
	def getState0L3(map: scala.collection.Map[Obj, AbstractStateL3]): Option[StateL3] = getFromMap(map)
	
	private def getFromMap[T](map: scala.collection.Map[Obj, Object]): Option[T] = map.get(this) match {
		case Some(o) =>
			if (o.isInstanceOf[T])
				Some(o.asInstanceOf[T])
			else
				None
		case _ =>
			None
	}
}

sealed class Setting[T] {
	var default_? : Option[T] = None
	var user_? : Option[T] = None
	var possible: List[T] = Nil
	
	def get = user_? match {
		case None =>
			default_?.get
		case Some(o) =>
			o
	}
	
	def get_? : Option[T] = user_? match {
		case None =>
			default_?
		case Some(o) =>
			user_?
	} 
	
	def isDefined: Boolean = { user_?.isDefined || default_?.isDefined }
	def isEmpty: Boolean = !isDefined
}

class Well extends Obj {
	type ConfigL1 = WellConfigL1
	type ConfigL3 = WellConfigL3
	type StateL1 = WellStateL1
	type StateL3 = WellStateL3
	
	def createConfigL1(c3: ConfigL3, map1: collection.Map[Obj, AbstractConfigL1]): Either[Seq[String], ConfigL1] = {
		import c3._
		val errors = new ArrayBuffer[String]
		if (holder_?.isEmpty)
			errors += "index not set"
		if (index_?.isEmpty)
			errors += "holder not set"
		if (!errors.isEmpty)
			return Left(errors)
			
		Right(new WellConfigL1(
				well = this,
				holder = holder_?.get,
				index = index_?.get))
	}

	def createConfigL3() = new ConfigL3
	
	def createState0L1(state3: StateL3): Either[Seq[String], StateL1] = {
		import state3._
		val errors = new ArrayBuffer[String]
		if (liquid_?.isEmpty)
			errors += "liquid not set"
		if (nVolume_?.isEmpty)
			errors += "volume not set"
		if (!errors.isEmpty)
			return Left(errors)
			
		Right(new WellStateL1(
				well = this,
				liquid = liquid_?.get,
				nVolume = nVolume_?.get))
	}
	
	def createState0L3() = new StateL3
}

class WellConfigL1(
	val well: Well,
	val holder: Plate,
	val index: Int
) extends AbstractConfigL1

class WellConfigL3 extends AbstractConfigL3 {
	var holder_? : Option[Plate] = None
	var index_? : Option[Int] = None
}

class WellStateL1(
	val well: Well,
	val liquid: Liquid,
	val nVolume: Double
) extends AbstractStateL1

class WellStateL3 extends AbstractStateL3 {
	var bRequiresIntialLiq_? : Option[Boolean] = None
	var liquid_? : Option[Liquid] = None
	var nVolume_? : Option[Double] = None
}

class Plate extends Obj {
	type ConfigL1 = PlateConfigL1
	type ConfigL3 = PlateConfigL3
	type StateL1 = PlateStateL1
	type StateL3 = PlateStateL3
	
	def createConfigL1(c3: ConfigL3, map1: collection.Map[Obj, AbstractConfigL1]): Either[Seq[String], ConfigL1] = {
		c3.dim_? match {
			case None =>
				Left(Seq("dimension not set"))
			case Some(dim) =>
				if (dim.wells.forall(map1.contains)) {
					val wells1 = dim.wells.map(well => well.getConfigL1(map1).get)
					Right(new PlateConfigL1(
							nRows = dim.nRows,
							nCols = dim.nCols,
							nWells = dim.nRows * dim.nCols,
							wells = wells1))
				}
				else {
					Left(Seq("missing level1 config for wells"))
				}
		}
	}

	def createConfigL3() = new ConfigL3
	
	def createState0L1(state3: StateL3): Either[Seq[String], StateL1] = {
		import state3._
		val errors = new ArrayBuffer[String]
		if (location_?.isEmpty)
			errors += "location not set"
		if (!errors.isEmpty)
			return Left(errors)
			
		Right(new PlateStateL1(
				plate = this,
				location = location_?.get))
	}
	
	def createState0L3() = new StateL3
}

class PlateConfigL1(
	val nRows: Int,
	val nCols: Int,
	val nWells: Int,
	val wells: Seq[WellConfigL1]
) extends AbstractConfigL1

class PlateConfigL3 extends AbstractConfigL3 {
	val dim_? : Option[PlateConfigDimensionL3] = None
}

class PlateConfigDimensionL3(
	val nRows: Int,
	val nCols: Int,
	val wells: Seq[Well]
)

class PlateStateL1(
	val plate: Plate,
	val location: String
) extends AbstractStateL1

class PlateStateL3 extends AbstractStateL3 {
	var location_? : Option[String] = None
}
