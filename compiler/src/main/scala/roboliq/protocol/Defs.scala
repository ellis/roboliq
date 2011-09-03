package roboliq.protocol

import scala.collection.mutable.ArrayBuffer

import roboliq.common._


class Volume(n: Double) {
	def ul = n
}


class FixedPlate(model: PlateModel, val location: String)
class FixedCarrier(val location: String)
