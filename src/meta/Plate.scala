package meta

import scala.collection.mutable


/*
 * The robot consists of multiple locations
 * Locations may refer to carriers, plate receptacles, tube receptacles, trash bins, baths, hotels, interfaces to other devices
 * Plates can be open, sealed, or have covers
 * We need a way for objects to refer to each other, for example for a plate cover to know which plate it belongs to
 * Want to track all liquids throughout simulation/analysis
 * Plate attributes: location (or parent?), index, rows, cols
 * Well attributes: parent, index, volume
 */

object ValSource extends Enumeration {
	type ValSource = Value
	val Undetermined, Settings, User, Computer = Value
}

class Val {
	var source = ValSource.Undetermined
	var n = 0.0
}

object Val {
	def apply(source: ValSource.Value, n: Double): Val = {
		val v = new Val
		v.source = source
		v.n = n
		v
	}
}

object AttributeKind extends Enumeration {
	type AttributeKind = Value
	val Parent, Index, Rows, Cols, Volume, Cooled = Value
}

class Attribute(val kind: AttributeKind.Value) {
	private val map = new mutable.HashMap[Int, Val]
	
	def get(iStep: Int): Option[Val] = {
		var pair0: Tuple2[Int, Val] = null
		map.foreach { pair =>
			if (pair._1 <= iStep) {
				if (pair0 == null || pair._1 > pair0._1)
					pair0 = pair
			}
		}
		if (pair0 == null)
			None
		else
			Some(pair0._2)
	}
	
	def put(iStep: Int, v: Val) {
		map.put(iStep, v)
	}
}

class MetaObject {
	private val map = new mutable.HashMap[AttributeKind.Value, Attribute]

	val parent = createAttribute(AttributeKind.Parent)
	val index = createAttribute(AttributeKind.Index)
	
	def createAttribute(kind: AttributeKind.Value): Attribute = {
		val attr = new Attribute(kind)
		addAttribute(attr)
		attr
	}
	
	def addAttribute(attr: Attribute) = map.put(attr.kind, attr)
	
	def setAttribute(kind: AttributeKind.Value, iStep: Int, v: Val) {
		val attr = createAttribute(kind)
		attr.put(iStep, v)
	}
	
	def getAttribute(kind: AttributeKind.Value, iStep: Int): Option[Val] = {
		map.get(kind) match {
			case None => None
			case Some(attr) => attr.get(iStep)
		}
	}
}

class Plate extends MetaObject {
	val rows = createAttribute(AttributeKind.Rows)
	val cols = createAttribute(AttributeKind.Cols)
}
