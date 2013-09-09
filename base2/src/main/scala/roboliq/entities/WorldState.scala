package roboliq.entities

import roboliq.core._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

object WorldProperty extends Enumeration {
	val Tip_Model = Value
}

case class WellPosition(
	parent: Labware,
	parentModel: PlateModel,
	index: Int,
	row: Int,
	col: Int
)

case class WorldState(
	//truth: Set[Rel],
	//properties: Map[WorldProperty.Value, Map[List[Object], Object]],
	tip_model_m: Map[Tip, TipModel],
	labware_model_m: Map[Labware, LabwareModel],
	labwareRowCol_well_m: Map[(Labware, RowCol), Well],
	well_labware_m: Map[Well, Labware],
	well_index_m: Map[Well, Int],
	well_isSource_l: Set[Well],
	well_history_m: Map[Well, WellHistory],
	well_aliquot_m: Map[Well, Aliquot]
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
	}*/
	
	def getTipModel(tip: Tip): Option[TipModel] = {
		tip_model_m.get(tip)
	}
	
	def getWell(key: (Labware, RowCol)): RsResult[Well] = {
		labwareRowCol_well_m.get(key).asRs(s"well not found for ${key._1.key}(${key._2})")
	}
	
	def getWellPosition(well: Well): Option[WellPosition] = {
		for {
			labware <- well_labware_m.get(well)
			model <- labware_model_m.get(labware)
			plateModel <- Option(if (model.isInstanceOf[PlateModel]) model.asInstanceOf[PlateModel] else null)
			index <- well_index_m.get(well)
		} yield {
			val row = WellIdentParser.wellRow(plateModel, index)
			val col = WellIdentParser.wellCol(plateModel, index)
			WellPosition(labware, plateModel, index, row, col)
		}
	}
	
	def getWellTipCleanPolicy(well: Well): TipCleanPolicy = {
		well_aliquot_m.get(well).map(_.mixture.tipCleanPolicy).getOrElse(TipCleanPolicy.NN)		
	}
	
	def toMutable = {
		val self = this
		new WorldStateBuilder {
			tip_model_m ++= self.tip_model_m
			labware_model_m ++= self.labware_model_m
			well_labware_m ++= self.well_labware_m
			well_index_m ++= self.well_index_m
			well_isSource_l ++= self.well_isSource_l
			well_history_m ++= self.well_history_m
			well_aliquot_m ++= self.well_aliquot_m
		}
	}
}


class WorldStateBuilder {
	val tip_model_m = new HashMap[Tip, TipModel]
	val labware_model_m = new HashMap[Labware, LabwareModel]
	val labwareRowCol_well_m = new HashMap[(Labware, RowCol), Well]
	val well_labware_m = new HashMap[Well, Labware]
	val well_index_m = new HashMap[Well, Int]
	val well_isSource_l = new HashSet[Well]
	val well_history_m = new HashMap[Well, WellHistory]
	val well_aliquot_m = new HashMap[Well, Aliquot]

	def addWell(well: Well, labware: Labware, rowcol: RowCol, index: Int) {
		labwareRowCol_well_m((labware, rowcol)) = well
		well_labware_m(well) = labware
		well_index_m(well) = index
	}
	
	def getWell(key: (Labware, RowCol)): RsResult[Well] = {
		labwareRowCol_well_m.get(key).asRs(s"well not found for ${key._1.key}(${key._2})")
	}

	def toImmutable = {
		WorldState(
			tip_model_m.toMap,
			labware_model_m.toMap,
			labwareRowCol_well_m.toMap,
			well_labware_m.toMap,
			well_index_m.toMap,
			well_isSource_l.toSet,
			well_history_m.toMap,
			well_aliquot_m.toMap
		)
	}
}
