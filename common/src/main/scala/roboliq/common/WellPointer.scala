package roboliq.common

sealed abstract class WellAddress

object WellAddress {
	def apply(index: Int): WellIndex = WellIndex(index)
}

sealed trait WellAddressPartial {
	protected def toIndexes(nRows: Int, nCols: Int): Result[Seq[Int]]
	def toPointer(kb: KnowledgeBase, plate: Plate): Result[WellPointer] = {
		for {
			dim <- Result.get(plate.setup.dim_?, "plate dimension not set")
			li <- toIndexes(dim.nRows, dim.nCols)
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
			li <- toIndexes(dim.nRows, dim.nCols)
		} yield {
			li.map(iWell => dim.wells(iWell))
		}
	}

	def toWells(states: RobotState, plate: Plate): Result[Seq[WellConfigL2]] = {
		val conf = plate.state(states).conf
		for { li <- toIndexes(conf.nRows, conf.nCols) }
		yield li.map(iWell => conf.wells(iWell).state(states).conf)
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
	def toIndex(nRows: Int, nCols: Int): Result[Int]
	
	protected def toIndexes(nRows: Int, nCols: Int): Result[Seq[Int]] = {
		for { index <- toIndex(nRows, nCols) }
		yield { Seq(index) }
	}
}

sealed trait WellPointer extends WellAddress {
	def getWells(kb: KnowledgeBase): Result[Seq[Well]]
	def getWells(states: RobotState): Result[Seq[WellConfigL2]]
	def getPlatesL4: Result[Seq[Plate]] = Success(Seq())
	def getReagentsL4: Result[Seq[Reagent]] = Success(Seq())
	def +(that: WellPointer) = new WellPointerSeq(this.toSeq ++ that.toSeq)
	protected def toSeq: Seq[WellPointer] = Seq(this)
}

object WellPointer {
	def apply(o: Well): WellPointerWell = WellPointerWell(o)
	def apply(o: Seq[Well]): WellPointerWells = WellPointerWells(o)
	//def apply(plate: Plate)(ptrs: WellAddressPartial*): WellPointerPlateAddress = new WellPointerPlateAddress(plate, ptrs.toSeq)
	def apply(o: Plate): WellPointerPlate = WellPointerPlate(o)
	def apply(o: Reagent): WellPointerReagent = new WellPointerReagent(o)
}

class WellPointerPlateWrapper(plate: Plate) {
	def apply(ptrs: WellAddressPartial*): WellPointerPlateAddress = new WellPointerPlateAddress(plate, ptrs.toSeq)
}

trait WellPointerImplicits {
	implicit def wellToPointer(o: Well): WellPointerWell = WellPointerWell(o)
	implicit def plateToPointer(o: Plate): WellPointerPlate = WellPointerPlate(o)
	implicit def plateToWrapper(o: Plate): WellPointerPlateWrapper = new WellPointerPlateWrapper(o)
	implicit def reagentToPointer(o: Reagent): WellPointerReagent = WellPointerReagent(o)
}

object WellPointerImplicits extends WellPointerImplicits {
	
}

case class WellPointerVar() extends WellPointer {
	var pointer_? : Option[WellPointer] = None

	def getWells(kb: KnowledgeBase): Result[Seq[Well]] = pointer_?.map(_.getWells(kb)).getOrElse(Success(Seq()))
	def getWells(states: RobotState): Result[Seq[WellConfigL2]] = pointer_?.map(_.getWells(states)).getOrElse(Success(Seq()))
	override def getPlatesL4: Result[Seq[Plate]] = pointer_?.map(_.getPlatesL4).getOrElse(Success(Seq()))
	override def getReagentsL4: Result[Seq[Reagent]] = pointer_?.map(_.getReagentsL4).getOrElse(Success(Seq()))
	override def toString = pointer_?.map(_.toString).getOrElse(super.toString)
}

case class WellPointerWell(well: Well) extends WellAddressSingle with WellPointer {
	def getWells(kb: KnowledgeBase): Result[Seq[Well]] = Success(Seq(well))
	def getWells(states: RobotState): Result[Seq[WellConfigL2]] = Success(Seq(well.state(states).conf))
	def toIndex(nRows: Int, nCols: Int): Result[Int] = Result.get(well.setup.index_?, "well index not set")
	override def toString = well.toString
}

case class WellPointerWells(lWell: Seq[Well]) extends WellPointer {
	def getWells(kb: KnowledgeBase): Result[Seq[Well]] = Success(lWell)
	def getWells(states: RobotState): Result[Seq[WellConfigL2]] = Success(lWell.map(_.state(states).conf))
	override def toString = lWell.mkString(";")
}

case class WellPointerPlate(plate: Plate) extends WellPointer {
	def getWells(kb: KnowledgeBase): Result[Seq[Well]] = {
		for { dim <- Result.get(plate.setup.dim_?, "plate dimension must be defined") }
		yield { dim.wells }
	}
	def getWells(states: RobotState): Result[Seq[WellConfigL2]] = Success(plate.state(states).conf.wells.map(_.state(states).conf))
	override def getPlatesL4: Result[Seq[Plate]] = Success(Seq(plate))
}

case class WellPointerReagent(reagent: Reagent) extends WellPointer {
	def getWells(kb: KnowledgeBase): Result[Seq[Well]] = {
		Success(kb.getReagentWells(reagent).toSeq)
	}
	
	def getWells(states: RobotState): Result[Seq[WellConfigL2]] = {
		val lWell = states.map.toSeq.collect({ case (well: Well, wellState: WellStateL2) if well.setup.reagent_? == Some(reagent) => wellState.conf })
		Success(lWell)
	}

	override def getReagentsL4: Result[Seq[Reagent]] = Success(Seq(reagent))
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
	def toIndex(nRows: Int, nCols: Int): Result[Int] = {
		val sRow = (iRow + 'A').asInstanceOf[Char].toString
		val sCol = (iCol + 1).toString
		if (iRow < 0 || iRow >= nRows) {
			Error("invalid row: "+sRow)
		}
		else if (iCol < 0 || iCol >= nCols) {
			Error("invalid column: "+sCol)
		}
		else {
			val index = iRow + iCol * nRows
			Success(index)
		}
	}
	override def toString = (iRow + 'A').asInstanceOf[Char].toString + (iCol + 1)
}

trait WellCoords {
	val A1 = WellCoord(0, 0)
	val A2 = WellCoord(0, 1)
	val A3 = WellCoord(0, 2)
	val A4 = WellCoord(0, 3)
	val A5 = WellCoord(0, 4)
	val A6 = WellCoord(0, 5)
	val A7 = WellCoord(0, 6)
	val A8 = WellCoord(0, 7)
	val B1 = WellCoord(1, 0)
	val B2 = WellCoord(1, 1)
	val B3 = WellCoord(1, 2)
	val B4 = WellCoord(1, 3)
	val B5 = WellCoord(1, 4)
	val B6 = WellCoord(1, 5)
	val B7 = WellCoord(1, 6)
	val B8 = WellCoord(1, 7)
	val C1 = WellCoord(2, 0)
	val C2 = WellCoord(2, 1)
	val C3 = WellCoord(2, 2)
	val C4 = WellCoord(2, 3)
	val C5 = WellCoord(2, 4)
	val C6 = WellCoord(2, 5)
	val C7 = WellCoord(2, 6)
	val C8 = WellCoord(2, 7)
	val D1 = WellCoord(3, 0)
	val D2 = WellCoord(3, 1)
	val D3 = WellCoord(3, 3)
	val D4 = WellCoord(3, 3)
	val D5 = WellCoord(3, 4)
	val D6 = WellCoord(3, 5)
	val D7 = WellCoord(3, 6)
	val D8 = WellCoord(3, 7)
	val G7 = WellCoord(6, 6)
}

case class WellIndex(index: Int) extends WellAddressSingle with WellAddressPartial{
	def toIndex(nRows: Int, nCols: Int): Result[Int] = Success(index)
	override def toString = index.toString
}

case class WellAddressPlus(a: WellAddressSingle, n: Int) extends WellAddress with WellAddressPartial {
	protected def toIndexes(nRows: Int, nCols: Int): Result[Seq[Int]] = {
		for { i0 <- a.toIndex(nRows, nCols) }
		yield { (i0 until (i0 + n)).toSeq }
	}
	
	override def toString = a.toString + "+" + n
}

case class WellAddressMinus(a: WellAddressSingle, b: WellAddressSingle) extends WellAddress with WellAddressPartial {
	protected def toIndexes(nRows: Int, nCols: Int): Result[Seq[Int]] = {
		for {
			i0 <- a.toIndex(nRows, nCols);
			i1 <- b.toIndex(nRows, nCols);
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
	def getWells(states: RobotState): Result[Seq[WellConfigL2]] = {
		Result.flatMap(addrs) { _.toWells(states, plate) }
	}
	override def getPlatesL4: Result[Seq[Plate]] = Success(Seq(plate))
	override def toString = plate.toString+":"+addrs.mkString(",") 
}

case class WellPointerSeq(seq: Seq[WellPointer]) extends WellPointer {
	def getWells(kb: KnowledgeBase): Result[Seq[Well]] = {
		Result.flatMap(seq) { _.getWells(kb) }
	}
	def getWells(states: RobotState): Result[Seq[WellConfigL2]] = {
		Result.flatMap(seq) { _.getWells(states) }
	}
	override protected def toSeq: Seq[WellPointer] = seq
	override def getPlatesL4: Result[Seq[Plate]] = Result.flatMap(seq) { _.getPlatesL4 }
	override def getReagentsL4: Result[Seq[Reagent]] = Result.flatMap(seq) { _.getReagentsL4 }
	override def toString = seq.mkString(";") 
}
