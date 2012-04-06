package roboliq.core
 
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ArrayBuffer


class ObjBase(bb: BeanBase) {
	private val m_mapIdToPart = new HashMap[String, PartBean]
	private val m_mapTipModel = new HashMap[String, TipModel]
	private val m_mapTip = new HashMap[String, Tip]
	private val m_mapPlateModel = new HashMap[String, PlateModel]
	private val m_mapPlate = new HashMap[String, Plate]
	private val m_mapWell = new HashMap[String, Well]
	
	private val m_mapWellState = new HashMap[String, WellState]
	
	def loadedPlates: Iterable[Plate] = m_mapPlate.values
	def loadedTubes: Iterable[Tube] = m_mapWell.values.collect({case o: Tube => o})
	
	def findTipModel_?(id: String, messages: CmdMessageWriter): Option[TipModel] = {
		find(id, m_mapTipModel, bb.mapTipModel, TipModel.fromBean _, messages, "TipModel")
	}
	
	def findTip_?(id: String, messages: CmdMessageWriter): Option[Tip] = {
		find(id, m_mapTip, bb.mapTip, Tip.fromBean(this, messages), messages, "Tip")
	}
	
	private def find[A, B](
		id: String,
		mapObj: HashMap[String, A],
		mapBean: scala.collection.Map[String, B],
		fnCreate: (B => Result[A]),
		messages: CmdMessageWriter,
		sClass: String
	): Option[A] = {
		mapObj.get(id).orElse {
			mapBean.get(id) match {
				case None =>
					messages.addError(sClass+" with id `"+id+"` not found")
					None
				case Some(bean) =>
					fnCreate(bean) match {
						case Error(ls) =>
							ls.foreach(messages.addError)
							None
						case Success(obj) =>
							mapObj(id) = obj
							Some(obj)
					}
			}
		}
	}
	
	def findTips_?(lId: List[String], messages: CmdMessageWriter): Option[List[Tip]] = {
		val l = lId.map(id => findTip_?(id, messages))
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
	
	/*
	def findWell_?(o: Object, property: String, node: CmdNodeBean, requireId: Boolean = true): Option[Well] = {
		node.getValueNonNull_?()
		if (id == null) {
			if (requireId)
				node.checkPropertyNonNull(null)
			None
		}
		else {
			m_mapWell.get(id) match {
				case Some(obj) => Some(obj)
				case None =>
					createWell(id) match {
						case Error(ls) => ls.foreach(node.addError); None
						case Success(well) => Some(well)
					}
			}
		}
	}
	*/
	
	def findWell_?(id: String, node: CmdNodeBean, requireId: Boolean = true): Option[Well] = {
		if (id == null) {
			if (requireId)
				node.checkPropertyNonNull(null)
			None
		}
		else {
			m_mapWell.get(id) match {
				case Some(obj) => Some(obj)
				case None =>
					createWell(id) match {
						case Error(ls) => ls.foreach(node.addError); None
						case Success(well) => Some(well)
					}
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
				val well = new Tube(idPlate)
				m_mapWell(id) = well
				val wellState = new TubeState(
					obj = well,
					location = null,
					liquid = Liquid.empty,
					nVolume = LiquidVolume.empty,
					bCheckVolume = true,
					history = Nil
				)
				well
			}
			else {
				val well = findPlate(idPlate) match {
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
				val wellState = new PlateWellState(
					conf = well,
					liquid = Liquid.empty,
					nVolume = LiquidVolume.empty,
					bCheckVolume = true,
					history = Nil
				)
				m_mapWell(id) = well
				m_mapWellState(id) = wellState
				well
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
