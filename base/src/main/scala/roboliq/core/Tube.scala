package roboliq.core

import scala.collection
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.reflect.BeanProperty


class Tube(
	val id: String,
	val model: PlateModel
) extends Well {
	val idPlate = id
	val index = 0
	val iRow = 0
	val iCol = 0
	val indexName = ""
}

object Tube {
	def fromBean(ob: ObjBase)(bean: PlateBean): Result[Tube] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
			idModel <- Result.mustBeSet(bean.model, "model")
			model <- ob.findPlateModel(idModel)
		} yield {
			new Tube(id, model)
		}
	}
}