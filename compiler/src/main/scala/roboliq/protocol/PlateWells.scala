/*package roboliq.protocol

//import roboliq.common
//import roboliq.common._


case class WellCoord(row: Int, col: Int) {
	override def toString = (row + 'A').asInstanceOf[Char].toString + (col + 1)
}

sealed abstract class WellLoc

case class WellLocA(a: WellCoord) extends WellLoc {
	def +(n: Int): WellLocAN = new WellLocAN(a, n)
	def -(b: WellCoord): WellLocAB = new WellLocAB(a, b)
	override def toString = a.toString
}

case class WellLocAN(a: WellCoord, n: Int) extends WellLoc {
	override def toString = a.toString + "+" + n
}

case class WellLocAB(a: WellCoord, b: WellCoord) extends WellLoc {
	override def toString = a + "-" + b
}

case class PlateWells(seq: Seq[Tuple2[Plate, Seq[WellLoc]]]) {
	def +(that: PlateWells) = new PlateWells(seq ++ that.seq)
	override def toString = seq.map(pair => pair._1.toString + ":" + pair._2.mkString(",")).mkString(";") 
}
*/