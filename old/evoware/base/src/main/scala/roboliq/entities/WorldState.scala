package roboliq.entities

import roboliq.core._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import spray.json.JsValue

object WorldProperty extends Enumeration {
	val Tip_Model = Value
}

case class WellPosition(
	parent: Labware,
	parentModel: PlateModel,
	index: Int,
	row: Int,
	col: Int
) {
	def toString(eb: EntityBase): String = {
		val labwareName = eb.getIdent(parent).getOrElse("ERROR")
		val wellName = WellIdentParser.wellIndexName(parentModel.rows, parentModel.cols, row, col)
		s"$labwareName($wellName)"
	}
}

case class WorldState(
	//truth: Set[Rel],
	//properties: Map[WorldProperty.Value, Map[List[Object], Object]],
	//tip_model_m: Map[Tip, TipModel],
	value_m: Map[List[String], JsValue],
	tip_state_m: Map[Tip, TipState],
	labware_model_m: Map[Labware, LabwareModel],
	// Labware can either be on a site or another piece of labware
	labware_location_m: Map[Labware, Entity],
	labware_isSealed_l: Set[Labware],
	labwareRowCol_well_m: Map[(Labware, RowCol), Well],
	well_labware_m: Map[Well, Labware],
	well_index_m: Map[Well, Int],
	well_rowcol_m: Map[Well, RowCol],
	well_isSource_l: Set[Well],
	well_history_m: Map[Well, WellHistory],
	well_aliquot_m: Map[Well, Aliquot],
	device_isOpen_l: Set[Device]
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
		tip_state_m.get(tip).flatMap(_.model_?)
	}
	
	def getWell(key: (Labware, RowCol)): RsResult[Well] = {
		labwareRowCol_well_m.get(key).asRs(s"well not found for ${key._1.key}(${key._2})")
	}
	
	def getLabwareModel(labware: Labware): RsResult[LabwareModel] = labware_model_m.get(labware).asRs(s"missing model for labware $labware")
	def getWellLabware(well: Well): RsResult[Labware] = well_labware_m.get(well).asRs(s"missing labware for well $well")
	
	def getWellPosition(well: Well): RsResult[WellPosition] = {
		for {
			labware <- getWellLabware(well)
			model <- getLabwareModel(labware)
			plateModel <- RsResult.asInstanceOf[PlateModel](model)
			index <- well_index_m.get(well).asRs(s"missing index for well $well")
		} yield {
			val row = WellIdentParser.wellRow(plateModel, index)
			val col = WellIdentParser.wellCol(plateModel, index)
			WellPosition(labware, plateModel, index, row, col)
		}
	}
	
	def getWellTipCleanPolicy(well: Well): TipCleanPolicy = {
		well_aliquot_m.get(well).map(_.mixture.tipCleanPolicy).getOrElse(TipCleanPolicy.NN)		
	}
	
	def getTipState(tip: Tip): TipState = {
		tip_state_m.getOrElse(tip, TipState.createEmpty(tip))
	}
	
	def toMutable = {
		val self = this
		new WorldStateBuilder {
			//tip_model_m ++= self.tip_model_m
			value_m ++= self.value_m
			tip_state_m ++= self.tip_state_m
			labware_model_m ++= self.labware_model_m
			labware_location_m ++= self.labware_location_m
			labware_isSealed_l ++= self.labware_isSealed_l
			labwareRowCol_well_m ++= self.labwareRowCol_well_m
			well_labware_m ++= self.well_labware_m
			well_index_m ++= self.well_index_m
			well_rowcol_m ++= self.well_rowcol_m
			well_isSource_l ++= self.well_isSource_l
			well_history_m ++= self.well_history_m
			well_aliquot_m ++= self.well_aliquot_m
		}
	}
}


class WorldStateBuilder {
	//val tip_model_m = new HashMap[Tip, TipModel]
	val value_m = new HashMap[List[String], JsValue]
	val tip_state_m = new HashMap[Tip, TipState]
	val labware_model_m = new HashMap[Labware, LabwareModel]
	val labware_location_m = new HashMap[Labware, Entity] 
	val labware_isSealed_l = new HashSet[Labware]
	val labwareRowCol_well_m = new HashMap[(Labware, RowCol), Well]
	val well_labware_m = new HashMap[Well, Labware]
	val well_index_m = new HashMap[Well, Int]
	val well_rowcol_m = new HashMap[Well, RowCol]
	val well_isSource_l = new HashSet[Well]
	val well_history_m = new HashMap[Well, WellHistory]
	val well_aliquot_m = new HashMap[Well, Aliquot]
	val device_isOpen_l = new HashSet[Device]

	def addWell(well: Well, labware: Labware, rowcol: RowCol, index: Int) {
		labwareRowCol_well_m((labware, rowcol)) = well
		well_labware_m(well) = labware
		well_index_m(well) = index
		well_rowcol_m(well) = rowcol
	}
	
	def getWell(key: (Labware, RowCol)): RsResult[Well] = {
		labwareRowCol_well_m.get(key).asRs(s"well not found for ${key._1.key}(${key._2})")
	}
	
	def getTipState(tip: Tip): TipState = {
		tip_state_m.get(tip) match {
			case Some(tipState) => tipState
			case None =>
				val tipState = TipState.createEmpty(tip)
				tip_state_m(tip) = tipState
				tipState
		}
	}

	def toImmutable = {
		WorldState(
			value_m.toMap,
			//tip_model_m.toMap,
			tip_state_m.toMap,
			labware_model_m.toMap,
			labware_location_m.toMap,
			labware_isSealed_l.toSet,
			labwareRowCol_well_m.toMap,
			well_labware_m.toMap,
			well_index_m.toMap,
			well_rowcol_m.toMap,
			well_isSource_l.toSet,
			well_history_m.toMap,
			well_aliquot_m.toMap,
			device_isOpen_l.toSet
		)
	}
}
