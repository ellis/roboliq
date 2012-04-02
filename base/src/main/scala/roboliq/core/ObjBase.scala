package roboliq.core

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ArrayBuffer


class ObjBase(bb: BeanBase) {
	private val m_mapTipModel = new HashMap[String, TipModel]
	private val m_mapTip = new HashMap[String, Tip]
	private val m_mapPlateModel = new HashMap[String, PlateModel]
	private val m_mapPlate = new HashMap[String, Plate]
	private val m_mapWell = new HashMap[String, Well]
	//private val m_mapSubstance = new HashMap[String, SubstanceItem]
	
	//def mapPlateModel: scala.collection.Map[String, PlateModel] = m_mapPlateModel
	//def mapPlate: scala.collection.Map[String, Plate] = m_mapPlate
	//def mapSubstance: scala.collection.Map[String, SubstanceItem] = m_mapSubstance
	
	
	def findTip_?(id: String, node: CmdNodeBean): Option[Tip] = {
		m_mapTip.get(id) match {
			case Some(obj) => Some(obj)
			case None => node.addError("tip `"+id+"` not found"); None
		}
	}
	
	def findTips_?(lId: List[String], node: CmdNodeBean): Option[List[Tip]] = {
		val l = lId.map(id => findTip_?(id, node))
		if (l.exists(_.isEmpty))
			None
		else
			Some(l.flatten)
	}


	def findPlateModel(id: String): Result[PlateModel] = {
		m_mapPlateModel.get(id) match {
			case Some(obj) => Success(obj)
			case None => createPlateModel(id)
		}
	}
	
	private def createPlateModel(id: String): Result[PlateModel] = {
		for {
			bean <- Result.get(bb.mapPlateModel.get(id), "plate model \""+id+"\" not found")
			obj <- PlateModel.fromBean(bean)
		}
		yield {
			m_mapPlateModel(id) = obj
			obj
		}
	}
	
	def findPlate(id: String): Result[Plate] = {
		m_mapPlate.get(id) match {
			case Some(obj) => Success(obj)
			case None => createPlate(id)
		}
	}
	
	private def createPlate(id: String): Result[Plate] = {
		for {
			bean <- Result.get(bb.mapPlate.get(id), "plate \""+id+"\" not found")
			idModel <- Result.mustBeSet(bean.model, "model")
			model <- findPlateModel(idModel)
		}
		yield {
			val obj = new Plate(id, model)
			m_mapPlate(id) = obj
			obj
		}
	}

	//def findSubstance(id: String): Result[]
	
	private def createSubstance(id: String): Unit = {
		
	}
	
	def findWell_?(id: String, node: CmdNodeBean): Option[Well] = {
		m_mapWell.get(id) match {
			case Some(obj) => Some(obj)
			case None =>
				createWell(id) match {
					case Error(ls) => ls.foreach(node.addError); None
					case Success(well) => Some(well)
				}
		}
	}
	
	def findWell(id: String): Result[Well] = {
		m_mapWell.get(id) match {
			case Some(obj) => Success(obj)
			case None => createWell(id)
		}
	}
	
	def findWells(lId: Seq[String]): Result[Seq[Well]] = {
		Result.mapOver(lId)(findWell)
	}
	
	private def createWell(id: String): Result[Well] = {
		for {
			res <- Printer.parseWellId(id)
		} yield {
			val (idPlate, indexName, iRow, iCol) = res
			if (indexName == "") {
				new Tube(idPlate)
			}
			else {
				findPlate(idPlate) match {
					case Error(ls) => return Error(ls)
					case Success(plate) =>
						val index = iCol * plate.model.nRows
						new PlateWell(
							id = id,
							idPlate = idPlate,
							index = index,
							iRow = iRow,
							iCol = iCol,
							indexName = indexName
						)
				}
			}
		}
	}
	
	def findSystemString_?(id: String, node: CmdNodeBean): Option[String] = {
		bb.mapSystemProperties.get(id) match {
			case None =>
				node.addError("systemProperties", id, "must be set")
				None
			case Some(o) =>
				Some(o.asInstanceOf[String])
		}
	}
	
}
/*
object ObjBase {
	def fromBeanBase(bb: BeanBase, ids: Set[String]): Result[ObjBase] = {
		val ob = new ObjBase(bb)
		
		val lsError = new ArrayBuffer[String]
		
		val lsPlate = new HashSet[String]
		val lsPlateModel = new HashSet[String]
		val lsWell = new HashSet[String]
		
		lsWell ++= ids.filter(_.contains('('))
		
		lsPlate ++= ids.filter(bb.mapPlate.contains)
		// Find all plates referenced by wells
		lsPlate ++= lsWell.map(s => s.take(s.indexOf("(")))
		
		lsPlateModel ++= ids.filter(bb.mapPlateModel.contains)
		// Find all plate models referenced by plates
		lsPlateModel ++= lsPlate.collect({ case idPlate if bb.mapPlate(idPlate).model != null => bb.mapPlate(idPlate).model })
		
		// Construct PlateModel objects
		for (id <- lsPlateModel) {
			val b = bb.mapPlateModel(id)
			PlateModel.fromBean(b) match {
				case Success(obj) => ob.m_mapPlateModel(id) = obj
				case Error(ls) => lsError ++= ls
			}
		}

		// Construct Plate objects
		for (id <- lsPlate) {
			val b = bb.mapPlate(id)
			Plate.fromBean(b, ob.mapPlateModel) match {
				case Success(obj) => ob.m_mapPlate(id) = obj
				case Error(ls) => lsError ++= ls
			}
		}

		// Construct Well objects
		for (id <- lsPlate) {
			val plate = ob.mapPlate(id)
			for (iWell <- 0 to plate.nWells) {
				val sWell = PlateModel.wellIndexName(plate.model.nRows, plate.model.nCols, iWell)
				val idWell = plate.id+"("+sWell+")"
				bb.
			}
			val idWell
			Plate.fromBean(b, ob.mapPlateModel) match {
				case Success(obj) => ob.m_mapPlate(id) = obj
				case Error(ls) => lsError ++= ls
			}
		}
		for ((id, plate) <- bb.mapPlate if ids.contains(id)) {
			ob.m_mapPlate
		}
		
		Success(new ObjBase)
	}
}
*/
