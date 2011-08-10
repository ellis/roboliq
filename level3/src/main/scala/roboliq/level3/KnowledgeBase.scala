package roboliq.level3

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet


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
	val map31 = new HashMap[Part, roboliq.parts.Part]
	
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
		val d = m_plateData.getOrElseUpdate(plate, new PlateData)
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
	
	def getPlateData(o: Plate): PlateData = {
		if (!m_plateData.contains(o))
			m_plateData(o) = new PlateData
		m_plateData(o)
	}
	
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
	
	def concretize() {
		val kb = this
		
		// check whether we have all the information we need
		def checkPart(part: Part): Boolean = {
			val d = m_partData(part)
			// Do we know its location?
			(
				d.parent.isDefined && 
				d.index.isDefined &&
				checkPart(d.parent.get)
			)
		}
		def checkWell(well: Well): Boolean = {
			if (!checkPart(well))
				return false
			val d = m_wellData(well)
			if (d.bRequiresIntialLiq_?.isEmpty)
				return false
			if (d.bRequiresIntialLiq_?.get == true) {
				if (d.bRequiresIntialLiq_?.isEmpty)
					return false
			}
			return true
		}
		def checkPlate(plate: Plate): Boolean = {
			if (!checkPart(plate))
				return false
			val d = m_plateData(plate)
			if (!d.nRows.isDefined || !d.nCols.isDefined || !d.wells.isDefined)
				return false
			if (!d.wells.get.forall(checkWell))
				return false
			return true
		}
		def checkLiquid(liquid: Liquid): Boolean = {
			val wells = kb.getLiqWells(liquid)
			if (wells.isEmpty)
				false
			else
				wells.forall(checkWell)
		}
		
		val b =
			liqs.foldLeft(true) { (b, o) => b && checkLiquid(o) } &&
			m_wells.foldLeft(true) { (b, o) => b && checkWell(o) } &&
			m_plates.foldLeft(true) { (b, o) => b && checkPlate(o) }
		
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
	}
}
