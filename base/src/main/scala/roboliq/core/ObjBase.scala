package roboliq.core
 
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ArrayBuffer


/**
 * A database of protocol objects.
 * 
 * @param bb database of JavaBean objects loaded from YAML source. 
 */
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
	private val m_mapPlateWell = new HashMap[String, PlateWell]
	private val m_mapLiquid = new HashMap[String, Liquid]
	
	val m_mapSubstanceToVessels = new HashMap[String, List[String]]
	val m_mapWell2 = new HashMap[String, Well]
	
	private var builder: StateBuilder = null

	/**
	 * State builder for the initial state of objects in the database.
	 * @see reconstructHistory()
	 */
	def getBuilder = builder
	/** List of plates which have been loaded from the database. */
	def loadedPlates: Iterable[Plate] = m_mapPlate.values
	/** List of tubes which have been loaded from the database. */
	def loadedTubes: Iterable[Tube] = m_mapTube.values
	/** List of wells on plates which have been loaded from the database. */
	def loadedPlateWells: Iterable[PlateWell] = m_mapPlateWell.values 
	
	/** List of tip models in the database. */
	def findAllTipModels(): Result[List[TipModel]] = {
		val l = bb.mapTipModel.keys.toList.map(findTipModel)
		if (l.forall(_.isSuccess))
			Result.sequence(l).map(_.toList)
		else
			Error(l.collect({case Error(ls) => ls}).flatten)
	}
	
	/** List of tips in the database. */
	def findAllTips(): Result[List[Tip]] = {
		val l = bb.mapTip.keys.toList.map(findTip)
		if (l.forall(_.isSuccess))
			Result.sequence(l).map(_.toList)
		else
			Error(l.collect({case Error(ls) => ls}).flatten)
	}
	
	/** List of plate locations in the database. */
	def findAllPlateLocations(): Result[List[PlateLocation]] = {
		val l = bb.mapLocation.keys.toList.map(findPlateLocation)
		if (l.forall(_.isSuccess))
			Result.sequence(l).map(_.toList)
		else
			Error(l.collect({case Error(ls) => ls}).flatten)
	}
	
	/** List of tube locations in the database. */
	def findAllTubeLocations(): Result[List[TubeLocation]] = {
		val l = bb.mapTubeLocation.keys.toList.map(findTubeLocation)
		println("findAllTubeLocations: "+l)
		if (l.forall(_.isSuccess))
			Result.sequence(l).map(_.toList)
		else
			Error(l.collect({case Error(ls) => ls}).flatten)
	}
	
	/** Find tip model with ID `id`. */
	def findTipModel(id: String): Result[TipModel] = {
		find(id, m_mapTipModel, bb.mapTipModel, TipModel.fromBean _, "TipModel")
	}
	
	/** Find tip model with ID `id`. */
	def findTipModel_?(id: String, messages: CmdMessageWriter): Option[TipModel] = {
		find(id, m_mapTipModel, bb.mapTipModel, TipModel.fromBean _, messages, "TipModel")
	}
	
	/** Find plate model with ID `id`. */
	def findPlateModel(id: String): Result[PlateModel] = {
		find(id, m_mapPlateModel, bb.mapPlateModel, PlateModel.fromBean _, "PlateModel")
	}
	
	/** Find tube model with ID `id`. */
	def findTubeModel(id: String): Result[TubeModel] = {
		find(id, m_mapTubeModel, bb.mapTubeModel, TubeModel.fromBean _, "TubeModel")
	}
	
	/** Find tip with ID `id`. */
	def findTip(id: String): Result[Tip] = {
		find(id, m_mapTip, bb.mapTip, Tip.fromBean(this), "Tip")
	}
	
	/** Find tip with ID `id`. */
	def findTip_?(id: String, messages: CmdMessageWriter): Option[Tip] = {
		find(id, m_mapTip, bb.mapTip, Tip.fromBean(this, messages), messages, "Tip")
	}
	
	/** Find initial location of plate with ID `id`. */
	def findPlateLocation(id: String): Result[PlateLocation] = {
		find(id, m_mapLocation, bb.mapLocation, PlateLocation.fromBean(this), "PlateLocation")
	}
	
	/** Find initial location of tube with ID `id`. */
	def findTubeLocation(id: String): Result[TubeLocation] = {
		find(id, m_mapTubeLocation, bb.mapTubeLocation, TubeLocation.fromBean(this), "TubeLocation")
	}
	
	/** Find plate with ID `id`. */
	def findPlate(id: String): Result[Plate] = {
		find(id, m_mapPlate, bb.mapPlate, Plate.fromBean(this), "Plate")
	}
	
	/** Find tube with ID `id`. */
	def findTube(id: String): Result[Tube] = {
		find(id, m_mapTube, bb.mapTube, Tube.fromBean(this), "Tube")
	}
	
	/** Find plate of tube with ID `id`. */
	def findPlateOrTube(id: String): Result[Object] = {
		findPlate(id).orElse(findTube(id))
	}
	
	/** Find substance with ID `id`. */
	def findSubstance(id: String): Result[Substance] = {
		find(id, m_mapSubstance, bb.mapSubstance, Substance.fromBean _, "Substance")
	}
	
	/** Find liquid with ID `id`. */
	def findLiquid(id: String): Result[Liquid] = {
		m_mapLiquid.get(id) match {
			case Some(liquid) => Success(liquid)
			case None =>
				findSubstance(id) match {
					case Error(ls) => Error(ls)
					case Success(substance: SubstanceLiquid) =>
						Success(new Liquid(
							id = id,
							sName_? = Some(id),
							sFamily = substance.physicalProperties.toString,
							contaminants = Set(),
							group = new LiquidGroup(substance.cleanPolicy),
							multipipetteThreshold = if (substance.expensive) 0 else 1000
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
	
	/** Find tips with IDs `lId`. */
	def findTips_?(lId: List[String], messages: CmdMessageWriter): Option[List[Tip]] = {
		val l = lId.map(id => findTip_?(id, messages))
		if (l.exists(_.isEmpty))
			None
		else
			Some(l.flatten)
	}
	
	/** Find well with ID `id`. */
	def findWell(id: String): Result[PlateWell] = {
		m_mapPlateWell.get(id) match {
			case Some(obj) => Success(obj)
			case None => createWell(id)
		}
	}
	
	/** Find well with ID `id`. */
	def findWell_?(id: String, node: CmdNodeBean, requireId: Boolean = true): Option[PlateWell] = {
		if (id == null) {
			if (requireId)
				node.checkPropertyNonNull(null)
			None
		}
		else {
			m_mapPlateWell.get(id) match {
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
		println("createWell: "+WellSpecParser.parse(id))
		for {
			lPlateWellSpec <- WellSpecParser.parse(id)
			_ <- Result.assert(lPlateWellSpec.length == 1, "must provide a single well ID instead of `"+id+"`")
			(idPlate, lWellSpec) = lPlateWellSpec.head
			_ <- Result.assert(lWellSpec.length == 1, "must provide a single well location instead of `"+id+"`")
			_ <- Result.assert(lWellSpec.head.isInstanceOf[WellSpecOne], "must provide a simple well ID instead of `"+id+"`")
			wellSpec = lWellSpec.head.asInstanceOf[WellSpecOne]
			plate <- findPlate(idPlate)
		} yield {
			val index = wellSpec.rc.col * plate.model.rows + wellSpec.rc.row
			val well = new PlateWell(
				id = id,
				idPlate = idPlate,
				index = index,
				iRow = wellSpec.rc.row,
				iCol = wellSpec.rc.col,
				indexName = wellSpec.rc.toString
			)
			m_mapPlateWell(id) = well
			m_mapWell2(id) = well
			well
		}
	}
	
	/** Find well with ID `id`. */
	def findWell2(id: String): Result[Well] = {
		m_mapWell2.get(id) match {
			case None => Error("Well information not available for id `"+id+"`")
			case Some(well2) => Success(well2)
		}
	}
	
	/**
	 * Find wells either on plate with ID `id` or wells containing the substance with ID `id`.
	 * 
	 * This relies on a HACK: it reads from `m_mapSubstanceToVessels` which is populated by [[roboliq.core.Processor]].
	 * The function will therefore only work after `m_mapSubstanceToVessels` has been populated by [[roboliq.core.Processor]].
	 * 
	 * @see findAllIdsContainingSubstance()
	 */
	def findWell2List(id: String): Result[List[Well]] = {
		val ℓid = m_mapSubstanceToVessels.getOrElse(id, List(id))
		Result.mapOver(ℓid)(findWell2)
	}
	
	/** Find system configuration string with ID `id`. */
	def findSystemString_?(id: String, node: CmdNodeBean): Option[String] = {
		bb.mapSystemProperties.get(id) match {
			case None =>
				node.addError("systemProperties", id, "must be set")
				None
			case Some(o) =>
				Some(o.asInstanceOf[String])
		}
	}
	
	/** Find initial state for tip with ID `id`. */
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
	
	/** Find initial state for plate with ID `id`. */
	def findPlateState(id: String): Result[PlateState] = {
		builder.map.get(id) match {
			case Some(state: PlateState) => Success(state)
			case Some(_) => Error("id `"+id+"`: stored state is not a PlateState")
			case None =>
				for { plate <- findPlate(id) } yield {
					val plateState = PlateState.createEmpty(plate)
					builder.map(id) = plateState
					plateState
				}
		}
	}
	
	/** Find initial state for well with ID `id`. */
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
	
	/** Construct the initial well state from events in the database. */
	def reconstructHistory(): Result[Unit] = {
		val lsError = new ArrayBuffer[String]
		builder = new StateBuilder(this)
		for (event <- bb.lEvent) {
			findWellState(event.obj) match {
				case Error(ls) => lsError ++= ls
				case Success(wellState) =>
					event.update(builder)
			}
		}
		if (lsError.isEmpty)
			Success()
		else
			Error(lsError.toList)
	}
	
	/** Return list of vessel IDs which contain the given substance. */
	def findAllIdsContainingSubstance(substance: Substance): Result[List[String]] = {
		def hasSubstance(st: WellState): Boolean = {
			substance match {
				// If the substance is a liquid
				case liquid: SubstanceLiquid =>
					val bContainsLiquid =
						st.content.mapSolventToVolume.contains(liquid) &&
						st.content.mapSoluteToMol.isEmpty
						
					if (bContainsLiquid) {
						if (st.content.mapSolventToVolume.size == 1) {
							true
						}
						// If the other liquid is water
						else if (st.content.mapSolventToVolume.size == 2) {
							st.content.mapSolventToVolume.keys.filter(_ ne liquid).exists(_.id == "water")
						}
						else {
							false
						}
					}
					else
						false
						
				case solid: SubstanceSolid =>
					val bContainsSolute =
						st.content.mapSoluteToMol.contains(solid) &&
						st.content.mapSoluteToMol.size == 1 &&
						st.content.mapSolventToVolume.forall(_._1.id == "water")
					bContainsSolute
			}
		}
		
		// Get list of 
		val l = builder.map.values.collect({case wellState: WellState if hasSubstance(wellState) => {
			wellState match {
				case st: PlateWellState => st.conf.id
				case st: TubeState => st.obj.id
			}
		}}).toList
		
		if (l.isEmpty)
			Error("no vessels found which contain substance `"+substance.id+"`")
		else
			Success(l)
	}
	
	/**
	 * Set the initial location of the given plate.
	 */
	def setInitialPlateLocation(plate: Plate, location: String) = {
		PlateLocationEventBean(plate, location).update(builder)
	}
	
	/**
	 * Set the initial location of the given tube.
	 */
	def setInitialTubeLocation(tube: Tube, location: String, row: Int, col: Int) = {
		TubeLocationEventBean(tube, location, row, col).update(builder)
		//println("stateA: "+ob.findWellState(tube.id))
		m_mapWell2(tube.id) = Well.forTube(findWellState(tube.id).get.asInstanceOf[TubeState], builder).get
	}
}
