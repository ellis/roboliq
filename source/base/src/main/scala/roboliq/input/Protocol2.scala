package roboliq.input

import spray.json.JsObject
import spray.json.JsValue
import scala.collection.mutable.ArrayBuffer
import aiplan.strips2.Strips
import scala.collection.mutable.HashMap

case class MyPlate(
	model_? : Option[String],
	location_? : Option[String]
)

class Protocol2 {
	def processData(
		data: JsObject
	): ContextE[(Map[String, String], Strips.State)] = {
		val m = data.fields
		processDataVariables(m)
	}
	
	def processDataVariables(
		m: Map[String, JsValue]
	): ContextE[(Map[String, String], Strips.State)] = {
		val objectToType_m = new HashMap[String, String]
		val atom_l = new ArrayBuffer[Strips.Atom]
		ContextE.context("variable") {
			for {
				variable_m <- ContextE.fromJson[Map[String, JsValue]](m, "variable")
				_ <- ContextE.mapAll(variable_m.toList) { case (name, jsval) =>
					ContextE.context(name) {
						for {
							m <- ContextE.fromJson[Map[String, JsValue]](jsval)
							typ <- ContextE.fromJson[String](m, "type")
							_ <- typ match {
								case "plate" =>
									for {
										plate <- ContextE.fromJson[MyPlate](jsval)
									} yield {
										objectToType_m += (name -> typ)
										atom_l += Strips.Atom("labware", Seq(name))
										plate.model_?.foreach(model => atom_l += Strips.Atom("model", Seq(name, model)))
										plate.location_?.foreach(location => atom_l += Strips.Atom("location", Seq(name, location)))
									}
								case _ =>
									ContextE.unit(())
							}
						} yield ()
					}
				}
			} yield {
				(objectToType_m.toMap, Strips.State(atom_l.toSet))
			}
		}
	}
}