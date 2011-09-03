package roboliq.protocol

class Plate(var name: String = null) {
	def apply(loc: WellLoc): PlateWells = new PlateWells(Seq(this -> Seq(loc)))
	override def toString = if (name != null) name else super.toString
}
