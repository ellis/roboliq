/*package roboliq.input

import scala.collection.mutable.HashMap
import scala.math.Ordering.Implicits._

class FlatMap {
	/** Stores tuples of (setting name, RjsBasicValue).  The RjsBasicValue should never be an RjsBasicMap. */
	private var nameToValue_m = Map[Vector[String], RjsBasicValue]()

	def getMap = nameToValue_m
	
	/**
	 * If value is an RjsBasicMap, add each of it's keys recursively to this FlatMap.
	 * Otherwise, add the given (name, value) pair to the map
	 */
	def set(name: String, value: RjsBasicValue) {
		val name_l = name.split(".").toVector
		// Remove old values
		nameToValue_m = nameToValue_m.filter(pair =>
			if (name_l == pair._1) false
			else if (pair._1.startsWith(name_l)) false
			else true
		)
		// Add new value
		add(name_l, value)
	}
	
	private def add(prefix: Vector[String], value: RjsBasicValue) {
		value match {
			case m: RjsBasicMap =>
				for ((name, value) <- m.map) {
					add(prefix :+ name, value)
				}
			case _ => nameToValue_m = nameToValue_m + (prefix -> value)
		}
	}
	
	def get(name: String): Option[RjsBasicValue] = {
		val prefix = name.split(".").toVector
		get2(prefix, nameToValue_m)
	}

	private def get2(prefix: Vector[String], nameToValue_m: Map[Vector[String], RjsBasicValue]): Option[RjsBasicValue] = {
		val n = prefix.length
		
		// Get all values with the requested prefix
		val l0 = nameToValue_m.filter(_._1.startsWith(prefix))
		if (l0.isEmpty) {
			None
		}
		else {
			// Find all field names for the requested prefix
			val field_l = l0.keySet.map(_.take(n + 1))
			if (field_l.isEmpty) {
				Some(l0.head._2)
			}
			else {
				Some(getMap(field_l, l0))
			}
		}
	}
	
	private def getMap(field_l: Set[Vector[String]], nameToValue_m: Map[Vector[String], RjsBasicValue]): RjsBasicMap = {
		val map = field_l.toList.map(prefix => {
			nameToValue_m.get(prefix) match {
				case Some(value) => prefix.last -> value
				case None => prefix.last -> get2(prefix, nameToValue_m).get
			}
		}).toMap
		RjsBasicMap(map)
	}

	/*private def get2(prefix: List[String], nameToValue_m: Map[List[String], RjsBasicValue]): Option[RjsBasicMap] = {
		nameToValue_m.map(pair => {
			val (prefix2, value2) = pair
			val prefix3 = prefix ++ List(prefix2.head)
			val value3 = {
				if (prefix2.tail.isEmpty) {
					prefix2.head -> value2
				}
				else {
					prefix2.head -> get2(prefix3, )
				}
			}
		})
		???
	}*/
	
	override def toString: String = {
		nameToValue_m.toList.sortBy(_._1.toList).map(pair => pair._1.mkString(".") + ": " + pair._2).mkString("\n")
	}
}*/