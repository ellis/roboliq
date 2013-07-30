package roboliq.entities

import roboliq.core._

object WorldProperty extends Enumeration {
	val Tip_Model = Value
}

case class WellPosition(
	parent: Labware,
	row: Int,
	col: Int
)

class WorldState(
	//truth: Set[Rel],
	//properties: Map[WorldProperty.Value, Map[List[Object], Object]],
	tip_model_m: Map[Tip, TipModel],
	well_labware_m: Map[Vessel, Labware],
	well_index_m: Map[Vessel, Int],
	well_isSource_l: Set[Vessel]
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
	
	/*def getWellPosition(well: Vessel): Option[WellPosition] = {
		well_position_m.get(well)
	}*/
}