package roboliq.input

import spray.json.JsObject
import spray.json.JsString
import roboliq.entities.Entity
import scala.collection.mutable.ArrayBuffer

class PlateBean {
	var name: String = null
	var model: String = null
}

case class PlateInput(
	name: Option[String],
	model: Option[String]
)

case class TubeInput(
	name: Option[String],
	model: Option[String],
	contents: Option[String]
)

class InputMain {
	val plateInputs = List[PlateInput](
		PlateInput(name = Some("plate1"), model = Some("Thermocycler Plate"))
	)
	val tubeInputs = List[TubeInput](
		TubeInput(name = Some("tube1"), model = Some("Small Tube"), contents = Some("water"))
	)
	val protocol = List[JsObject](
		JsObject(
			"command" -> JsString("shake"),
			"object" -> JsString("plate1")
		), 
		JsObject(
			"command" -> JsString("seak"),
			"object" -> JsString("plate1")
		)
	)
	
	var entities = new ArrayBuffer[(String, Entity)]
}