package roboliq.core

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty


class PlateLocationBean extends Bean {
	@BeanProperty var plateModels: java.util.List[String] = null
	@BeanProperty var cooled: java.lang.Boolean = null
}

class TubeLocationBean extends Bean {
	@BeanProperty var tubeModels: java.util.List[String] = null
	@BeanProperty var rows: java.lang.Integer = null
	@BeanProperty var cols: java.lang.Integer = null
	@BeanProperty var cooled: java.lang.Boolean = null
}

sealed abstract class Location

case class PlateLocation(
	val id: String,
	val plateModels: List[PlateModel],
	val cooled: Boolean
) extends Location

object PlateLocation {
	def fromBean(ob: ObjBase)(bean: PlateLocationBean): Result[PlateLocation] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
			plateModels <- Result.mustBeSet(bean.plateModels, "model")
			lModel <- Result.mapOver(plateModels.toList)(ob.findPlateModel)
		} yield {
			val cooled: Boolean = if (bean.cooled != null) bean.cooled else false
			new PlateLocation(id, lModel, cooled)
		}
	}
}

case class TubeLocation(
	val id: String,
	val tubeModels: List[TubeModel],
	val rows: Int,
	val cols: Int,
	val cooled: Boolean
) extends Location

object TubeLocation {
	def fromBean(ob: ObjBase)(bean: TubeLocationBean): Result[TubeLocation] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
			tubeModels <- Result.mustBeSet(bean.tubeModels, "model")
			lModel <- Result.mapOver(tubeModels.toList)(ob.findTubeModel)
		} yield {
			val rows: Int = if (bean.rows != null) bean.rows else 1
			val cols: Int = if (bean.rows != null) bean.rows else 1
			val cooled: Boolean = if (bean.cooled != null) bean.cooled else false
			new TubeLocation(id, lModel, rows, cols, cooled)
		}
	}
}