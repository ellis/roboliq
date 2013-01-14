package roboliq.utils0.applicative

import scalaz._
import Scalaz._

object ApplicativeMain extends App {
	val obj_m = Map("P1" -> "Plate1", "water" -> "Water")
	def cmd_m = Map("plate" -> "P1", "liquid" -> "water")
	
	def getParam(id: String): Option[String] = cmd_m.get(id)
	def getPlate(id: String): Option[String] = obj_m.get(id)
	def getLiquid(id: String): Option[String] = obj_m.get(id)
	
	(getParam("plate") |@| getParam("liquid")) { (plateId, liquidId) =>
		(getPlate(plateId) |@| getLiquid(liquidId)) { (plate, liquid) =>
			println(plate, liquid)
		}
	}
}