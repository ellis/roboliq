package roboliq.builder.parts

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

class WellKnowledge {
	var bRequiresIntialLiq_? : Option[Boolean] = None
	/** Initial liquid */
	var liq_? : Option[Liquid] = None
	/** Initial volume of liquid */
	var nVol_? : Option[Double] = None
}


class KnowledgeBase {
	val liqs = new HashSet[Liquid]
	private val m_wells = new HashSet[Well]
	private val m_plates = new HashSet[Plate]
	private val m_liquidData = new HashMap[Liquid, LiquidData]
	private val m_partData = new HashMap[Part, PartData]
	private val m_plateData = new HashMap[Plate, PlateData]
	
	val mapPartToLoc = new HashMap[Part, String]
	
	val mapLiqToVolConsumed = new HashMap[Liquid, Double]
	val wellKnowledge = new HashMap[Well, WellKnowledge]
	
	private var m_idNext = 1;
	
	private def setId(o: Part) {
		o.id_? = Some(m_idNext)
		m_idNext += 1
	}
	
	def addWell(well: Well, bSrc: Boolean) {
		if (!m_wells.contains(well)) {
			assert(!wellKnowledge.contains(well))
			
			setId(well)
			m_wells += well
			
			val wk = new WellKnowledge
			wk.bRequiresIntialLiq_? = Some(bSrc)
			wellKnowledge(well) = wk
		}
	}
	
	def addPlate(plate: Plate, bSrc: Boolean) {
		if (!m_plates.contains(plate)) {
			setId(plate)
			m_plates += plate
			plate.wells_? match {
				case None =>
				case Some(wells) =>
					wells.foreach(well => addWell(well, bSrc))
			}
		}
	}
	
	def getLiqWells(liq: Liquid): Set[Well] = {
		m_wells.filter(well => {
			wellKnowledge.get(well) match {
				case None => false
				case Some(wk) =>
					wk.liq_? match {
						case None => false
						case Some(liq2) => (liq eq liq2)
					}
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
		wellKnowledge(well).bRequiresIntialLiq_? match {
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
		val plates1 = m_plates.filter(_.nCols_?.isEmpty)
		if (!plates1.isEmpty) {
			println("Plates without dimensions:")
			for (plate <- plates1) {
				if (plate.nCols_?.isEmpty)
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
	
	//val liquidsNeedingLoc = new scala.collection.mutable.HashSet[Liquid]
	//val wellsNeedingLoc = new scala.collection.mutable.HashSet[Well]
	//val wellsNeedingLiquid = new scala.collection.mutable.HashSet[Well]
	
	//private val m_mapLiquidToWells = new scala.collection.mutable.HashMap[Liquid, List[Well]]
	
	//def getLiquidWells(liquid: Liquid): List[Well] = m_mapLiquidToWells.getOrElse(liquid, Nil)

	/*
	def addUserPart(o: Part) {
		partsUser += o
	}
	
	def addUserLiquid(o: Liquid) {
		liquids += o
	}
	
	def addLiquid(liquid: Liquid) {
		userObjects += liquid
		if (!m_mapLiquidToWells.contains(liquid))
			m_mapLiquidToWells(liquid) = Nil
		if (m_mapLiquidToWells(liquid).isEmpty)
			liquidsNeedingLoc += liquid
	}
	
	def addWell(well: Well) {
		userObject += well
		well.holder
	}
	*/
}
