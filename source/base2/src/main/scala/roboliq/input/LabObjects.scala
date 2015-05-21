package roboliq.input

import scala.language.dynamics
import scala.collection.mutable.HashMap

trait LabObject {
	val `type`: String
}

case class PlateObject() extends LabObject {
	val `type` = "Plate"
}

class PlateModelObject extends LabObject {
	val `type` = "PlateModel"
}

class SiteObject extends LabObject {
	val `type` = "Site"
}

class SiteModelObject extends LabObject {
	val `type` = "SiteModel"
}

class LabObjects extends Dynamic {
	var nameToValue_l = Vector[(String, RjsBasicValue)]()
	
	def set(name: String, value: RjsBasicValue) {
		// Remove old values
		val nameWithPeriod = name + "."
		nameToValue_l = nameToValue_l.filter(pair =>
			if (name == pair._1) false
			else if (pair._1.startsWith(nameWithPeriod)) false
			else true
		)
		// Add new value
		add(name, value)
	}
	
	private def add(prefix: String, value: RjsBasicValue) {
		value match {
			case m: RjsBasicMap =>
				val prefix1 = prefix + "."
				for ((name, value) <- m.map) {
					add(prefix1+name, value)
				}
			case _ => nameToValue_l = nameToValue_l :+ (prefix, value)
		}
	}
	
	def get(name: String): Option[RjsBasicValue] = {
		val nameWithPeriod = name + "."
		val l = nameToValue_l.filter(pair =>
			if (name == pair._1) true
			else if (pair._1.startsWith(nameWithPeriod)) true
			else false
		)
		???
	}
}