package roboliq.utils0.applicative

import scalaz._
import Scalaz._

class Environment(
	val obj_m: Map[String, String],
	val cmd_m: Map[String, String]
)

object ApplicativeMain extends App {
	
	def getParam(id: String)(implicit env: Environment): Option[String] = env.cmd_m.get(id)
	def getPlate(id: String)(implicit env: Environment): Option[String] = env.obj_m.get(id)
	def getLiquid(id: String)(implicit env: Environment): Option[String] = env.obj_m.get(id)
	
	val env1 = new Environment(
		Map("P1" -> "Plate1", "water" -> "Water"),
		Map("plate" -> "P1", "liquid" -> "water")
	)
	
	def makeit(): (Environment => Unit) = {
		def x(env0: Environment): Unit = {
			implicit val env = env0
			(getParam("plate") |@| getParam("liquid")) { (plateId, liquidId) =>
				(getPlate(plateId) |@| getLiquid(liquidId)) { (plate, liquid) =>
					println(plate, liquid)
				}
			}
		}
		x
	}
	
	makeit()(env1)
}