package roboliq.core

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty


class PlateLocationBean extends Bean {
	@BeanProperty var plateModels: java.util.List[String] = null
	@BeanProperty var cooled: java.lang.Boolean = null
}

class TubeLocationBean extends Bean {
	@BeanProperty var tubeModels: java.util.List[String] = null
	@BeanProperty var rackModel: String = null
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
	val rackModel: PlateModel,
	val tubeModels: List[TubeModel],
	val cooled: Boolean
) extends Location

object TubeLocation {
	def fromBean(ob: ObjBase)(bean: TubeLocationBean): Result[TubeLocation] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
			_ <- Result.mustBeSet(bean.rackModel, "rackModel")
			tubeModels <- Result.mustBeSet(bean.tubeModels, "model")
			lTubeModel <- Result.mapOver(tubeModels.toList)(ob.findTubeModel)
			rackModel <- ob.findPlateModel(bean.rackModel)
		} yield {
			val cooled: Boolean = if (bean.cooled != null) bean.cooled else false
			new TubeLocation(id, rackModel, lTubeModel, cooled)
		}
	}
}