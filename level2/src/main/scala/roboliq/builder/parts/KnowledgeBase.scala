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
	val wells = new HashSet[Well]
	val plates = new HashSet[Plate]
	
	val mapLiqToVolConsumed = new HashMap[Liquid, Double]
	val wellKnowledge = new HashMap[Well, WellKnowledge]
	
	def getLiqWells(liq: Liquid): Set[Well] = {
		wells.filter(well => {
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
