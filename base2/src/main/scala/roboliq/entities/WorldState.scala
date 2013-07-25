package roboliq.entities

import roboliq.core._

object WorldProperty extends Enumeration {
	val Tip_Model = Value
}

class WorldState(
	//truth: Set[Rel],
	//properties: Map[WorldProperty.Value, Map[List[Object], Object]],
	tip_model_m: Map[Tip, TipModel]
) {
	/*def getProperty[A : Manifest](property: WorldProperty.Value, args: List[Object]): RsResult[A] = {
		properties.get(property) match {
			case Some(m) =>
				m.get(args) match {
					case Some(o) =>
						if (o.isInstanceOf[A]) {
							RsSuccess(o.asInstanceOf[A])
						}
						else {
							RsError(s"wrong type")
						}
					case None =>
						RsError("not found")
				}
			case None =>
				RsError("not found")
		}
	}
	
	def getTipModel(tip: Tip): RsResult[TipModel] = {
		getProperty[TipModel](WorldProperty.Tip_Model, List(tip))
	}*/
	
	def getTipModel(tip: Tip): Option[TipModel] = {
		tip_model_m.get(tip)
	}
}