package roboliq.common

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet


abstract class Obj {
	type ConfigL1
	type ConfigL3
	type StateL1
	type StateL3
	
	def createConfigL1(configL3: ConfigL3): Either[Seq[String], ConfigL1]
	def createConfigL3(): ConfigL3
	def createState0L1(state3: StateL3): Either[Seq[String], StateL1]
	def createState0L3(): StateL3

	def createConfigL1(map: scala.collection.Map[Obj, Any]): Either[Seq[String], ConfigL1] = {
		getConfigL3(map) match {
			case Some(c3) => createConfigL1(c3)
			case _ => Left(List("object's level 3 config not found"))
		}
	}
	
	def createState0L1(map: scala.collection.Map[Obj, Any]): Either[Seq[String], StateL1] = {
		getState0L3(map) match {
			case Some(st3) => createState0L1(st3)
			case _ => Left(List("object's level 3 config not found"))
		}
	}
	
	def getConfigL1(map: scala.collection.Map[Obj, Any]): Option[ConfigL1] = getFromMap(map)
	def getConfigL3(map: scala.collection.Map[Obj, Any]): Option[ConfigL3] = getFromMap(map)
	def getState0L1(map: scala.collection.Map[Obj, Any]): Option[StateL1] = getFromMap(map)
	def getState0L3(map: scala.collection.Map[Obj, Any]): Option[StateL3] = getFromMap(map)
	
	private def getFromMap[T](map: scala.collection.Map[Obj, Any]): Option[T] = map.get(this) match {
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
	
	def createConfigL1(c3: ConfigL3): Either[Seq[String], ConfigL1] = {
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
) 

class WellConfigL3 {
	var holder_? : Option[Plate] = None
	var index_? : Option[Int] = None
}

class WellStateL1(
	val well: Well,
	val liquid: Liquid,
	val nVolume: Double
)

class WellStateL3 {
	var bRequiresIntialLiq_? : Option[Boolean] = None
	var liquid_? : Option[Liquid] = None
	var nVolume_? : Option[Double] = None
}

class Plate extends Obj {
	type ConfigL1 = PlateConfigL1
	type ConfigL3 = PlateConfigL3
	type StateL1 = PlateStateL1
	type StateL3 = PlateStateL3
	
	def createConfigL1(c3: ConfigL3): Either[Seq[String], ConfigL1] = {
		import c3._
		val errors = new ArrayBuffer[String]
		if (c1_?.isEmpty)
			errors += "dimension not set"
		if (!errors.isEmpty)
			return Left(errors)
			
		Right(new PlateConfigL1(
				nRows = c1_?.get.nRows,
				nCols = c1_?.get.nCols,
				nWells = c1_?.get.nWells,
				wells = c1_?.get.wells))
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
	val wells: Seq[Well]
)

class PlateConfigL3 {
	val c1_? : Option[PlateConfigL1] = None
}

class PlateStateL1(
	val plate: Plate,
	val location: String
)

class PlateStateL3 {
	var location_? : Option[String] = None
}
