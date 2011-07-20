package roboliq.builder.parts

class Part {
	var id_? : Option[Int] = None
	var parent_? : Option[Part] = None
	var index_? : Option[Int] = None
	
	def setId(id: Int) { id_? = Some(id) }
	def setParent(parent: Part) { parent_? = Some(parent) }
	def setIndex(index: Int) { index_? = Some(index) }
}

class Liquid

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
}
