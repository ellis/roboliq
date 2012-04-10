package roboliq.core
 
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ArrayBuffer


class ObjBase(bb: BeanBase) {
	private val m_mapIdToPart = new HashMap[String, PartBean]
	
	private val m_mapTipModel = new HashMap[String, TipModel]
	private val m_mapPlateModel = new HashMap[String, PlateModel]
	private val m_mapTubeModel = new HashMap[String, TubeModel]
	private val m_mapTip = new HashMap[String, Tip]
	private val m_mapLocation = new HashMap[String, PlateLocation]
	private val m_mapTubeLocation = new HashMap[String, TubeLocation]

	private val m_mapSubstance = new HashMap[String, Substance]
	private val m_mapPlate = new HashMap[String, Plate]
	private val m_mapTube = new HashMap[String, Tube]
	private val m_mapWell = new HashMap[String, PlateWell]
	private val m_mapLiquid = new HashMap[String, Liquid]
	
	val m_mapWell2 = new HashMap[String, Well2]
	
	//private val m_mapWellState = new HashMap[String, WellState]
	//private val m_mapState = new HashMap[String, Object]
	
	private var builder: StateBuilder = null
	//def states: scala.collection.Map[String, Object] = m_mapState

	def getBuilder = builder
	def loadedPlates: Iterable[Plate] = m_mapPlate.values
	def loadedTubes: Iterable[Tube] = m_mapWell.values.collect({case o: Tube => o})
	
	def findAllTipModels(): Result[List[TipModel]] = {
		val l = bb.mapTipModel.keys.toList.map(findTipModel)
		if (l.forall(_.isSuccess))
			Result.sequence(l).map(_.toList)
		else
			Error(l.collect({case Error(ls) => ls}).flatten)
	}
	
	def findAllTips(): Result[List[Tip]] = {
		val l = bb.mapTip.keys.toList.map(findTip)
		if (l.forall(_.isSuccess))
			Result.sequence(l).map(_.toList)
		else
			Error(l.collect({case Error(ls) => ls}).flatten)
	}
	
	def findAllPlateLocations(): Result[List[PlateLocation]] = {
		val l = bb.mapLocation.keys.toList.map(findPlateLocation)
		if (l.forall(_.isSuccess))
			Result.sequence(l).map(_.toList)
		else
			Error(l.collect({case Error(ls) => ls}).flatten)
	}
	
	def findAllTubeLocations(): Result[List[TubeLocation]] = {
		val l = bb.mapTubeLocation.keys.toList.map(findTubeLocation)
		println("findAllTubeLocations: "+l)
		if (l.forall(_.isSuccess))
			Result.sequence(l).map(_.toList)
		else
			Error(l.collect({case Error(ls) => ls}).flatten)
	}
	
	def findTipModel(id: String): Result[TipModel] = {
		find(id, m_mapTipModel, bb.mapTipModel, TipModel.fromBean _, "TipModel")
	}
	
	def findTipModel_?(id: String, messages: CmdMessageWriter): Option[TipModel] = {
		find(id, m_mapTipModel, bb.mapTipModel, TipModel.fromBean _, messages, "TipModel")
	}
	
	def findPlateModel(id: String): Result[PlateModel] = {
		find(id, m_mapPlateModel, bb.mapPlateModel, PlateModel.fromBean _, "PlateModel")
	}
	
	def findTubeModel(id: String): Result[TubeModel] = {
		find(id, m_mapTubeModel, bb.mapTubeModel, TubeModel.fromBean _, "TubeModel")
	}
	
	def findTip(id: String): Result[Tip] = {
		find(id, m_mapTip, bb.mapTip, Tip.fromBean(this), "Tip")
	}
	
	def findTip_?(id: String, messages: CmdMessageWriter): Option[Tip] = {
		find(id, m_mapTip, bb.mapTip, Tip.fromBean(this, messages), messages, "Tip")
	}
	
	def findPlateLocation(id: String): Result[PlateLocation] = {
		find(id, m_mapLocation, bb.mapLocation, PlateLocation.fromBean(this), "PlateLocation")
	}
	
	def findTubeLocation(id: String): Result[TubeLocation] = {
		find(id, m_mapTubeLocation, bb.mapTubeLocation, TubeLocation.fromBean(this), "TubeLocation")
	}
	
	def findPlate(id: String): Result[Plate] = {
		find(id, m_mapPlate, bb.mapPlate, Plate.fromBean(this), "Plate")
	}
	
	def findTube(id: String): Result[Tube] = {
		find(id, m_mapTube, bb.mapTube, Tube.fromBean(this), "Tube")
	}
	
	def findPlateOrTube(id: String): Result[Object] = {
		findPlate(id).orElse(findTube(id))
	}
	
	def findSubstance(id: String): Result[Substance] = {
		find(id, m_mapSubstance, bb.mapSubstance, Substance.fromBean _, "Substance")
	}
	
	def findLiquid(id: String): Result[Liquid] = {
		m_mapLiquid.get(id) match {
			case Some(liquid) => Success(liquid)
			case None =>
				findSubstance(id) match {
					case Error(ls) => Error(ls)
					case Success(substance: SubstanceLiquid) =>
						Success(new Liquid(
							sName = id,
							sFamily = substance.physicalProperties.toString,
							contaminants = Set(),
							group = new LiquidGroup(substance.cleanPolicy),
							multipipetteThreshold = if (substance.allowMultipipette) 0 else 1000
						))
						
					case _ => Error("substance `"+id+"` is not a liquid")
				}
		}
	}
	
	private def find[A, B](
		id: String,
		mapObj: HashMap[String, A],
		mapBean: scala.collection.Map[String, B],
		fnCreate: (B => Result[A]),
		sClass: String
	): Result[A] = {
		mapObj.get(id) match {
			case Some(obj) => Success(obj)
			case None =>
				mapBean.get(id) match {
					case None =>
						Error(sClass+" with id `"+id+"` not found")
					case Some(bean) =>
						fnCreate(bean) match {
							case Error(ls) => Error(ls)
							case Success(obj) =>
								mapObj(id) = obj
								Success(obj)
						}
				}
		}
	}
	
	private def find[A, B](
		id: String,
		mapObj: HashMap[String, A],
		mapBean: scala.collection.Map[String, B],
		fnCreate: (B => Result[A]),
		messages: CmdMessageWriter,
		sClass: String
	): Option[A] = {
		find(id, mapObj, mapBean, fnCreate, sClass) match {
			case Success(obj) => Some(obj)
			case Error(ls) => ls.foreach(messages.addError); None
		}
	}
	
	def findTips_?(lId: List[String], messages: CmdMessageWriter): Option[List[Tip]] = {
		val l = lId.map(id => findTip_?(id, messages))
		if (l.exists(_.isEmpty))
			None
		else
			Some(l.flatten)
	}
	
	def findWell(id: String): Result[PlateWell] = {
		m_mapWell.get(id) match {
			case Some(obj) => Success(obj)
			case None => createWell(id)
		}
	}
	
	def findWell_?(id: String, node: CmdNodeBean, requireId: Boolean = true): Option[PlateWell] = {
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
	
	// REFACTOR: remove this and fix any code which breaks -- ellis, 2012-04-10
	def findWells(name: String): Result[List[PlateWell]] = {
		WellSpecParser.parseToIds(name, this).flatMap(findWells)
	}

	// REFACTOR: remove this and fix any code which breaks -- ellis, 2012-04-10
	def findWells(lId: List[String]): Result[List[PlateWell]] = {
		Result.mapOver(lId)(findWell)
	}
	
	// REFACTOR: remove this and fix any code which breaks -- ellis, 2012-04-10
	def findWells_?(name: String, node: CmdNodeBean, requireId: Boolean = true): Option[List[PlateWell]] = {
		if (name == null) {
			if (requireId)
				node.checkPropertyNonNull(null)
			None
		}
		else {
			findWells(name) match {
				case Error(ls) => ls.foreach(node.addError); None
				case Success(l) => Some(l)
			}
		}
	}
	
	private def createWell(id: String): Result[PlateWell] = {
		for {
			lPlateWellSpec <- WellSpecParser.parse(id)
			_ <- Result.assert(lPlateWellSpec.length == 1, "must provide a simple well ID instead of `"+id+"`")
			_ <- Result.assert(lPlateWellSpec.head._2.isInstanceOf[WellSpecOne], "must provide a simple well ID instead of `"+id+"`")
			val (idPlate, wellSpec: WellSpecOne) = lPlateWellSpec.head
			plate <- findPlate(idPlate)
		} yield {
			val index = wellSpec.rc.col * plate.model.nRows
			val well = new PlateWell(
				id = id,
				idPlate = idPlate,
				index = index,
				iRow = wellSpec.rc.row,
				iCol = wellSpec.rc.col,
				indexName = wellSpec.rc.toString
			)
			m_mapWell(id) = well
			m_mapWell2(id) = well
			well
		}
		/*
		for {
			res <- Printer.parseWellId(id)
		} yield {
			val (idPlate, indexName, iRow, iCol) = res
			if (indexName == "") {
				val well = findTube(idPlate) match {
					case Error(ls) => return Error(ls)
					case Success(tube) => tube
				}
				m_mapWell(id) = well
				well
			}
			else {
				val well = findPlate(idPlate) match {
					case Error(ls) => return Error(ls)
					case Success(plate) =>
				well
			}
		}
		*/
	}
	
	def findWell2(id: String): Result[Well2] = {
		m_mapWell2.get(id) match {
			case None => Error("Well information not available for id `"+id+"`")
			case Some(well2) => Success(well2)
		}
	}
	
	/*
	private def loadWellEvents(id: String) {
		//println("loadWellEvents: "+id)
		bb.mapEvents.get(id) match {
			case None =>
			case Some(history) =>
				//println("history: "+history.toList)
				history.foreach(item => {
					item.asInstanceOf[EventBean].update(builder)
				})
		}
	}
	*/
	
	def findSystemString_?(id: String, node: CmdNodeBean): Option[String] = {
		bb.mapSystemProperties.get(id) match {
			case None =>
				node.addError("systemProperties", id, "must be set")
				None
			case Some(o) =>
				Some(o.asInstanceOf[String])
		}
	}
	
	def findTipState(id: String): Result[TipState] = {
		builder.map.get(id) match {
			case Some(state: TipState) => Success(state)
			case Some(_) => Error("id `"+id+"`: stored state is not a TipState")
			case None =>
				findTip(id) match {
					case Error(ls) => Error(ls)
					case Success(tip) =>
						val tipState = TipState.createEmpty(tip)
						builder.map(id) = tipState
						Success(tipState)
				}
		}
	}
	
	def findWellState(id: String): Result[WellState] = {
		//println("ObjBase.findWellState: "+id)
		builder.map.get(id) match {
			case Some(state: WellState) => Success(state)
			case Some(_) => Error("id `"+id+"`: stored state is not a WellState")
			case None =>
				val wellState = findWell(id) match {
					case Success(well) =>
						new PlateWellState(
							conf = well,
							content = VesselContent.createEmpty(id),
							bCheckVolume = true,
							history = Nil
						)
					case Error(ls) => findTube(id) match {
						case Error(ls) => return Error(ls)
						case Success(tube) =>
							new TubeState(
								obj = tube,
								idPlate = null,
								row = -1,
								col = -1,
								content = VesselContent.createEmpty(id),
								bCheckVolume = true,
								history = Nil
							)
					}
				}
				builder.map(id) = wellState
				Success(wellState)
		}
	}
	
	def reconstructHistory(): Result[Unit] = {
		builder = new StateBuilder(this)
		for (event <- bb.lEvent) {
			findWellState(event.obj) match {
				case Error(ls) => println("reconstructHistory: unhandled: "+event)
				case Success(wellState) =>
					event.update(builder)
			}
		}
		Success()
	}
	
	def findAllIdsContainingSubstance(substance: Substance): Result[List[String]] = {
		def hasSubstance(st: WellState): Boolean = {
			substance match {
				case liquid: SubstanceLiquid => st.content.mapSolventToVolume.contains(liquid)
				case _ => st.content.mapSoluteToMol.contains(substance)
			}
		}
		
		// Get list of 
		val l = builder.map.values.collect({case wellState: WellState if hasSubstance(wellState) => {
			wellState match {
				case st: PlateWellState => st.conf.id
				case st: TubeState => st.obj.id
			}
		}}).toList
		
		Success(l)
	}
	
	def setInitialTubeLocation(tube: Tube, location: String, row: Int, col: Int) = {
		TubeLocationEventBean(tube, location, row, col).update(builder)
		//println("stateA: "+ob.findWellState(tube.id))
		m_mapWell2(tube.id) = Well2.forTube(findWellState(tube.id).get.asInstanceOf[TubeState], builder).get
	}

	/*def loadState(id: String): Option[Object] = {
		m_mapState.get(id).orElse {
			if (findWell(id).isSuccess) {
				
			}
			else if ()
			m_mapWell.get(id) match {
				case None =>
				case Some(well) =>
			}
			
			None
		}
	}*/
}