package roboliq.common

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import roboliq.common._


class KnowledgeBase {
	//private val m_liqs = new HashSet[Liquid]
	private val m_objs = new HashSet[Obj]
	private val m_wells = new HashSet[Well]
	private val m_plates = new HashSet[PlateObj]

	// Move this to Pipette device's knowledgebase
	val mapLiqToVolConsumed = new HashMap[Liquid, Double]
	
	val lWell: scala.collection.Set[Well] = m_wells
	
	/*def addLiquid(o: Liquid) {
		m_liqs += o
	}*/
	
	def addObject(o: Obj) {
		if (!m_objs.contains(o)) {
			m_objs += o
		}
	}
	
	def addReagent(o: Reagent) {
		addObject(o)
	}
	
	def addWell(o: Well) {
		addObject(o)
		m_wells += o
	}
	
	def addWell(o: Well, bSrc: Boolean) {
		if (o.bRequiresIntialLiq_?.isEmpty)
			o.bRequiresIntialLiq_? = Some(bSrc)
	}
	
	def addPlate(o: PlateObj) {
		addObject(o)
		if (o.dim_?.isDefined)
			o.dim_?.get.wells.foreach(well => addWell(well))
		m_plates += o
	}
	
	def addPlate(o: PlateObj, bSrc: Boolean) {
		if (o.dim_?.isDefined)
			o.dim_?.get.wells.foreach(well => addWell(well, bSrc))
	}

	def addWellPointer(o: WellPointer): Unit = o match {
		case p: WellPointerVar => p.pointer_?.foreach(addWellPointer)
		case WellPointerWell(well) => addWell(well)
		case WellPointerWells(lWell) => lWell.foreach(addWell)
		case WellPointerPlate(plate) => addPlate(plate)
		case WellPointerReagent(reagent) => addReagent(reagent)
		case WellPointerPlateAddress(plate, _) => addPlate(plate)
		case WellPointerSeq(seq) => seq.foreach(addWellPointer)
	}
	
	def addWellPointer(o: WellPointer, bSrc: Boolean) {
		addWellPointer(o)
		o.getWells(this).foreach(_.foreach(well => addWell(well, bSrc)))
	}
	
	def getReagentWells(reagent: Reagent): Set[Well] = {
		m_wells.filter(well => {
			well.reagent_? match {
				case None => false
				case Some(reagent2) => (reagent eq reagent2)
			}
		}).toSet
	}
	
	def getLiqWells(liquid: Liquid, states: StateMap): Set[Well] = {
		m_wells.filter(well => {
			well.state(states).liquid eq liquid
		}).toSet
	}
	
	def doesWellRequireInitialLiq(well: Well): Boolean = {
		well.bRequiresIntialLiq_? match {
			case None => false
			case Some(b) => b
		}
	}
	
	/*def printKnown() {
		println("Liquids:")
		m_liqs.foreach(println)
		println()
		
		println("Plates:")
		m_plates.foreach(println)
		println()
		
		println("Wells:")
		m_wells.foreach(println)
		println()
	}*/
	
	//type Errors = Seq[Tuple2[Obj, Seq[String]]]
	
	def concretize(): Either[CompileStageError, KnowledgeStageSuccess] = {
		for (plate <- m_plates) {
			if (plate.dim_?.isDefined && plate.sLabel_?.isDefined) {
				for ((well, i) <- plate.dim_?.get.wells.zipWithIndex) {
					well.sLabel_? = Some(plate.sLabel_?.get + ":" + (i+1))
					well.holder_? = Some(plate)
					well.index_? = Some(i)
				}
			}
		}
		
		val setObjTried = new HashSet[Obj]
		val mapConfs = new HashMap[Obj, ObjConfig]
		val builder = new StateBuilder
		val log = new LogBuilder
		
		println("A: "+log.errors.isEmpty)
		
		def constructConfStateTuples(lObj: Iterable[Obj]) {
			for (obj <- lObj) {
				if (!setObjTried.contains(obj)) {
					obj.createConfigAndState0() match {
						case Error(ls) => log.errors += new LogItem(obj, ls)
						case Success((conf, state)) =>
							mapConfs(obj) = conf
							builder.map(obj) = state
					}
					setObjTried += obj
				}
			}
		}
		
		println("A: "+log.errors.isEmpty)
		fillEmptySourceWells()
		println("A: "+log.errors.isEmpty)
		
		// Make sure all well reagents have been registered
		m_wells.foreach(well => well.reagent_? match {
			case Some(reagent) => addReagent(reagent)
			case None =>
		})
		
		println("A: "+log.errors.isEmpty)
		// Reagents
		val reagents = m_objs.collect({case o: Reagent => o})
		constructConfStateTuples(reagents)
			
		println("A: "+log.errors.isEmpty)
		// Plates
		constructConfStateTuples(m_plates)

		println("A: "+log.errors.isEmpty)
		fillEmptySourceWells()
		val reagents2 = m_objs.collect({case o: Reagent => o})
		constructConfStateTuples(reagents2)

		println("A: "+log.errors.isEmpty)
		// Wells
		for (obj <- m_wells) {
			obj.createConfigAndState0(builder) match {
				case Error(ls) =>
					println("Error for well:", obj)
					log.errors += new LogItem(obj, ls)
				case Success((conf, state)) =>
					mapConfs(obj) = conf
					builder.map(obj) = state
			}
			setObjTried += obj
		}
		
		println("A: "+log.errors.isEmpty)
		// Everything else
		constructConfStateTuples(m_objs)

		if (log.errors.isEmpty) {
			val map2 = builder.map.map(pair => {
				val (obj, state) = pair
				val conf = mapConfs(obj)
				obj -> new SetupConfigState(conf, state)
			}).toMap
			val map31 = new ObjMapper(map2)
			Right(KnowledgeStageSuccess(map31, map31.createRobotState()))
		}
		else {
			Left(CompileStageError(log.toImmutable()))
		}
	}
	
	private def fillEmptySourceWells() {
		m_wells.foreach(fillEmptySourceWell)
	}
	
	private def fillEmptySourceWell(setup: Well) {
		if (setup.bRequiresIntialLiq_? == Some(true) && setup.reagent_?.isEmpty) {
			println("fillEmptySourceWell: "+setup.bRequiresIntialLiq_?.toString+setup)
			println(setup.holder_?, setup.index_?)
			for {
				plate <- setup.holder_?
				index <- setup.index_?
			} {
				val id = plate.getLabel(this) + "#" + (index + 1)
				//println("id: "+id)
				//pConfig.setReagent(id, plate, index + 1, "DEFAULT", None)
				val reagent = new Reagent
				reagent.sName_? = Some(id)
				reagent.sFamily_? = Some("Water")
				reagent.group_? = Some(new LiquidGroup(GroupCleanPolicy.Decontaminate))
				setup.reagent_? = Some(reagent)
			}
		}
	}
	
	def getLabel(obj: Obj): String = obj.getLabel(this)

	/*def printErrors(errors: Errors) {
		val grouped: Map[Obj, Errors] = errors.groupBy(_._1)
		val errors2 = grouped.map(pair => pair._1 -> pair._2.flatMap(_._2))
		val sorted = errors2.toSeq.sortBy(pair => m_setups(pair._1).getLabel(this))
		sorted.foreach(printError)
	}
	
	def printError(pair: Tuple2[Obj, Seq[String]]) {
		val (obj, ls) = pair
		println(m_setups(obj).getLabel(this))
		for (s <- ls) {
			println("\t"+s)
		}
	}*/
	
	override def toString: String = {
		val lReagent = m_objs.toList.collect({case o: Reagent => o})
		val lsReagent: List[String] = lReagent.map(_.getLabel(this))
		val lsReagentWells: List[String] = lReagent.map(setup => {
			setup.getLabel(this)+" -> "+getReagentWells(setup).map(_.toString()).mkString(",")
		})
		(
			("Reagents:" :: lsReagent) ++
			("Reagent Wells" :: lsReagentWells)
		).mkString("\n")
	}
}
