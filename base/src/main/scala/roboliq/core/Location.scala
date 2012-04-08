package roboliq.core

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty


class LocationBean extends Bean {
	@BeanProperty var plateModels: java.util.List[String] = null
	@BeanProperty var cooled: java.lang.Boolean = null
}

case class Location(
	val id: String,
	val plateModels: List[PlateModel],
	val cooled: Boolean
)

object Location {
	def fromBean(ob: ObjBase)(bean: LocationBean): Result[Location] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
			plateModels <- Result.mustBeSet(bean.plateModels, "model")
			lModel <- Result.mapOver(plateModels.toList)(ob.findPlateModel)
		} yield {
			val cooled: Boolean = if (bean.cooled != null) bean.cooled else false
			new Location(id, lModel, cooled)
		}
	}
}