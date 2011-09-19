package roboliq.common

sealed abstract class WellAddress

object WellAddress {
	def apply(well: Well): WellPointerWell = WellPointerWell(well)
	def apply(index: Int): WellIndex = WellIndex(index)
	def apply(plate: Plate)(ptrs: WellAddressPartial*): WellPointerPlateAddress = new WellPointerPlateAddress(plate, ptrs.toSeq)
}

sealed trait WellAddressPartial {
	protected def toIndexes(dim: PlateSetupDimensionL4): Result[Seq[Int]]
	def toPointer(kb: KnowledgeBase, plate: Plate): Result[WellPointer] = {
		for {
			dim <- Result.get(plate.setup.dim_?, "plate dimension not set")
			li <- toIndexes(dim)
		} yield {
			val lWell = li.map(iWell => dim.wells(iWell))
			if (li.size == 1) {
				WellPointerWell(lWell.head)
			}
			else {
				WellPointerWells(lWell)
			}
		}
	}

	def toWells(kb: KnowledgeBase, plate: Plate): Result[Seq[Well]] = {
		for {
			dim <- Result.get(plate.setup.dim_?, "plate dimension not set")
			li <- toIndexes(dim)
		} yield {
			li.map(iWell => dim.wells(iWell))
		}
	}

	/*
	private def toWell(dim: PlateSetupDimensionL4, iWell: Int): Result[Well] = {
		val (nRows, nCols) = (dim.nRows, dim.nCols)
		val nWells = nRows * nCols
		if (iWell >= 0 && iWell < nWells) {
			Success(dim.wells(iWell))
		}
		else {
			val iRow = iWell % nRows
			val iCol = iWell / nCols
			Error("bad well indexExceeds plate dimension of "+nRows+"x"+nCols+": "+(pair._1+pair._2+"+"+pair._3)
		}
	}
	*/
}

sealed abstract class WellAddressSingle extends WellAddress {
	def +(n: Int): WellAddressPlus = new WellAddressPlus(this, n)
	def -(b: WellAddressSingle): WellAddressMinus = new WellAddressMinus(this, b)
	def toIndex(dim: PlateSetupDimensionL4): Result[Int]
	
	protected def toIndexes(dim: PlateSetupDimensionL4): Result[Seq[Int]] = {
		for { index <- toIndex(dim) }
		yield { Seq(index) }
	}
}

sealed trait WellPointer extends WellAddress {
	def getWells(kb: KnowledgeBase): Result[Seq[Well]]
	def +(that: WellPointer) = new WellPointerSeq(this.toSeq ++ that.toSeq)
	protected def toSeq: Seq[WellPointer] = Seq(this)
}

case class WellPointerWell(well: Well) extends WellAddressSingle with WellPointer {
	def getWells(kb: KnowledgeBase): Result[Seq[Well]] = Success(Seq(well))
	def toIndex(dim: PlateSetupDimensionL4): Result[Int] = Result.get(well.setup.index_?, "well index not set")
	override def toString = well.toString
}

case class WellPointerWells(lWell: Seq[Well]) extends WellPointer {
	def getWells(kb: KnowledgeBase): Result[Seq[Well]] = Success(lWell)
	override def toString = lWell.mkString(";")
}

case class WellPointerPlate(plate: Plate) extends WellPointer {
	def getWells(kb: KnowledgeBase): Result[Seq[Well]] = {
		for { dim <- Result.get(plate.setup.dim_?, "plate dimension must be defined") }
		yield { dim.wells }
	}
}

case class WellPointerReagent(reagent: Reagent) extends WellPointer {
	def getWells(kb: KnowledgeBase): Result[Seq[Well]] = {
		Success(kb.getReagentWells(reagent).toSeq)
	}
}


/*
					val dim = pc.dim_?.get
					val (nRows, nCols) = (dim.nRows, dim.nCols)
					val (iRow, iCol, nWells) = (pair._1 - 'A', pair._2 - 1, pair._3)
					if (iRow < 0 || iRow >= nRows) {
						sError = ("Invalid row: "+pair._1)
						Nil
					}
					else if (iCol < 0 || iCol >= nCols) {
						sError = ("Invalid column: "+pair._2)
						Nil
					}
					else {
						val i0 = iRow + iCol * nRows
						val i1 = i0 + nWells - 1
						if (i1 >= nRows * nCols) {
							sError = ("Exceeds plate dimension of "+nRows+"x"+nCols+": "+pair._1+pair._2+"+"+pair._3)
							Nil
						}
						else {
							(i0 to i1).map(plate -> _)
						}
					}
 */
case class WellCoord(iRow: Int, iCol: Int) extends WellAddressSingle with WellAddressPartial {
	def toIndex(dim: PlateSetupDimensionL4): Result[Int] = {
		import dim._
		val sRow = (iRow + 'A').asInstanceOf[Char].toString
		val sCol = (iCol + 1).toString
		if (iRow < 0 || iRow >= nRows) {
			Error("invalid row: "+sRow)
		}
		else if (iCol < 0 || iCol >= nCols) {
			Error("invalid column: "+sCol)
		}
		else {
			val index = iRow + iCol * dim.nRows
			Success(index)
		}
	}
	override def toString = (iRow + 'A').asInstanceOf[Char].toString + (iCol + 1)
}

object WellCoord {
	val A1 = WellCoord(0, 0)
	val A2 = WellCoord(0, 1)
	val A3 = WellCoord(0, 2)
	val A4 = WellCoord(0, 3)
	val A5 = WellCoord(0, 4)
	val A6 = WellCoord(0, 5)
	val A7 = WellCoord(0, 6)
	val A8 = WellCoord(0, 7)
	val G7 = WellCoord(6, 6)
}

case class WellIndex(index: Int) extends WellAddressSingle with WellAddressPartial{
	def toIndex(dim: PlateSetupDimensionL4): Result[Int] = Success(index)
	override def toString = index.toString
}

case class WellAddressPlus(a: WellAddressSingle, n: Int) extends WellAddress with WellAddressPartial {
	protected def toIndexes(dim: PlateSetupDimensionL4): Result[Seq[Int]] = {
		for { i0 <- a.toIndex(dim) }
		yield { (i0 until (i0 + n)).toSeq }
	}
	
	override def toString = a.toString + "+" + n
}

case class WellAddressMinus(a: WellAddressSingle, b: WellAddressSingle) extends WellAddress with WellAddressPartial {
	protected def toIndexes(dim: PlateSetupDimensionL4): Result[Seq[Int]] = {
		for {
			i0 <- a.toIndex(dim);
			i1 <- b.toIndex(dim);
			_ <- Result.assert(i0 <= i1, "second well must have a higher index than the first")
		}
		yield { (i0 until i1).toSeq }
	}
	override def toString = a + "-" + b
}

/*case class WellAddressColon(a: WellAddressSingle, b: WellAddressSingle) extends WellAddress with WellAddressPartial {
	protected def toIndexes(dim: PlateSetupDimensionL4): Result[Seq[Int]] = {
		for { i0 <- a.toIndex(dim) }
		yield { (i0 until (i0 + n)).toSeq }
	}
	override def toString = a + ":" + b
}*/

case class WellPointerPlateAddress(plate: Plate, addrs: Seq[WellAddressPartial]) extends WellPointer {
	def getWells(kb: KnowledgeBase): Result[Seq[Well]] = {
		Result.flatMap(addrs) { _.toWells(kb, plate) }
	}
	override def toString = plate.toString+":"+addrs.mkString(",") 
}

case class WellPointerSeq(seq: Seq[WellPointer]) extends WellPointer {
	def getWells(kb: KnowledgeBase): Result[Seq[Well]] = {
		Result.flatMap(seq) { _.getWells(kb) }
	}
	override protected def toSeq: Seq[WellPointer] = seq
	override def toString = seq.mkString(";") 
}
