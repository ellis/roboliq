package roboliq.builder.parts

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
}

class Part {
	var id_? : Option[Int] = None
	var parent_? : Option[Part] = None
	var index_? : Option[Int] = None
	var location_? : Option[String] = None
	
	def setId(id: Int) { id_? = Some(id) }
	def setParent(parent: Part) { parent_? = Some(parent) }
	def setIndex(index: Int) { index_? = Some(index) }
}

class PartData {
	var id = new Setting[Int]
	var parent = new Setting[Part]
	var index = new Setting[Int]
	var location = new Setting[String]
}

class PartProxy(kb: KnowledgeBase, o: Part) {
	val data = kb.getPartData(o)
	
	def location = data.location.get
	def location_=(s: String) { data.location.user_? = Some(s) }
}

class Liquid

class LiquidData {
	var liquidClass = new Setting[String]
}

class LiquidProxy(kb: KnowledgeBase, o: Liquid) {
	val data = kb.getLiquidData(o)
	
	def liquidClass = data.liquidClass.get
	def liquidClass_=(s: String) { data.liquidClass.user_? = Some(s) }
}

class Well extends Part {
}

object Well {
	def apply(parent: Part, index: Int): Well = {
		val o = new Well
		o.parent_? = Some(parent)
		o.index_? = Some(index)
		o
	}
}

class Plate extends Part {
	var nRows_? : Option[Int] = None
	var nCols_? : Option[Int] = None
	var wells_? : Option[Seq[Well]] = None
	
	def setDimension(nRows: Int, nCols: Int) {
		require(nRows_?.isEmpty && nCols_?.isEmpty && wells_?.isEmpty, "setDimension() can only be called on a plate whose dimension has not been previously defined.")
		require(nRows > 0, "nRows must be > 0")
		require(nCols > 0, "nCols must be > 0")
		val nWells = nRows * nCols
		val wells = (0 to nWells).map(index => Well(this, index))
		nRows_? = Some(nRows)
		nCols_? = Some(nCols)
		wells_? = Some(wells)
	}
	
	def apply(rows: Int, cols: Int, location: String): Plate = {
		setDimension(rows, cols)
		location_? = Some(location)
		this
	}
}

class PlateData {
	var nRows_? : Option[Int] = None
	var nCols_? : Option[Int] = None
	var wells_? : Option[Seq[Well]] = None
}

class PlateProxy(kb: KnowledgeBase, o: Plate) {
	val data = kb.getPlateData(o)
	
	def rows_=(n: Int) { data.nRows_? = Some(n) }
}

object Plate {
	def apply(): Plate = {
		val o = new Plate
		o
	}
	
	def apply(nRows: Int, nCols: Int): Plate = {
		val o = new Plate
		o.setDimension(nRows, nCols)
		o
	}
	
	def apply(rows: Int, cols: Int, location: String): Plate = {
		val o = apply(rows, cols)
		o.location_? = Some(location)
		o
	}
}
