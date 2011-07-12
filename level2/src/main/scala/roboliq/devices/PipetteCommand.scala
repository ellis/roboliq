package roboliq.devices

import roboliq.parts._


class TipSpec()
class MixSpec(val nVolume: Double, val nCount: Int)
class PipetteSpec(
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
	def pipette(args: PipetteArgs) {
		
	}
}