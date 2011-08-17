package roboliq.level3

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import roboliq.common._


class KnowledgeBase {
	private val m_liqs = new HashSet[Liquid]
	private val m_objs = new HashSet[Obj]
	private val m_wells = new HashSet[Well]
	private val m_plates = new HashSet[Plate]
	//private val m_configL1 = new HashMap[Obj, Any]
	private val m_configL3 = new HashMap[Obj, Any]
	//private val m_state0L1 = new HashMap[Obj, Any]
	private val m_state0L3 = new HashMap[Obj, Any]
	/*private val m_liquidData = new HashMap[Liquid, LiquidData]
	private val m_partData = new HashMap[Part, PartData]
	private val m_wellData = new HashMap[Well, WellData]
	private val m_plateData = new HashMap[Plate, PlateData]*/
	
	//val mapPartToLoc = new HashMap[Part, String]

	// Move this to Pipette device's knowledgebase
	val mapLiqToVolConsumed = new HashMap[Liquid, Double]
	
	//val configL1: scala.collection.Map[Obj, Any] = m_configL1
	val configL3: scala.collection.Map[Obj, Any] = m_configL3
	//val state0L1: scala.collection.Map[Obj, Any] = m_state0L1
	val state0L3: scala.collection.Map[Obj, Any] = m_state0L3
	
	private def addObject(o: Obj) {
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
	
	/*def addWell(well: Well, bSrc: Boolean) {
		addWell(well)
		if (well.getState0L3(state0L3).get.bRequiresIntialLiq_?.isEmpty)
			well.getState0L3(state0L3).get.bRequiresIntialLiq_? = Some(bSrc)
	}*/
	
	def addPlate(o: Plate) {
		addObject(o)
		m_plates += o
	}
	
	private def getObjData[T](o: Obj, map: HashMap[Obj, Any]): T =
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
		val configL1 = for ((o, _) <- m_configL3) yield {
			o.createConfigL1(m_configL3) match {
				case Left(ls) => Left(o -> ls)
				case Right(c) => Right(o -> c)
			}
		}
		val state0L1 = for ((o, _) <- m_state0L3) yield {
			o.createState0L1(m_configL3) match {
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
			val errors = configL1.map(_.left.get) ++ state0L1.map(_.left.get)
			Left(errors.toSeq)
		}
	}
}

/*
class KnowledgeBase {
	val liqs = new HashSet[Liquid]
	private val m_wells = new HashSet[Well]
	private val m_plates = new HashSet[Plate]
	private val m_liquidData = new HashMap[Liquid, LiquidData]
	private val m_partData = new HashMap[Part, PartData]
	private val m_wellData = new HashMap[Well, WellData]
	private val m_plateData = new HashMap[Plate, PlateData]
	
	//val mapPartToLoc = new HashMap[Part, String]
	
	val mapLiqToVolConsumed = new HashMap[Liquid, Double]
	
	/** Map level 3 parts to level 1 parts */
	val map31 = new HashMap[ObjectL3, roboliq.parts.ObjectL1]
	
	private var m_idNext = 1;
	
	def liquidData(o: Liquid) = m_liquidData.getOrElseUpdate(o, new LiquidData)
	def partData(o: Part) = m_partData.getOrElseUpdate(o, new PartData)
	def wellData(o: Well) = m_wellData.getOrElseUpdate(o, new WellData)
	def plateData(o: Plate) = m_plateData.getOrElseUpdate(o, new PlateData)
	
	private def setId(o: Part) {
		val d = m_partData.getOrElseUpdate(o, new PartData)
		d.id.user_? = Some(m_idNext)
		m_idNext += 1
	}
	
	def addWell(well: Well, bSrc: Boolean) {
		if (!m_wells.contains(well)) {
			setId(well)
			m_wells += well

			val d = m_wellData.getOrElseUpdate(well, new WellData)
			d.bRequiresIntialLiq_? = Some(bSrc)
		}
	}
	
	def addPlate(plate: Plate, bSrc: Boolean) {
		if (!m_plates.contains(plate)) {
			setId(plate)
			m_plates += plate
		}
		val d = getPlateData(plate)
		//println("plate: "+plate)
		//println(d.wells)
		if (d.wells.isDefined) {
			d.wells.get.foreach(well => addWell(well, bSrc))
		}
	}
	
	def getLiqWells(liq: Liquid): Set[Well] = {
		m_wells.filter(well => {
			val d = m_wellData(well)
			d.liq_? match {
				case None => false
				case Some(liq2) => (liq eq liq2)
			}
		}).toSet
	}
	
	def getLiquidData(o: Liquid): LiquidData = {
		if (!m_liquidData.contains(o))
			m_liquidData(o) = new LiquidData
		m_liquidData(o)
	}
	
	def getPartData(o: Part): PartData = {
		if (!m_partData.contains(o))
			m_partData(o) = new PartData
		m_partData(o)
	}
	
	def getPlateData(o: Plate): PlateData = m_plateData.getOrElseUpdate(o, new PlateData)
	
	def doesWellRequireInitialLiq(well: Well): Boolean = {
		m_wellData(well).bRequiresIntialLiq_? match {
			case None => false
			case Some(b) => b
		}
	}
	
	def printKnown() {
		println("Liquids:")
		liqs.foreach(println)
		println()
		
		println("Plates:")
		m_plates.foreach(println)
		println()
		
		println("Wells:")
		m_wells.foreach(println)
		println()
	}
	
	def printUnknown() {
		val plates1 = m_plateData.filter(pair => pair._2.nCols.isEmpty)
		if (!plates1.isEmpty) {
			println("Plates without dimensions:")
			for ((plate, d) <- plates1) {
				println(plate)
			}
			println()
		}
		
		println("Liquids:")
		liqs.foreach(println)
		println()
		
		println("Wells:")
		m_wells.foreach(println)
		println()
	}
	
	def concretize(): Seq[Tuple2[Object, String]] = {
		val kb = this
		
		val missing = new ArrayBuffer[Tuple2[Object, String]]
		
		// check whether we have all the information we need
		def checkPart(part: Part): Boolean = {
			val d = m_partData(part)
			// Do we know its location?
			val b = d.location.isDefined ||
				(
					d.parent.isDefined && 
					d.index.isDefined &&
					checkPart(d.parent.get)
				)
			if (!b)
				missing += part -> "location"
			b
		}
		def checkWell(well: Well): Boolean = {
			var b = true
			b &= checkPart(well)
			val d = m_wellData(well)
			//if (d.bRequiresIntialLiq_?.isEmpty)
			//	return false
			if (d.bRequiresIntialLiq_?.isDefined && d.bRequiresIntialLiq_?.get == true) {
				if (d.liq_?.isEmpty) {
					missing += well -> "liquid" 
					b = false
				}
			}
			b
		}
		def checkPlate(plate: Plate): Boolean = {
			var b = true
			b &= checkPart(plate)
			val d = m_plateData(plate)
			if (!d.nRows.isDefined || !d.nCols.isDefined || !d.wells.isDefined) {
				missing += plate -> "setDimension"
				b = false
			}
			else {
				b &= d.wells.get.forall(checkWell)
			}
			b
		}
		def checkLiquid(liquid: Liquid): Boolean = {
			val wells = kb.getLiqWells(liquid)
			if (wells.isEmpty) {
				missing += liquid -> "wells"
				false
			}
			else
				wells.forall(checkWell)
		}
		
		val b =
			liqs.foldLeft(true) { (b, o) => b && checkLiquid(o) } &&
			m_wells.foldLeft(true) { (b, o) => b && checkWell(o) } &&
			m_plates.foldLeft(true) { (b, o) => b && checkPlate(o) }
		
		map31.clear()
		if (b) {
			val ps = new HashMap[Plate, roboliq.parts.Plate]
			val ws = new HashMap[Well, roboliq.parts.Well]
			m_plates.foreach(plate => {
				val d = m_plateData(plate)
				val p1 = new roboliq.parts.Plate(d.nRows.get, d.nCols.get)
				ps(plate) = p1
				map31(plate) = p1
				d.wells.get.foreach(well => {
					val wd = m_partData(well)
					val w1 = new roboliq.parts.Well(p1, wd.index.get)
					ws(well) = w1
					map31(well) = w1
				})
			})
		}
		
		missing
	}
}
*/
