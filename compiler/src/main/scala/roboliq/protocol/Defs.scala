package roboliq.protocol

import scala.collection.mutable.ArrayBuffer

import roboliq.common._

	
object LiquidProperties extends Enumeration {
	val Water, Glycerol = Value
}

class Volume(n: Double) {
	def ul = n
	def ml = n * 1000
}


//class FixedPlate(model: PlateModel, val location: String)
//class FixedCarrier(val location: String)
