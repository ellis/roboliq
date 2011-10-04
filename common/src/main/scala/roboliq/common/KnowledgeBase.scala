package roboliq.common

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import roboliq.common._


class KnowledgeBase {
	//private val m_liqs = new HashSet[Liquid]
	private val m_objs = new HashSet[Obj]
	private val m_wells = new HashSet[Well]
	private val m_plates = new HashSet[Plate]
	private val m_setups = new HashMap[Obj, ObjSetup]

	// Move this to Pipette device's knowledgebase
	val mapLiqToVolConsumed = new HashMap[Liquid, Double]
	
	val setups: scala.collection.Map[Obj, ObjSetup] = m_setups
	
	/*def addLiquid(o: Liquid) {
		m_liqs += o
	}*/
	
	def addObject(o: Obj) {
		if (!m_objs.contains(o)) {
			m_objs += o
			m_setups(o) = o.createSetup()
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
		val setup = getWellSetup(o)
		if (setup.bRequiresIntialLiq_?.isEmpty)
			setup.bRequiresIntialLiq_? = Some(bSrc)
	}
	
	def addPlate(o: Plate) {
		addObject(o)
		if (o.setup.dim_?.isDefined)
			o.setup.dim_?.get.wells.foreach(well => addWell(well))
		m_plates += o
	}
	
	def addPlate(o: Plate, bSrc: Boolean) {
		val setup = getPlateSetup(o)
		if (setup.dim_?.isDefined)
			setup.dim_?.get.wells.foreach(well => addWell(well, bSrc))
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
	
	def getObjSetup[T](o: Obj): T =
		m_setups(o).asInstanceOf[T]
	
	def getWellSetup(o: Well): WellSetup = {
		addWell(o)
		getObjSetup(o)
	}
	
	def getPlateSetup(o: Plate): PlateSetup = {
		addPlate(o)
		getObjSetup(o)
	}
	
	def getReagentSetup(o: Reagent): ReagentSetup = {
		addObject(o)
		getObjSetup(o)
	}
	
	def getLocationSetup(o: Location): LocationSetup = {
		addObject(o)
		getObjSetup(o)
	}
	
	def getMementoSetup[T](o: Memento[T]): MementoSetup[T] = {
		addObject(o)
		getObjSetup(o)
	}
	
	def getReagentWells(reagent: Reagent): Set[Well] = {
		m_wells.filter(well => {
			val wellSetup = getWellSetup(well)
			wellSetup.reagent_? match {
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
		getWellSetup(well).bRequiresIntialLiq_? match {
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
			val pc = getPlateSetup(plate)
			if (pc.dim_?.isDefined && pc.sLabel_?.isDefined) {
				for ((well, i) <- pc.dim_?.get.wells.zipWithIndex) {
					val wc = getWellSetup(well)
					wc.sLabel_? = Some(pc.sLabel_?.get + ":" + (i+1))
					wc.holder_? = Some(plate)
					wc.index_? = Some(i)
				}
			}
		}
		
		val setObjTried = new HashSet[Obj]
		val mapConfs = new HashMap[Obj, ObjConfig]
		val builder = new StateBuilder
		val log = new LogBuilder
		
		def constructConfStateTuples(setups: scala.collection.Map[Obj, ObjSetup]) {
			for ((obj, setup) <- setups) {
				if (!setObjTried.contains(obj)) {
					obj.createConfigAndState0(setup.asInstanceOf[obj.Setup]) match {
						case Error(ls) => log.errors += new LogItem(obj, ls)
						case Success((conf, state)) =>
							mapConfs(obj) = conf
							builder.map(obj) = state
					}
					setObjTried += obj
				}
			}
		}
		
		val wellSetups = m_setups.filter(_._2.isInstanceOf[WellSetup]).map(pair => pair._1.asInstanceOf[Well] -> pair._2.asInstanceOf[WellSetup])

		// Make sure all well reagents have been registered
		wellSetups.foreach(pair => pair._2.reagent_? match {
			case Some(reagent) =>
				val reagentSetup = getReagentSetup(reagent)
				//if (reagentSetup.sName_?.isEmpty) {
				//	reagentSetup.sName_?
				//}
			case _ =>
		})
		
		// Reagents
		val reagentSetups = m_setups.filter(_._2.isInstanceOf[ReagentSetup])
		constructConfStateTuples(reagentSetups)
			
		// Plates
		val plateSetups = m_setups.filter(_._2.isInstanceOf[PlateSetup])
		constructConfStateTuples(plateSetups)

		// Wells
		for ((obj, setup) <- wellSetups) {
			obj.createConfigAndState0(setup.asInstanceOf[obj.Setup], builder) match {
				case Error(ls) => log.errors += new LogItem(obj, ls)
				case Success((conf, state)) =>
					mapConfs(obj) = conf
					builder.map(obj) = state
			}
			setObjTried += obj
		}
		
		// Everything else
		constructConfStateTuples(m_setups)

		if (log.errors.isEmpty) {
			val map2 = builder.map.map(pair => {
				val (obj, state) = pair
				val conf = mapConfs(obj)
				obj -> new SetupConfigState(m_setups(obj), conf, state)
			}).toMap
			val map31 = new ObjMapper(map2)
			Right(KnowledgeStageSuccess(map31, map31.createRobotState()))
		}
		else {
			Left(CompileStageError(log.toImmutable()))
		}
	}
	
	def getLabel(obj: Obj): String = {
		m_setups.get(obj) match {
			case Some(setup) => setup.getLabel(this)
			case None => obj.toString
		}
	}
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
}
