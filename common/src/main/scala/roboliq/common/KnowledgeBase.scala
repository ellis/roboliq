package roboliq.common

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import roboliq.common._


class KnowledgeBase {
	private val m_liqs = new HashSet[Liquid]
	private val m_objs = new HashSet[Obj]
	private val m_wells = new HashSet[Well]
	private val m_plates = new HashSet[Plate]
	private val m_configL3 = new HashMap[Obj, AbstractConfigL3]
	private val m_state0L3 = new HashMap[Obj, AbstractStateL3]

	// Move this to Pipette device's knowledgebase
	val mapLiqToVolConsumed = new HashMap[Liquid, Double]
	
	//val configL1: scala.collection.Map[Obj, Any] = m_configL1
	val configL3: scala.collection.Map[Obj, AbstractConfigL3] = m_configL3
	//val state0L1: scala.collection.Map[Obj, Any] = m_state0L1
	val state0L3: scala.collection.Map[Obj, AbstractStateL3] = m_state0L3
	
	def addLiquid(o: Liquid) {
		m_liqs += o
	}
	
	def addObject(o: Obj) {
		if (!m_objs.contains(o)) {
			m_objs += o
			m_configL3(o) = o.createConfigL3()
			m_state0L3(o) = o.createState0L3()
		}
	}
	
	def addWell(o: Well) {
		addObject(o)
		m_wells += o
	}
	
	def addWell(o: Well, bSrc: Boolean) {
		addWell(o)
		if (o.getState0L3(state0L3).get.bRequiresIntialLiq_?.isEmpty)
			o.getState0L3(state0L3).get.bRequiresIntialLiq_? = Some(bSrc)
	}
	
	def addPlate(o: Plate) {
		addObject(o)
		m_plates += o
	}
	
	def addPlate(o: Plate, bSrc: Boolean) {
		addPlate(o)
		if (o.getConfigL3(configL3).get.dim_?.isDefined)
			o.getConfigL3(configL3).get.dim_?.get.wells.foreach(well => addWell(well, bSrc))
	}
	
	private def getObjData[T](o: Obj, map: HashMap[Obj, _ <: Any]): T =
		map(o).asInstanceOf[T]
	
	def getWellConfigL3(o: Well): WellConfigL3 = {
		addWell(o)
		getObjData(o, m_configL3)
	}
	
	def getWellState0L3(o: Well): WellStateL3 = {
		addWell(o)
		getObjData(o, m_state0L3)
	}
	
	def getPlateConfigL3(o: Plate): PlateConfigL3 = {
		addPlate(o)
		getObjData(o, m_configL3)
	}
	
	def getPlateState0L3(o: Plate): PlateStateL3 = {
		addPlate(o)
		getObjData(o, m_state0L3)
	}
	
	def getLiqWells(liq: Liquid): Set[Well] = {
		m_wells.filter(well => {
			val st = m_state0L3(well).asInstanceOf[WellStateL3]
			st.liquid_? match {
				case None => false
				case Some(liq2) => (liq eq liq2)
			}
		}).toSet
	}
	
	def doesWellRequireInitialLiq(well: Well): Boolean = {
		getWellState0L3(well).bRequiresIntialLiq_? match {
			case None => false
			case Some(b) => b
		}
	}
	
	def printKnown() {
		println("Liquids:")
		m_liqs.foreach(println)
		println()
		
		println("Plates:")
		m_plates.foreach(println)
		println()
		
		println("Wells:")
		m_wells.foreach(println)
		println()
	}
	
	type Errors = Seq[Tuple2[Obj, Seq[String]]]
	
	def concretize(): Either[Errors, ObjMapper] = {
		for (plate <- m_plates) {
			val pc = getPlateConfigL3(plate)
			if (pc.dim_?.isDefined) {
				for ((well, i) <- pc.dim_?.get.wells.zipWithIndex) {
					val wc = getWellConfigL3(well)
					wc.holder_? = Some(plate)
					wc.index_? = Some(i)
				}
			}
		}
		
		val configL1 = for ((o, _) <- m_configL3) yield {
			o.createConfigL1(m_configL3) match {
				case Left(ls) => Left(o -> ls)
				case Right(c) => Right(o -> c)
			}
		}
		val state0L1 = for ((o, _) <- m_state0L3) yield {
			o.createState0L1(m_state0L3) match {
				case Left(ls) => Left(o -> ls)
				case Right(st) => Right(o -> st)
			}
		}
		val st1 = m_state0L3.map(pair => pair._1.createState0L1(m_state0L3))
		
		if (configL1.forall(_.isRight) && state0L1.forall(_.isRight)) {
			val map31 = new ObjMapper(
					configL1 = configL1.map(_.right.get).toMap,
					configL3 = m_configL3.toMap,
					state0L1 = state0L1.map(_.right.get).toMap,
					state0L3 = m_state0L3.toMap
			)
			Right(map31)
		}
		else {
			val errors = configL1.filter(_.isLeft).map(_.left.get) ++ state0L1.filter(_.isLeft).map(_.left.get)
			Left(errors.toSeq)
		}
	}
}
