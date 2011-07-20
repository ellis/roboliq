package roboliq.devices

import roboliq.builder.parts._

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
					val liq = src.asInstanceOf[Liquid]
					val wells = kb.getLiqWells(liq)
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
