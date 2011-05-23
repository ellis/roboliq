package meta

import scala.collection.mutable


// Objects have kind and attributes with history
// AttributeInstances have a typed value, source, time index at which the value was set
// Attributes have a collection of AttributeInstances

/*
object AttrSource extends Enumeration {
	//type AttrSource = Value
	val Undetermined, Settings, User, Computer = Value
}

object AttrKind extends Enumeration {
	//type AttrKind = Value
	val Parent, Index, Rows, Cols, Volume, IsPipettable, IsCooled, RequiresCooling = Value
}


class AttrInstance[T](val value: T, val iStep: Int, val source: AttrSource.Value)

class Attr[T](kind: AttrKind.Value) {
	private val map = new mutable.HashMap[Int, AttrInstance[T]]
	
	def getInstance(iStep: Int): Option[AttrInstance[T]] = {
		var pair0: Tuple2[Int, AttrInstance[T]] = null
		map.foreach { pair =>
			if (pair._1 <= iStep) {
				if (pair0 == null || pair._1 > pair0._1)
					pair0 = pair
			}
		}
		if (pair0 == null)
			None
		else
			Some(pair0._2)
	}
	
	def put(iStep: Int, inst: AttrInstance[T]) {
		map.put(iStep, inst)
	}
}

class MetaObject {
	val parent = new Attr[MetaObject](AttrKind.Parent)
}
*/