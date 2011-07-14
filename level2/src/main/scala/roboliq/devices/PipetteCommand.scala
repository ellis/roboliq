package roboliq.devices

import roboliq.parts._

class KnowledgeBase {
	val userObjects = new scala.collection.mutable.HashSet[Object]
	
	val liquidsNeedingLoc = new scala.collection.mutable.HashSet[Liquid]
	val wellsNeedingLoc = new scala.collection.mutable.HashSet[Well]
	val wellsNeedingLiquid = new scala.collection.mutable.HashSet[Well]
	
	private val m_mapLiquidToWells = new scala.collection.mutable.HashMap[Liquid, List[Well]]
	
	def getLiquidWells(liquid: Liquid): List[Well] = m_mapLiquidToWells.getOrElse(liquid, Nil)
	
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
}

class TipSpec()
class MixSpec(val nVolume: Double, val nCount: Int)
class PipetteItem(
		val src: Object,
		val dest: Object,
		val nVolume: Double,
		val tipSpec_? : Option[TipSpec] = None,
		val mixSpec_? : Option[MixSpec] = None)

class PipetteArgs(
	val srcs: Seq[Object],
	val dests: Seq[Object],
	val volumes: Map[Object, Double],
	val mixSpecs: Map[Object, MixSpec]
)

class PipetteCommand {
	def pipette(kb: KnowledgeBase, items: Seq[PipetteItem]) {
		val srcs = Set() ++ items.map(_.src)
		if (srcs.size == 0)
			return // TODO: add error handling
		else {
			val src0 = srcs.head
			for (src <- srcs) {
				if (src.isInstanceOf[Liquid]) {
					val liquid = src.asInstanceOf[Liquid]
					val wells = kb.getLiquidWells(liquid)
					if (wells.isEmpty)
						kb.liquidsNeedingLoc += liquid
				}
				else if (src.isInstanceOf[Well]) {
					
				}
			}
			val srcLiquids = 0
		}
	}
}
