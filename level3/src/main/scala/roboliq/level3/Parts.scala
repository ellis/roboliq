package roboliq.level3


class Part {
	var sLabel: String = "<none>"
}
class Liquid {
	var sLabel: String = "<none>"
} 
class Well extends Part
class Plate extends Part


sealed class Setting[T] {
	var default_? : Option[T] = None
	var user_? : Option[T] = None
	var possible: List[T] = Nil
	
	def get = user_? match {
		case None =>
			default_?.get
		case Some(o) =>
			o
	}
	
	def get_? : Option[T] = user_? match {
		case None =>
			default_?
		case Some(o) =>
			user_?
	} 
	
	def isDefined: Boolean = { user_?.isDefined || default_?.isDefined }
	def isEmpty: Boolean = !isDefined
}

class PartData {
	val id = new Setting[Int]
	val parent = new Setting[Part]
	val index = new Setting[Int]
	val location = new Setting[String]

	def setId(id: Int) { this.id.user_? = Some(id) }
	def setParent(parent: Part) { this.parent.user_? = Some(parent) }
	def setIndex(index: Int) { this.index.user_? = Some(index) }
}

class PartProxy(kb: KnowledgeBase, o: Part) {
	val data = kb.getPartData(o)
	
	def id = data.id.get
	def id_=(id: Int) { data.id.user_? = Some(id) }
	
	def index = data.index.get
	def index_=(i: Int) { data.index.user_? = Some(i) }
	
	def location = data.location.get
	def location_=(s: String) { data.location.user_? = Some(s) }
}

class LiquidData {
	var liquidClass = new Setting[String]
}

class LiquidProxy(kb: KnowledgeBase, o: Liquid) {
	val data = kb.getLiquidData(o)
	
	def liquidClass = data.liquidClass.get
	def liquidClass_=(s: String) { data.liquidClass.user_? = Some(s) }
}

class WellData {
	var bRequiresIntialLiq_? : Option[Boolean] = None
	/** Initial liquid */
	var liq_? : Option[Liquid] = None
	/** Initial volume of liquid */
	var nVol_? : Option[Double] = None
}

object WellProxy {
	def apply(kb: KnowledgeBase, parent: Part, index: Int): Well = {
		val o = new Well
		val data = kb.getPartData(o)
		data.parent.user_? = Some(parent)
		data.index.user_? = Some(index)
		o
	}
}

class PlateData {
	val nRows = new Setting[Int]
	val nCols = new Setting[Int]
	val wells = new Setting[Seq[Well]]
}

class PlateProxy(kb: KnowledgeBase, o: Plate) {
	val data = kb.getPlateData(o)
	
	def rows = data.nRows.get
	def rows_=(n: Int) { data.nRows.user_? = Some(n) }

	def cols = data.nCols.get
	def cols_=(n: Int) { data.nCols.user_? = Some(n) }

	def setDimension(nRows: Int, nCols: Int) {
		val data = kb.getPlateData(o)
		require(data.nRows.isEmpty && data.nCols.isEmpty && data.wells.isEmpty, "setDimension() can only be called on a plate whose dimension has not been previously defined.")
		require(nRows > 0, "nRows must be > 0")
		require(nCols > 0, "nCols must be > 0")
		val nWells = nRows * nCols
		val wells = (0 to nWells).map(index => WellProxy(kb, o, index))
		rows = nRows
		cols = nCols
		data.wells.user_? = Some(wells)
	}
}

object Plate {
	def apply(): Plate = {
		val o = new Plate
		o
	}
	
	def apply(kb: KnowledgeBase, nRows: Int, nCols: Int): Plate = {
		val o = new Plate
		val p = new PlateProxy(kb, o)
		p.setDimension(nRows, nCols)
		o
	}
	
	def apply(kb: KnowledgeBase, rows: Int, cols: Int, location: String): Plate = {
		val o = apply(kb, rows, cols)
		val p = new PartProxy(kb, o)
		p.location = location
		o
	}
}
